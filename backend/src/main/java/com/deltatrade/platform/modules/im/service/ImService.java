package com.deltatrade.platform.modules.im.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.boosting.mapper.BoostingOrderMapper;
import com.deltatrade.platform.modules.boosting.model.BoostingOrderDO;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.im.mapper.ImConversationMapper;
import com.deltatrade.platform.modules.im.mapper.ImMessageMapper;
import com.deltatrade.platform.modules.im.mapper.ImParticipantMapper;
import com.deltatrade.platform.modules.im.mapper.ImSupportReadStateMapper;
import com.deltatrade.platform.modules.im.model.ImConversationDO;
import com.deltatrade.platform.modules.im.model.ImMessageDO;
import com.deltatrade.platform.modules.im.model.ImParticipantDO;
import com.deltatrade.platform.modules.im.model.ImSupportReadStateDO;
import com.deltatrade.platform.modules.order.mapper.TradeOrderMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImService {

    private static final Logger log = LoggerFactory.getLogger(ImService.class);
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String SUPPORT_NAME = "平台客服";
    private static final int WORKBENCH_SEED_LIMIT = 80;

    private final ImConversationMapper imConversationMapper;
    private final ImParticipantMapper imParticipantMapper;
    private final ImMessageMapper imMessageMapper;
    private final ImSupportReadStateMapper imSupportReadStateMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final BoostingOrderMapper boostingOrderMapper;
    private final AccountListingMapper accountListingMapper;
    private final AuthUserMapper authUserMapper;
    private final OssStorageService ossStorageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ImService(
        ImConversationMapper imConversationMapper,
        ImParticipantMapper imParticipantMapper,
        ImMessageMapper imMessageMapper,
        ImSupportReadStateMapper imSupportReadStateMapper,
        TradeOrderMapper tradeOrderMapper,
        BoostingOrderMapper boostingOrderMapper,
        AccountListingMapper accountListingMapper,
        AuthUserMapper authUserMapper,
        OssStorageService ossStorageService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.imConversationMapper = imConversationMapper;
        this.imParticipantMapper = imParticipantMapper;
        this.imMessageMapper = imMessageMapper;
        this.imSupportReadStateMapper = imSupportReadStateMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.boostingOrderMapper = boostingOrderMapper;
        this.accountListingMapper = accountListingMapper;
        this.authUserMapper = authUserMapper;
        this.ossStorageService = ossStorageService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
	    public ConversationPayload getConversation(Long userId, String conversationNo) {
        long startAt = System.currentTimeMillis();
        ConversationContext context = resolveUserContext(userId, conversationNo);
        ImConversationDO conversation = ensureConversation(context);
        List<ImMessageDO> messages = listMessages(conversationNo);
        Participant viewer = context.requireParticipant(userId);
        markReadInternal(userId, conversationNo, lastMessageId(messages));
        ConversationPayload payload = toPayload(context, messages, viewer);
        log.info("im conversation loaded conversationNo={} userId={} scene={} messages={} costMs={}",
            conversationNo, userId, context.sceneCode, messages.size(), System.currentTimeMillis() - startAt);
	        return payload;
	    }

	    @Transactional(readOnly = true)
	    public LatestConversationResult getLatestConversation(Long userId) {
	        List<ImConversationDO> rows = imConversationMapper.selectList(
	            Wrappers.<ImConversationDO>lambdaQuery()
	                .and(wrapper -> wrapper.eq(ImConversationDO::getBuyerUserId, userId).or().eq(ImConversationDO::getSellerUserId, userId))
	                .orderByDesc(ImConversationDO::getLastMessageAt)
	                .orderByDesc(ImConversationDO::getUpdatedAt)
	                .last("LIMIT 20")
	        );
        return rows.stream()
            .filter(this::isUserVisibleConversation)
            .findFirst()
            .map(row -> new LatestConversationResult(row.getConversationNo()))
            .orElseGet(() -> new LatestConversationResult(null));
	    }

	    @Transactional
	    public ConversationPayload sendMessage(Long userId, String conversationNo, SendMessageCommand command) {
        long startAt = System.currentTimeMillis();
        ConversationContext context = resolveUserContext(userId, conversationNo);
        ImConversationDO conversation = ensureConversation(context);
        Participant participant = context.requireParticipant(userId);
        validateCommand(command);

        LocalDateTime now = LocalDateTime.now();
        ImMessageDO message = buildMessage(conversationNo, participant.roleCode, userId, participant.displayName, command, now);
        insertMessage(message);
        updateConversationLatest(conversation, buildExcerpt(message), now);
        markReadInternal(userId, conversationNo, message.getId());
        List<ImMessageDO> messages = listMessages(conversationNo);
        ConversationPayload payload = toPayload(context, messages, participant);
        pushConversationRefresh(context, conversation, messages, "USER_MESSAGE");
        log.info("im message send success conversationNo={} userId={} senderRole={} type={} costMs={}",
            conversationNo, userId, participant.roleCode, message.getMessageType(), System.currentTimeMillis() - startAt);
        return payload;
    }

	    @Transactional
	    public ReadMarkResult markRead(Long userId, String conversationNo) {
        long startAt = System.currentTimeMillis();
        ConversationContext context = resolveUserContext(userId, conversationNo);
        ensureConversation(context);
        List<ImMessageDO> messages = listMessages(conversationNo);
        long lastId = lastMessageId(messages);
        markReadInternal(userId, conversationNo, lastId);
        log.info("im mark-read success conversationNo={} userId={} lastMessageId={} costMs={}",
            conversationNo, userId, lastId, System.currentTimeMillis() - startAt);
	        return new ReadMarkResult(conversationNo, lastId);
	    }

	    @Transactional
	    public void ensureTradeConversation(String conversationNo) {
	        if (conversationNo == null || conversationNo.trim().isEmpty()) {
	            return;
	        }
	        TradeOrderDO tradeOrder = tradeOrderMapper.selectOne(
	            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getChatGroupNo, conversationNo).last("LIMIT 1")
	        );
	        if (tradeOrder == null) {
	            return;
	        }
	        if (!isPaidTradeStatus(tradeOrder.getStatus())) {
	            return;
	        }
	        ensureConversation(buildTradeContext(tradeOrder, false));
	    }

	    @Transactional
	    public ConversationPayload openListingConsultation(Long userId, String listingNo, String presetText) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成付款，付款后可在订单消息中联系卖家");
    }

    @Transactional
    public WorkbenchPayload loadWorkbench(Long userId, String keyword, String sceneCode) {
        long startAt = System.currentTimeMillis();
        Participant supportViewer = loadSupportViewer(userId);
        seedWorkbenchConversations();
        String normalizedKeyword = trimNullable(keyword);
        String normalizedScene = trimNullable(sceneCode);

        List<ImConversationDO> conversations = imConversationMapper.selectList(
            Wrappers.<ImConversationDO>lambdaQuery()
                .orderByDesc(ImConversationDO::getLastMessageAt)
                .orderByDesc(ImConversationDO::getUpdatedAt)
        );

        List<WorkbenchConversationItem> rows = conversations.stream()
            .map(this::toWorkbenchConversationItem)
            .filter(Objects::nonNull)
            .filter(item -> matchesWorkbenchFilters(item, normalizedKeyword, normalizedScene))
            .collect(Collectors.toList());
        rows.sort(Comparator
            .comparing((WorkbenchConversationItem item) -> item.closedOrder)
            .thenComparing(
                item -> item.lastMemberMessageTime == null ? LocalDateTime.MIN : item.lastMemberMessageTime,
                Comparator.reverseOrder()
            )
            .thenComparing(item -> item.conversationNo, Comparator.reverseOrder())
        );

        WorkbenchPayload payload = new WorkbenchPayload(
            supportViewer.displayName,
            supportViewer.avatarUrl,
            rows,
            rows.size(),
            rows.stream().filter(item -> "BOOSTING_ORDER".equals(item.getSceneCode())).count(),
            rows.stream().filter(item -> "TRADE_ORDER".equals(item.getSceneCode()) || "LISTING_CONSULT".equals(item.getSceneCode())).count()
        );
        log.info("im workbench loaded userId={} keyword={} sceneCode={} rows={} costMs={}",
            userId, safeText(normalizedKeyword, "ALL"), safeText(normalizedScene, "ALL"), rows.size(), System.currentTimeMillis() - startAt);
        return payload;
    }

    @Transactional
    public ConversationPayload getWorkbenchConversation(Long userId, String conversationNo) {
        long startAt = System.currentTimeMillis();
        Participant supportViewer = loadSupportViewer(userId);
        ConversationContext context = resolveSupportContext(conversationNo);
        ensureConversation(context);
        List<ImMessageDO> messages = listMessages(conversationNo);
        markSupportRead(conversationNo, lastMessageId(messages));
        ConversationPayload payload = toPayload(context, messages, supportViewer);
        log.info("im workbench conversation loaded conversationNo={} userId={} scene={} messages={} costMs={}",
            conversationNo, userId, context.sceneCode, messages.size(), System.currentTimeMillis() - startAt);
        return payload;
    }

    @Transactional
    public ConversationPayload sendWorkbenchMessage(Long userId, String conversationNo, SendMessageCommand command) {
        long startAt = System.currentTimeMillis();
        Participant supportViewer = loadSupportViewer(userId);
        ConversationContext context = resolveSupportContext(conversationNo);
        ImConversationDO conversation = ensureConversation(context);
        validateCommand(command);

        LocalDateTime now = LocalDateTime.now();
        ImMessageDO message = buildMessage(conversationNo, "CUSTOMER_SERVICE", userId, supportViewer.displayName, command, now);
        insertMessage(message);
        updateConversationLatest(conversation, buildExcerpt(message), now);

        List<ImMessageDO> messages = listMessages(conversationNo);
        ConversationPayload payload = toPayload(context, messages, supportViewer);
        pushConversationRefresh(context, conversation, messages, "SUPPORT_MESSAGE");
        log.info("im workbench message send success conversationNo={} userId={} type={} costMs={}",
            conversationNo, userId, message.getMessageType(), System.currentTimeMillis() - startAt);
        return payload;
    }

    private ConversationContext resolveUserContext(Long userId, String conversationNo) {
        ImConversationDO listingConsult = imConversationMapper.selectOne(
            Wrappers.<ImConversationDO>lambdaQuery()
                .eq(ImConversationDO::getConversationNo, conversationNo)
                .eq(ImConversationDO::getSceneCode, "LISTING_CONSULT")
                .last("LIMIT 1")
        );
        if (listingConsult != null) {
            boolean isBuyer = Objects.equals(userId, listingConsult.getBuyerUserId());
            boolean isSeller = Objects.equals(userId, listingConsult.getSellerUserId());
            if (!isBuyer && !isSeller) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户无权访问该咨询会话");
            }
            return buildListingConsultContext(listingConsult);
        }

        TradeOrderDO tradeOrder = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getChatGroupNo, conversationNo).last("LIMIT 1")
        );
        if (tradeOrder != null) {
            if (!isPaidTradeStatus(tradeOrder.getStatus())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成付款，付款后可进入群聊");
            }
            boolean isBuyer = Objects.equals(userId, tradeOrder.getBuyerUserId());
            boolean isSeller = Objects.equals(userId, tradeOrder.getSellerUserId());
            if (!isBuyer && !isSeller) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户无权访问该群聊");
            }
            return buildTradeContext(tradeOrder);
        }

        BoostingOrderDO boostingOrder = boostingOrderMapper.selectOne(
            Wrappers.<BoostingOrderDO>lambdaQuery().eq(BoostingOrderDO::getChatGroupNo, conversationNo).last("LIMIT 1")
        );
        if (boostingOrder != null) {
            if (!Objects.equals(userId, boostingOrder.getBuyerUserId())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户无权访问该客服会话");
            }
            return buildBoostingContext(boostingOrder);
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "群聊不存在或尚未生成");
    }

    private ConversationContext resolveSupportContext(String conversationNo) {
        return resolveSupportContext(conversationNo, true);
    }

    private ConversationContext resolveSupportContext(String conversationNo, boolean includeAvatars) {
        ImConversationDO listingConsult = imConversationMapper.selectOne(
            Wrappers.<ImConversationDO>lambdaQuery()
                .eq(ImConversationDO::getConversationNo, conversationNo)
                .eq(ImConversationDO::getSceneCode, "LISTING_CONSULT")
                .last("LIMIT 1")
        );
        if (listingConsult != null) {
            return buildListingConsultContext(listingConsult, includeAvatars);
        }
        TradeOrderDO tradeOrder = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getChatGroupNo, conversationNo).last("LIMIT 1")
        );
        if (tradeOrder != null) {
            return buildTradeContext(tradeOrder, includeAvatars);
        }
        BoostingOrderDO boostingOrder = boostingOrderMapper.selectOne(
            Wrappers.<BoostingOrderDO>lambdaQuery().eq(BoostingOrderDO::getChatGroupNo, conversationNo).last("LIMIT 1")
        );
        if (boostingOrder != null) {
            return buildBoostingContext(boostingOrder, includeAvatars);
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "群聊不存在或尚未生成");
    }

    private ConversationContext buildTradeContext(TradeOrderDO tradeOrder) {
        return buildTradeContext(tradeOrder, true);
    }

    private ConversationContext buildTradeContext(TradeOrderDO tradeOrder, boolean includeAvatars) {
        Map<Long, AuthUserDO> users = includeAvatars
            ? loadUsers(Arrays.asList(tradeOrder.getBuyerUserId(), tradeOrder.getSellerUserId()))
            : Collections.emptyMap();
        Participant buyer = new Participant(
            tradeOrder.getBuyerUserId(),
            "BUYER",
            safeText(tradeOrder.getBuyerNickname(), "买家"),
            includeAvatars ? previewNullable(users.get(tradeOrder.getBuyerUserId()) == null ? null : users.get(tradeOrder.getBuyerUserId()).getAvatarKey()) : null
        );
        Participant seller = new Participant(
            tradeOrder.getSellerUserId(),
            "SELLER",
            safeText(tradeOrder.getSellerDisplayName(), safeText(tradeOrder.getSellerNickname(), "卖家")),
            includeAvatars ? previewNullable(users.get(tradeOrder.getSellerUserId()) == null ? null : users.get(tradeOrder.getSellerUserId()).getAvatarKey()) : null
        );
        List<SummaryItem> summaryItems = Arrays.asList(
            new SummaryItem("订单号", tradeOrder.getOrderNo()),
            new SummaryItem("账号编号", safeText(tradeOrder.getListingNo(), "—")),
            new SummaryItem("商品名称", safeText(tradeOrder.getListingTitle(), "账号交易订单")),
            new SummaryItem("订单总额", formatAmount(tradeOrder.getTotalAmount())),
            new SummaryItem("当前状态", renderTradeStatus(tradeOrder.getStatus()))
        );
        return new ConversationContext(
            safeText(tradeOrder.getChatGroupNo(), ""),
            "TRADE_ORDER",
            tradeOrder.getOrderNo(),
            safeText(tradeOrder.getListingNo(), ""),
            safeText(tradeOrder.getListingTitle(), "账号交易群聊"),
            safeText(tradeOrder.getListingSummary(), "买家、卖家与平台客服可在此沟通账号交接和售后事宜。"),
            renderTradeStatus(tradeOrder.getStatus()),
            SUPPORT_NAME,
            buyer,
            seller,
            summaryItems
        );
    }

    private ConversationContext buildBoostingContext(BoostingOrderDO boostingOrder) {
        return buildBoostingContext(boostingOrder, true);
    }

    private ConversationContext buildBoostingContext(BoostingOrderDO boostingOrder, boolean includeAvatars) {
        Map<Long, AuthUserDO> users = includeAvatars
            ? loadUsers(Collections.singletonList(boostingOrder.getBuyerUserId()))
            : Collections.emptyMap();
        Participant buyer = new Participant(
            boostingOrder.getBuyerUserId(),
            "BUYER",
            safeText(boostingOrder.getBuyerNickname(), "买家"),
            includeAvatars ? previewNullable(users.get(boostingOrder.getBuyerUserId()) == null ? null : users.get(boostingOrder.getBuyerUserId()).getAvatarKey()) : null
        );
        List<SummaryItem> summaryItems = Arrays.asList(
            new SummaryItem("订单号", boostingOrder.getOrderNo()),
            new SummaryItem("服务名称", safeText(boostingOrder.getServiceName(), "代肝订单")),
            new SummaryItem("订单金额", formatAmount(boostingOrder.getPrice())),
            new SummaryItem("当前状态", renderBoostingStatus(boostingOrder.getStatus()))
        );
        return new ConversationContext(
            safeText(boostingOrder.getChatGroupNo(), ""),
            "BOOSTING_ORDER",
            boostingOrder.getOrderNo(),
            safeText(boostingOrder.getServiceNo(), ""),
            safeText(boostingOrder.getServiceName(), "代肝客服会话"),
            safeText(boostingOrder.getServiceDescription(), "代肝相关问题、进度沟通和售后反馈请在此联系平台客服。"),
            renderBoostingStatus(boostingOrder.getStatus()),
            SUPPORT_NAME,
            buyer,
            null,
            summaryItems
        );
    }

    private ConversationContext buildListingConsultContext(ImConversationDO conversation) {
        return buildListingConsultContext(conversation, true);
    }

    private ConversationContext buildListingConsultContext(ImConversationDO conversation, boolean includeAvatars) {
        AccountListingDO listing = requirePublishedListing(conversation.getSourceOrderNo());
        AuthUserDO buyerUser = requireUser(conversation.getBuyerUserId());
        return buildListingConsultContext(conversation.getConversationNo(), listing, buyerUser, includeAvatars);
    }

    private ConversationContext buildListingConsultContext(String conversationNo, AccountListingDO listing, AuthUserDO buyerUser) {
        return buildListingConsultContext(conversationNo, listing, buyerUser, true);
    }

    private ConversationContext buildListingConsultContext(String conversationNo, AccountListingDO listing, AuthUserDO buyerUser, boolean includeAvatars) {
        Map<Long, AuthUserDO> users = includeAvatars
            ? loadUsers(Arrays.asList(buyerUser.getId(), listing.getSellerUserId()))
            : Collections.emptyMap();
        Participant buyer = new Participant(
            buyerUser.getId(),
            "BUYER",
            safeText(buyerUser.getNickname(), "咨询买家"),
            includeAvatars ? previewNullable(users.get(buyerUser.getId()) == null ? null : users.get(buyerUser.getId()).getAvatarKey()) : null
        );
        Participant seller = new Participant(
            listing.getSellerUserId(),
            "SELLER",
            safeText(resolveSellerDisplayName(listing), safeText(listing.getSellerNickname(), "卖家")),
            includeAvatars ? previewNullable(users.get(listing.getSellerUserId()) == null ? null : users.get(listing.getSellerUserId()).getAvatarKey()) : null
        );
        List<SummaryItem> summaryItems = Arrays.asList(
            new SummaryItem("商品编号", safeText(listing.getListingNo(), "—")),
            new SummaryItem("商品名称", safeText(listing.getTitle(), "账号商品")),
            new SummaryItem("商品售价", formatAmount(listing.getPrice())),
            new SummaryItem("卖家类型", renderSellerType(listing.getSellerType()))
        );
        return new ConversationContext(
            conversationNo,
            "LISTING_CONSULT",
            safeText(listing.getListingNo(), ""),
            safeText(listing.getListingNo(), ""),
            safeText(listing.getTitle(), "商品售前咨询"),
            "售前咨询已接入站内会话，买家可先咨询卖家，平台客服可随时介入监管。",
            "咨询中",
            SUPPORT_NAME,
            buyer,
            seller,
            summaryItems
        );
    }

    private ImConversationDO ensureConversation(ConversationContext context) {
        ImConversationDO conversation = imConversationMapper.selectOne(
            Wrappers.<ImConversationDO>lambdaQuery().eq(ImConversationDO::getConversationNo, context.conversationNo).last("LIMIT 1")
        );
        if (conversation != null) {
            ensureParticipant(context.conversationNo, context.buyer);
            if (context.seller != null) {
                ensureParticipant(context.conversationNo, context.seller);
            }
            return conversation;
        }
        LocalDateTime now = LocalDateTime.now();
        conversation = new ImConversationDO();
        conversation.setConversationNo(context.conversationNo);
        conversation.setSceneCode(context.sceneCode);
        conversation.setSourceOrderNo(context.sourceOrderNo);
        conversation.setTitle(context.title);
        conversation.setBuyerUserId(context.buyer.userId);
        conversation.setSellerUserId(context.seller == null ? null : context.seller.userId);
        conversation.setSupportDisplayName(context.supportDisplayName);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setLastMessageExcerpt("会话已创建");
        conversation.setLastMessageAt(now);
        imConversationMapper.insert(conversation);
        ensureParticipant(context.conversationNo, context.buyer);
        if (context.seller != null) {
            ensureParticipant(context.conversationNo, context.seller);
        }
        ImMessageDO welcome = systemMessage(context.conversationNo, "欢迎进入客服会话", buildWelcomeContent(context), now);
        insertMessage(welcome);
        ImMessageDO hello = supportMessage(context.conversationNo, null, SUPPORT_NAME, "你好，这里是平台客服，已为你接入本次订单会话，有问题可以直接留言。", now.plusSeconds(1));
        insertMessage(hello);
        updateConversationLatest(conversation, buildExcerpt(hello), hello.getCreatedAt());
        pushConversationRefresh(context, conversation, listMessages(context.conversationNo), "INIT");
        log.info("im conversation init success conversationNo={} scene={} sourceOrderNo={}",
            context.conversationNo, context.sceneCode, context.sourceOrderNo);
        return conversation;
    }

    private void ensureParticipant(String conversationNo, Participant participant) {
        if (participant == null || participant.userId == null) {
            return;
        }
        ImParticipantDO existing = imParticipantMapper.selectOne(
            Wrappers.<ImParticipantDO>lambdaQuery()
                .eq(ImParticipantDO::getConversationNo, conversationNo)
                .eq(ImParticipantDO::getUserId, participant.userId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ImParticipantDO row = new ImParticipantDO();
        row.setConversationNo(conversationNo);
        row.setUserId(participant.userId);
        row.setParticipantRole(participant.roleCode);
        row.setLastReadMessageId(0L);
        row.setJoinedAt(now);
        row.setUpdatedAt(now);
        imParticipantMapper.insert(row);
    }

    private void seedWorkbenchConversations() {
        List<TradeOrderDO> tradeOrders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .isNotNull(TradeOrderDO::getChatGroupNo)
                .ne(TradeOrderDO::getChatGroupNo, "")
                .in(TradeOrderDO::getStatus, Arrays.asList("WAITING_TRADE", "IN_PROGRESS", "COMPLETED", "REFUND_PENDING", "AFTER_SALE"))
                .orderByDesc(TradeOrderDO::getUpdatedAt)
                .last("LIMIT " + WORKBENCH_SEED_LIMIT)
        );
        for (TradeOrderDO order : tradeOrders) {
            ensureConversation(buildTradeContext(order, false));
        }
        List<BoostingOrderDO> boostingOrders = boostingOrderMapper.selectList(
            Wrappers.<BoostingOrderDO>lambdaQuery()
                .isNotNull(BoostingOrderDO::getChatGroupNo)
                .ne(BoostingOrderDO::getChatGroupNo, "")
                .orderByDesc(BoostingOrderDO::getUpdatedAt)
                .last("LIMIT " + WORKBENCH_SEED_LIMIT)
        );
        for (BoostingOrderDO order : boostingOrders) {
            ensureConversation(buildBoostingContext(order, false));
        }
    }

    private WorkbenchConversationItem toWorkbenchConversationItem(ImConversationDO conversation) {
        ConversationContext context;
        try {
            context = resolveSupportContext(conversation.getConversationNo(), false);
        } catch (BusinessException ignore) {
            return null;
        }
        ImMessageDO latestMemberMessage = findLatestMemberMessage(conversation.getConversationNo());
        LocalDateTime latestMemberAt = latestMemberMessage == null ? conversation.getLastMessageAt() : latestMemberMessage.getCreatedAt();
        long latestMessageId = findLatestMessageId(conversation.getConversationNo());
        ImSupportReadStateDO supportReadState = findSupportReadState(conversation.getConversationNo());
        long supportReadMessageId = supportReadState == null || supportReadState.getLastReadMessageId() == null ? 0L : supportReadState.getLastReadMessageId();
        long userUnreadCount = latestMessageId > supportReadMessageId ? countSupportUnreadMessages(conversation.getConversationNo(), supportReadMessageId) : 0L;
        boolean newConversationUnread = supportReadState == null && latestMessageId > 0;
        long unreadCount = userUnreadCount > 0 ? userUnreadCount : (newConversationUnread ? 1L : 0L);
        return new WorkbenchConversationItem(
            conversation.getConversationNo(),
            context.sceneCode,
            renderSceneLabel(context.sceneCode),
            context.sourceOrderNo,
            context.title,
            context.description,
            context.statusLabel,
            context.buyer == null ? "—" : context.buyer.displayName,
            context.seller == null ? "平台客服会话" : context.seller.displayName,
            safeText(conversation.getLastMessageExcerpt(), "等待消息"),
            formatTime(latestMemberAt),
            latestMemberAt,
            isClosedWorkbenchConversation(context),
            latestMessageId,
            unreadCount > 0,
            unreadCount
        );
    }

    private ImMessageDO findLatestMemberMessage(String conversationNo) {
        return imMessageMapper.selectOne(
            Wrappers.<ImMessageDO>lambdaQuery()
                .eq(ImMessageDO::getConversationNo, conversationNo)
                .in(ImMessageDO::getSenderRole, Arrays.asList("BUYER", "SELLER", "CUSTOMER_SERVICE"))
                .orderByDesc(ImMessageDO::getCreatedAt)
                .last("LIMIT 1")
        );
    }

    private long findLatestMessageId(String conversationNo) {
        ImMessageDO latest = imMessageMapper.selectOne(
            Wrappers.<ImMessageDO>lambdaQuery()
                .eq(ImMessageDO::getConversationNo, conversationNo)
                .orderByDesc(ImMessageDO::getId)
                .last("LIMIT 1")
        );
        return latest == null || latest.getId() == null ? 0L : latest.getId();
    }

    private long findSupportReadMessageId(String conversationNo) {
        ImSupportReadStateDO state = findSupportReadState(conversationNo);
        return state == null || state.getLastReadMessageId() == null ? 0L : state.getLastReadMessageId();
    }

    private ImSupportReadStateDO findSupportReadState(String conversationNo) {
        return imSupportReadStateMapper.selectOne(
            Wrappers.<ImSupportReadStateDO>lambdaQuery()
                .eq(ImSupportReadStateDO::getConversationNo, conversationNo)
                .last("LIMIT 1")
        );
    }

    private long countSupportUnreadMessages(String conversationNo, long lastReadMessageId) {
        Long count = imMessageMapper.selectCount(
            Wrappers.<ImMessageDO>lambdaQuery()
                .eq(ImMessageDO::getConversationNo, conversationNo)
                .gt(ImMessageDO::getId, lastReadMessageId)
                .in(ImMessageDO::getSenderRole, Arrays.asList("BUYER", "SELLER"))
        );
        return count == null ? 0L : count.longValue();
    }

    private void markSupportRead(String conversationNo, long lastMessageId) {
        if (conversationNo == null || conversationNo.trim().isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ImSupportReadStateDO existing = imSupportReadStateMapper.selectOne(
            Wrappers.<ImSupportReadStateDO>lambdaQuery()
                .eq(ImSupportReadStateDO::getConversationNo, conversationNo)
                .last("LIMIT 1")
        );
        if (existing == null) {
            ImSupportReadStateDO created = new ImSupportReadStateDO();
            created.setConversationNo(conversationNo);
            created.setLastReadMessageId(Math.max(0L, lastMessageId));
            created.setUpdatedAt(now);
            imSupportReadStateMapper.insert(created);
            return;
        }
        long current = existing.getLastReadMessageId() == null ? 0L : existing.getLastReadMessageId();
        if (lastMessageId <= current) {
            return;
        }
        existing.setLastReadMessageId(lastMessageId);
        existing.setUpdatedAt(now);
        imSupportReadStateMapper.updateById(existing);
    }

    private boolean isClosedWorkbenchConversation(ConversationContext context) {
        if (context == null) {
            return false;
        }
        if ("TRADE_ORDER".equals(context.sceneCode)) {
            return "已关闭".equals(context.statusLabel);
        }
        if ("BOOSTING_ORDER".equals(context.sceneCode)) {
            return "已取消".equals(context.statusLabel);
        }
        return false;
    }

    private boolean matchesWorkbenchFilters(WorkbenchConversationItem item, String keyword, String sceneCode) {
        if (sceneCode != null && !sceneCode.equals(item.getSceneCode())) {
            return false;
        }
        if (keyword == null) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return item.getConversationNo().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getSourceOrderNo().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getBuyerName().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getCounterpartyName().toLowerCase(Locale.ROOT).contains(normalized)
            || item.getLastMessageExcerpt().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private Participant loadSupportViewer(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        return new Participant(
            userId,
            "CUSTOMER_SERVICE",
            safeText(user.getNickname(), SUPPORT_NAME),
            previewNullable(user.getAvatarKey())
        );
    }

    private void validateCommand(SendMessageCommand command) {
        String text = trimNullable(command.getText());
        String fileKey = trimNullable(command.getFileKey());
        if ((text == null || text.isEmpty()) && (fileKey == null || fileKey.isEmpty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "消息内容不能为空");
        }
    }

    private ImMessageDO buildMessage(String conversationNo, String senderRole, Long senderUserId, String senderName, SendMessageCommand command, LocalDateTime createdAt) {
        ImMessageDO message = new ImMessageDO();
        message.setConversationNo(conversationNo);
        message.setSenderRole(senderRole);
        message.setSenderUserId(senderUserId);
        message.setSenderName(senderName);
        message.setMessageType(resolveMessageType(command.getFileName()));
        message.setContentText(trimNullable(command.getText()));
        message.setFileKey(trimNullable(command.getFileKey()));
        message.setFileName(trimNullable(command.getFileName()));
        message.setCreatedAt(createdAt);
        return message;
    }

    private List<ImMessageDO> listMessages(String conversationNo) {
        return imMessageMapper.selectList(
            Wrappers.<ImMessageDO>lambdaQuery()
                .eq(ImMessageDO::getConversationNo, conversationNo)
                .orderByAsc(ImMessageDO::getCreatedAt)
                .orderByAsc(ImMessageDO::getId)
        );
    }

    private void markReadInternal(Long userId, String conversationNo, long lastMessageId) {
        ImParticipantDO participant = imParticipantMapper.selectOne(
            Wrappers.<ImParticipantDO>lambdaQuery()
                .eq(ImParticipantDO::getConversationNo, conversationNo)
                .eq(ImParticipantDO::getUserId, userId)
                .last("LIMIT 1")
        );
        if (participant == null) {
            return;
        }
        participant.setLastReadMessageId(lastMessageId);
        participant.setUpdatedAt(LocalDateTime.now());
        imParticipantMapper.updateById(participant);
    }

    private ConversationPayload toPayload(ConversationContext context, List<ImMessageDO> messages, Participant viewer) {
        Map<Long, AuthUserDO> users = loadUsers(
            messages.stream()
                .map(ImMessageDO::getSenderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList())
        );
        return new ConversationPayload(
            context.conversationNo,
            context.sceneCode,
            renderSceneLabel(context.sceneCode),
            context.sourceOrderNo,
            context.listingNo,
            context.title,
            context.description,
            context.statusLabel,
            context.supportDisplayName,
            viewer.roleCode,
            viewer.displayName,
            viewer.avatarUrl,
            buildParticipantPayloads(context, viewer),
            context.summaryItems,
            messages.stream().map(item -> toMessagePayload(item, viewer, users)).collect(Collectors.toList())
        );
    }

    private List<ConversationParticipantPayload> buildParticipantPayloads(ConversationContext context, Participant viewer) {
        List<ConversationParticipantPayload> rows = new ArrayList<ConversationParticipantPayload>();
        if (context.buyer != null) {
            rows.add(toConversationParticipant(context.buyer, viewer));
        }
        if (context.seller != null) {
            rows.add(toConversationParticipant(context.seller, viewer));
        }
        rows.add(new ConversationParticipantPayload(
            "CUSTOMER_SERVICE",
            renderSenderRole("CUSTOMER_SERVICE"),
            safeText(context.supportDisplayName, SUPPORT_NAME),
            viewer != null && "CUSTOMER_SERVICE".equals(viewer.roleCode) ? viewer.avatarUrl : null,
            viewer != null && "CUSTOMER_SERVICE".equals(viewer.roleCode)
        ));
        return rows;
    }

    private ConversationParticipantPayload toConversationParticipant(Participant participant, Participant viewer) {
        return new ConversationParticipantPayload(
            participant.roleCode,
            renderSenderRole(participant.roleCode),
            participant.displayName,
            participant.avatarUrl,
            viewer != null && Objects.equals(viewer.userId, participant.userId)
        );
    }

    private MessagePayload toMessagePayload(ImMessageDO item, Participant viewer, Map<Long, AuthUserDO> users) {
        String fileUrl = previewNullable(item.getFileKey());
        String avatarUrl = null;
        if (item.getSenderUserId() != null) {
            AuthUserDO sender = users.get(item.getSenderUserId());
            avatarUrl = previewNullable(sender == null ? null : sender.getAvatarKey());
        }
        boolean mine = viewer.userId != null && Objects.equals(viewer.userId, item.getSenderUserId());
        return new MessagePayload(
            item.getId(),
            item.getSenderRole(),
            renderSenderRole(item.getSenderRole()),
            item.getSenderName(),
            avatarUrl,
            mine,
            item.getMessageType(),
            item.getContentText(),
            fileUrl,
            item.getFileName(),
            formatTime(item.getCreatedAt())
        );
    }

    private void pushConversationRefresh(ConversationContext context, ImConversationDO conversation, List<ImMessageDO> messages, String reason) {
        long latestMessageId = lastMessageId(messages);
        ConversationRefreshEvent conversationEvent = new ConversationRefreshEvent(
            context.conversationNo,
            context.sourceOrderNo,
            context.sceneCode,
            latestMessageId,
            reason,
            formatTime(conversation.getLastMessageAt())
        );
        messagingTemplate.convertAndSend("/topic/im/" + context.conversationNo, conversationEvent);
        String senderRole = resolveWorkbenchSenderRole(messages);
        messagingTemplate.convertAndSend("/topic/im/workbench", new WorkbenchRefreshEvent(
            context.conversationNo,
            context.sceneCode,
            context.sourceOrderNo,
            context.title,
            safeText(conversation.getLastMessageExcerpt(), "等待消息"),
            formatTime(conversation.getLastMessageAt()),
            context.statusLabel,
            reason,
            latestMessageId,
            senderRole,
            resolveWorkbenchEventType(reason, senderRole)
        ));
    }

    private String resolveWorkbenchSenderRole(List<ImMessageDO> messages) {
        if (messages == null || messages.isEmpty()) {
            return "SYSTEM";
        }
        ImMessageDO latest = messages.get(messages.size() - 1);
        return safeText(latest.getSenderRole(), "SYSTEM");
    }

    private String resolveWorkbenchEventType(String reason, String senderRole) {
        if ("INIT".equals(reason)) {
            return "INIT";
        }
        if ("BUYER".equals(senderRole) || "SELLER".equals(senderRole)) {
            return "USER_MESSAGE";
        }
        if ("CUSTOMER_SERVICE".equals(senderRole)) {
            return "SUPPORT_MESSAGE";
        }
        return "SYSTEM_MESSAGE";
    }

    private String buildWelcomeContent(ConversationContext context) {
        if ("BOOSTING_ORDER".equals(context.sceneCode)) {
            return "欢迎进入代肝客服会话，买家可在此咨询排期、进度和售后问题，平台客服会全程跟进。";
        }
        return "欢迎进入交易群聊，买家、卖家可在此沟通账号交接事宜，平台客服全程监管。";
    }

    private ImMessageDO systemMessage(String conversationNo, String title, String content, LocalDateTime createdAt) {
        ImMessageDO row = new ImMessageDO();
        row.setConversationNo(conversationNo);
        row.setSenderRole("SYSTEM");
        row.setSenderName("系统消息");
        row.setMessageType("SYSTEM");
        row.setContentText(title + "： " + content);
        row.setCreatedAt(createdAt);
        return row;
    }

    private ImMessageDO supportMessage(String conversationNo, Long senderUserId, String senderName, String content, LocalDateTime createdAt) {
        ImMessageDO row = new ImMessageDO();
        row.setConversationNo(conversationNo);
        row.setSenderRole("CUSTOMER_SERVICE");
        row.setSenderUserId(senderUserId);
        row.setSenderName(senderName);
        row.setMessageType("TEXT");
        row.setContentText(content);
        row.setCreatedAt(createdAt);
        return row;
    }

    private void insertMessage(ImMessageDO message) {
        imMessageMapper.insert(message);
        log.info("mysql insert success target=im_message conversationNo={} senderRole={} type={}",
            message.getConversationNo(), message.getSenderRole(), message.getMessageType());
    }

    private void updateConversationLatest(ImConversationDO conversation, String excerpt, LocalDateTime time) {
        conversation.setLastMessageExcerpt(excerpt);
        conversation.setLastMessageAt(time);
        conversation.setUpdatedAt(LocalDateTime.now());
        imConversationMapper.updateById(conversation);
        log.info("mysql update success target=im_conversation conversationNo={} lastMessageAt={}",
            conversation.getConversationNo(), time);
    }

    private Map<Long, AuthUserDO> loadUsers(List<Long> userIds) {
        List<Long> normalized = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        return authUserMapper.selectList(Wrappers.<AuthUserDO>lambdaQuery().in(AuthUserDO::getId, normalized))
            .stream()
            .collect(Collectors.toMap(AuthUserDO::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private long lastMessageId(List<ImMessageDO> messages) {
        if (messages.isEmpty()) {
            return 0L;
        }
        return messages.get(messages.size() - 1).getId() == null ? 0L : messages.get(messages.size() - 1).getId();
    }

    private String resolveMessageType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "TEXT";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
            return "IMAGE";
        }
        return "FILE";
    }

    private String buildExcerpt(ImMessageDO message) {
        if ("IMAGE".equals(message.getMessageType())) {
            return "[图片] " + safeText(message.getFileName(), "图片消息");
        }
        if ("FILE".equals(message.getMessageType())) {
            return "[文件] " + safeText(message.getFileName(), "文件消息");
        }
        return safeText(message.getContentText(), "新消息");
    }

    private String renderSceneLabel(String sceneCode) {
        if ("BOOSTING_ORDER".equals(sceneCode)) {
            return "代肝客服";
        }
        if ("LISTING_CONSULT".equals(sceneCode)) {
            return "售前咨询";
        }
        return "交易群聊";
    }

    private String renderSenderRole(String senderRole) {
        if ("BUYER".equals(senderRole)) {
            return "买家";
        }
        if ("SELLER".equals(senderRole)) {
            return "卖家";
        }
        if ("CUSTOMER_SERVICE".equals(senderRole)) {
            return "平台客服";
        }
        return "系统";
    }

    private boolean isUserVisibleConversation(ImConversationDO conversation) {
        if (conversation == null || !"TRADE_ORDER".equals(conversation.getSceneCode())) {
            return true;
        }
        TradeOrderDO tradeOrder = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getChatGroupNo, conversation.getConversationNo())
                .last("LIMIT 1")
        );
        return tradeOrder != null && isPaidTradeStatus(tradeOrder.getStatus());
    }

    private boolean isPaidTradeStatus(String status) {
        return Arrays.asList("WAITING_TRADE", "IN_PROGRESS", "COMPLETED", "REFUND_PENDING", "AFTER_SALE").contains(status);
    }

    private String renderTradeStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return "待付款";
        }
	        if ("WAITING_TRADE".equals(status)) {
	            return "交易中";
	        }
        if ("IN_PROGRESS".equals(status)) {
            return "交易中";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("AFTER_SALE".equals(status)) {
            return "售后中";
        }
        return "已关闭";
    }

    private String renderBoostingStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return "待付款";
        }
        if ("WAITING_SERVICE".equals(status)) {
            return "待代肝";
        }
        if ("IN_SERVICE".equals(status)) {
            return "代肝中";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("AFTER_SALE".equals(status)) {
            return "售后中";
        }
        return "已取消";
    }

    private String formatAmount(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return "¥" + safe.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "—" : DISPLAY_TIME_FORMATTER.format(value);
    }

    private String previewNullable(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        try {
            return ossStorageService.previewUrl(objectKey.trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String safeText(String value, String fallback) {
        String normalized = trimNullable(value);
        return normalized == null ? fallback : normalized;
    }

    private AccountListingDO requirePublishedListing(String listingNo) {
        AccountListingDO listing = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getListingNo, listingNo)
                .last("LIMIT 1")
        );
        if (listing == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在");
        }
        if (!"PUBLISHED".equalsIgnoreCase(safeText(listing.getStatus(), ""))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前商品未上架，暂不能发起咨询");
        }
        return listing;
    }

    private AuthUserDO requireUser(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        return user;
    }

    private String buildListingConsultationNo(Long userId, String listingNo) {
        String digest = UUID.nameUUIDFromBytes((safeText(listingNo, "") + ":" + userId).getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
        return "LC" + digest.substring(0, 30);
    }

    private String resolveSellerDisplayName(AccountListingDO listing) {
        String nickname = safeText(listing.getSellerNickname(), "卖家");
        if ("STUDIO".equalsIgnoreCase(listing.getSellerType())) {
            String studioName = trimNullable(listing.getStudioName());
            return studioName == null ? nickname : studioName;
        }
        return nickname;
    }

    private String renderSellerType(String sellerType) {
        return "STUDIO".equalsIgnoreCase(trimNullable(sellerType)) ? "工作室" : "个人卖家";
    }

    public static class SendMessageCommand {
        private final String text;
        private final String fileKey;
        private final String fileName;

        public SendMessageCommand(String text, String fileKey, String fileName) {
            this.text = text;
            this.fileKey = fileKey;
            this.fileName = fileName;
        }

        public String getText() { return text; }
        public String getFileKey() { return fileKey; }
        public String getFileName() { return fileName; }
    }

	    public static class ConversationPayload {
        private final String conversationNo;
        private final String sceneCode;
        private final String sceneLabel;
        private final String sourceOrderNo;
        private final String listingNo;
        private final String title;
        private final String description;
        private final String statusLabel;
        private final String supportDisplayName;
        private final String currentUserRole;
        private final String currentUserName;
        private final String currentUserAvatarUrl;
        private final List<ConversationParticipantPayload> participants;
        private final List<SummaryItem> summaryItems;
        private final List<MessagePayload> messages;

        public ConversationPayload(
            String conversationNo,
            String sceneCode,
            String sceneLabel,
            String sourceOrderNo,
            String listingNo,
            String title,
            String description,
            String statusLabel,
            String supportDisplayName,
            String currentUserRole,
            String currentUserName,
            String currentUserAvatarUrl,
            List<ConversationParticipantPayload> participants,
            List<SummaryItem> summaryItems,
            List<MessagePayload> messages
        ) {
            this.conversationNo = conversationNo;
            this.sceneCode = sceneCode;
            this.sceneLabel = sceneLabel;
            this.sourceOrderNo = sourceOrderNo;
            this.listingNo = listingNo;
            this.title = title;
            this.description = description;
            this.statusLabel = statusLabel;
            this.supportDisplayName = supportDisplayName;
            this.currentUserRole = currentUserRole;
            this.currentUserName = currentUserName;
            this.currentUserAvatarUrl = currentUserAvatarUrl;
            this.participants = participants;
            this.summaryItems = summaryItems;
            this.messages = messages;
        }

        public String getConversationNo() { return conversationNo; }
        public String getSceneCode() { return sceneCode; }
        public String getSceneLabel() { return sceneLabel; }
        public String getSourceOrderNo() { return sourceOrderNo; }
        public String getListingNo() { return listingNo; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getStatusLabel() { return statusLabel; }
        public String getSupportDisplayName() { return supportDisplayName; }
        public String getCurrentUserRole() { return currentUserRole; }
        public String getCurrentUserName() { return currentUserName; }
        public String getCurrentUserAvatarUrl() { return currentUserAvatarUrl; }
        public List<ConversationParticipantPayload> getParticipants() { return participants; }
        public List<SummaryItem> getSummaryItems() { return summaryItems; }
	        public List<MessagePayload> getMessages() { return messages; }
	    }

	    public static class LatestConversationResult {
	        private final String conversationNo;

	        public LatestConversationResult(String conversationNo) {
	            this.conversationNo = conversationNo;
	        }

	        public String getConversationNo() {
	            return conversationNo;
	        }
	    }

	    public static class ConversationParticipantPayload {
        private final String roleCode;
        private final String roleLabel;
        private final String displayName;
        private final String avatarUrl;
        private final boolean currentUser;

        public ConversationParticipantPayload(
            String roleCode,
            String roleLabel,
            String displayName,
            String avatarUrl,
            boolean currentUser
        ) {
            this.roleCode = roleCode;
            this.roleLabel = roleLabel;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
            this.currentUser = currentUser;
        }

        public String getRoleCode() { return roleCode; }
        public String getRoleLabel() { return roleLabel; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public boolean isCurrentUser() { return currentUser; }
    }

    public static class WorkbenchPayload {
        private final String supportDisplayName;
        private final String supportAvatarUrl;
        private final List<WorkbenchConversationItem> rows;
        private final int totalCount;
        private final long boostingCount;
        private final long tradeCount;

        public WorkbenchPayload(
            String supportDisplayName,
            String supportAvatarUrl,
            List<WorkbenchConversationItem> rows,
            int totalCount,
            long boostingCount,
            long tradeCount
        ) {
            this.supportDisplayName = supportDisplayName;
            this.supportAvatarUrl = supportAvatarUrl;
            this.rows = rows;
            this.totalCount = totalCount;
            this.boostingCount = boostingCount;
            this.tradeCount = tradeCount;
        }

        public String getSupportDisplayName() { return supportDisplayName; }
        public String getSupportAvatarUrl() { return supportAvatarUrl; }
        public List<WorkbenchConversationItem> getRows() { return rows; }
        public int getTotalCount() { return totalCount; }
        public long getBoostingCount() { return boostingCount; }
        public long getTradeCount() { return tradeCount; }
    }

    public static class WorkbenchConversationItem {
        private final String conversationNo;
        private final String sceneCode;
        private final String sceneLabel;
        private final String sourceOrderNo;
        private final String title;
        private final String description;
        private final String statusLabel;
        private final String buyerName;
        private final String counterpartyName;
        private final String lastMessageExcerpt;
        private final String lastMessageAt;
        private final LocalDateTime lastMemberMessageTime;
        private final boolean closedOrder;
        private final long latestMessageId;
        private final boolean unread;
        private final long unreadCount;

        public WorkbenchConversationItem(
            String conversationNo,
            String sceneCode,
            String sceneLabel,
            String sourceOrderNo,
            String title,
            String description,
            String statusLabel,
            String buyerName,
            String counterpartyName,
            String lastMessageExcerpt,
            String lastMessageAt,
            LocalDateTime lastMemberMessageTime,
            boolean closedOrder,
            long latestMessageId,
            boolean unread,
            long unreadCount
        ) {
            this.conversationNo = conversationNo;
            this.sceneCode = sceneCode;
            this.sceneLabel = sceneLabel;
            this.sourceOrderNo = sourceOrderNo;
            this.title = title;
            this.description = description;
            this.statusLabel = statusLabel;
            this.buyerName = buyerName;
            this.counterpartyName = counterpartyName;
            this.lastMessageExcerpt = lastMessageExcerpt;
            this.lastMessageAt = lastMessageAt;
            this.lastMemberMessageTime = lastMemberMessageTime;
            this.closedOrder = closedOrder;
            this.latestMessageId = latestMessageId;
            this.unread = unread;
            this.unreadCount = unreadCount;
        }

        public String getConversationNo() { return conversationNo; }
        public String getSceneCode() { return sceneCode; }
        public String getSceneLabel() { return sceneLabel; }
        public String getSourceOrderNo() { return sourceOrderNo; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getStatusLabel() { return statusLabel; }
        public String getBuyerName() { return buyerName; }
        public String getCounterpartyName() { return counterpartyName; }
        public String getLastMessageExcerpt() { return lastMessageExcerpt; }
        public String getLastMessageAt() { return lastMessageAt; }
        public long getLatestMessageId() { return latestMessageId; }
        public boolean isUnread() { return unread; }
        public long getUnreadCount() { return unreadCount; }
    }

    public static class SummaryItem {
        private final String label;
        private final String value;

        public SummaryItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
    }

    public static class MessagePayload {
        private final Long id;
        private final String senderRole;
        private final String senderRoleLabel;
        private final String senderName;
        private final String avatarUrl;
        private final boolean mine;
        private final String messageType;
        private final String text;
        private final String fileUrl;
        private final String fileName;
        private final String createdAt;

        public MessagePayload(
            Long id,
            String senderRole,
            String senderRoleLabel,
            String senderName,
            String avatarUrl,
            boolean mine,
            String messageType,
            String text,
            String fileUrl,
            String fileName,
            String createdAt
        ) {
            this.id = id;
            this.senderRole = senderRole;
            this.senderRoleLabel = senderRoleLabel;
            this.senderName = senderName;
            this.avatarUrl = avatarUrl;
            this.mine = mine;
            this.messageType = messageType;
            this.text = text;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getSenderRole() { return senderRole; }
        public String getSenderRoleLabel() { return senderRoleLabel; }
        public String getSenderName() { return senderName; }
        public String getAvatarUrl() { return avatarUrl; }
        public boolean isMine() { return mine; }
        public String getMessageType() { return messageType; }
        public String getText() { return text; }
        public String getFileUrl() { return fileUrl; }
        public String getFileName() { return fileName; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class ReadMarkResult {
        private final String conversationNo;
        private final long lastReadMessageId;

        public ReadMarkResult(String conversationNo, long lastReadMessageId) {
            this.conversationNo = conversationNo;
            this.lastReadMessageId = lastReadMessageId;
        }

        public String getConversationNo() { return conversationNo; }
        public long getLastReadMessageId() { return lastReadMessageId; }
    }

    public static class ConversationRefreshEvent {
        private final String conversationNo;
        private final String sourceOrderNo;
        private final String sceneCode;
        private final long latestMessageId;
        private final String reason;
        private final String lastMessageAt;

        public ConversationRefreshEvent(
            String conversationNo,
            String sourceOrderNo,
            String sceneCode,
            long latestMessageId,
            String reason,
            String lastMessageAt
        ) {
            this.conversationNo = conversationNo;
            this.sourceOrderNo = sourceOrderNo;
            this.sceneCode = sceneCode;
            this.latestMessageId = latestMessageId;
            this.reason = reason;
            this.lastMessageAt = lastMessageAt;
        }

        public String getConversationNo() { return conversationNo; }
        public String getSourceOrderNo() { return sourceOrderNo; }
        public String getSceneCode() { return sceneCode; }
        public long getLatestMessageId() { return latestMessageId; }
        public String getReason() { return reason; }
        public String getLastMessageAt() { return lastMessageAt; }
    }

    public static class WorkbenchRefreshEvent {
        private final String conversationNo;
        private final String sceneCode;
        private final String sourceOrderNo;
        private final String title;
        private final String lastMessageExcerpt;
        private final String lastMessageAt;
        private final String statusLabel;
        private final String reason;
        private final long latestMessageId;
        private final String senderRole;
        private final String eventType;

        public WorkbenchRefreshEvent(
            String conversationNo,
            String sceneCode,
            String sourceOrderNo,
            String title,
            String lastMessageExcerpt,
            String lastMessageAt,
            String statusLabel,
            String reason,
            long latestMessageId,
            String senderRole,
            String eventType
        ) {
            this.conversationNo = conversationNo;
            this.sceneCode = sceneCode;
            this.sourceOrderNo = sourceOrderNo;
            this.title = title;
            this.lastMessageExcerpt = lastMessageExcerpt;
            this.lastMessageAt = lastMessageAt;
            this.statusLabel = statusLabel;
            this.reason = reason;
            this.latestMessageId = latestMessageId;
            this.senderRole = senderRole;
            this.eventType = eventType;
        }

        public String getConversationNo() { return conversationNo; }
        public String getSceneCode() { return sceneCode; }
        public String getSourceOrderNo() { return sourceOrderNo; }
        public String getTitle() { return title; }
        public String getLastMessageExcerpt() { return lastMessageExcerpt; }
        public String getLastMessageAt() { return lastMessageAt; }
        public String getStatusLabel() { return statusLabel; }
        public String getReason() { return reason; }
        public long getLatestMessageId() { return latestMessageId; }
        public String getSenderRole() { return senderRole; }
        public String getEventType() { return eventType; }
    }

    private static class ConversationContext {
        private final String conversationNo;
        private final String sceneCode;
        private final String sourceOrderNo;
        private final String listingNo;
        private final String title;
        private final String description;
        private final String statusLabel;
        private final String supportDisplayName;
        private final Participant buyer;
        private final Participant seller;
        private final List<SummaryItem> summaryItems;

        private ConversationContext(
            String conversationNo,
            String sceneCode,
            String sourceOrderNo,
            String listingNo,
            String title,
            String description,
            String statusLabel,
            String supportDisplayName,
            Participant buyer,
            Participant seller,
            List<SummaryItem> summaryItems
        ) {
            this.conversationNo = conversationNo;
            this.sceneCode = sceneCode;
            this.sourceOrderNo = sourceOrderNo;
            this.listingNo = listingNo;
            this.title = title;
            this.description = description;
            this.statusLabel = statusLabel;
            this.supportDisplayName = supportDisplayName;
            this.buyer = buyer;
            this.seller = seller;
            this.summaryItems = summaryItems;
        }

        private Participant requireParticipant(Long userId) {
            if (buyer != null && Objects.equals(buyer.userId, userId)) {
                return buyer;
            }
            if (seller != null && Objects.equals(seller.userId, userId)) {
                return seller;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不在该会话中");
        }
    }

    private static class Participant {
        private final Long userId;
        private final String roleCode;
        private final String displayName;
        private final String avatarUrl;

        private Participant(Long userId, String roleCode, String displayName, String avatarUrl) {
            this.userId = userId;
            this.roleCode = roleCode;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }
    }
}
