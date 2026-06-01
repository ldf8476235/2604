package com.deltatrade.platform.modules.console.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.admin.mapper.AdminRoleMapper;
import com.deltatrade.platform.modules.admin.mapper.AdminRoleMemberMapper;
import com.deltatrade.platform.modules.admin.model.AdminRoleDO;
import com.deltatrade.platform.modules.admin.model.AdminRoleMemberDO;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConsoleAccessService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAccessService.class);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private final AuthUserMapper authUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminRoleMemberMapper adminRoleMemberMapper;
    private final StudioProfileMapper studioProfileMapper;
    private final ObjectMapper objectMapper;

    public ConsoleAccessService(
        AuthUserMapper authUserMapper,
        AdminRoleMapper adminRoleMapper,
        AdminRoleMemberMapper adminRoleMemberMapper,
        StudioProfileMapper studioProfileMapper,
        ObjectMapper objectMapper
    ) {
        this.authUserMapper = authUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminRoleMemberMapper = adminRoleMemberMapper;
        this.studioProfileMapper = studioProfileMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> loadAdminSession(AuthPrincipal principal) {
        AuthUserDO user = requireActiveUser(principal.getUserId());
        ensureDefaultAdminMember(user.getId());
        List<AdminRoleDO> roles = loadAdminRoles(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未开通管理后台权限");
        }
        Set<String> permissions = collectPermissions(roles);
        List<Map<String, Object>> roleRows = new ArrayList<Map<String, Object>>();
        for (AdminRoleDO role : roles) {
            roleRows.add(mapOf(
                "roleCode", role.getRoleCode(),
                "roleName", renderRoleName(role.getRoleCode(), role.getRoleName())
            ));
        }
        log.info("admin session loaded userId={} roleCount={} permissionCount={}", user.getId(), roles.size(), permissions.size());
        return mapOf(
            "userId", user.getId(),
            "nickname", safeText(user.getNickname()),
            "phone", safeText(user.getPhone()),
            "roles", roleRows,
            "permissions", new ArrayList<String>(permissions)
        );
    }

    public void requireAdminAccess(AuthPrincipal principal, String permission) {
        AuthUserDO user = requireActiveUser(principal.getUserId());
        ensureDefaultAdminMember(user.getId());
        List<AdminRoleDO> roles = loadAdminRoles(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未开通管理后台权限");
        }
        if (StringUtils.hasText(permission) && !collectPermissions(roles).contains(permission.trim())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号缺少后台功能权限");
        }
    }

    public void requireAnyAdminAccess(AuthPrincipal principal, String... permissions) {
        AuthUserDO user = requireActiveUser(principal.getUserId());
        ensureDefaultAdminMember(user.getId());
        List<AdminRoleDO> roles = loadAdminRoles(user.getId());
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未开通管理后台权限");
        }
        if (permissions == null || permissions.length == 0) {
            return;
        }
        Set<String> ownedPermissions = collectPermissions(roles);
        for (String permission : permissions) {
            if (StringUtils.hasText(permission) && ownedPermissions.contains(permission.trim())) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号缺少后台功能权限");
    }

    public Map<String, Object> loadStudioSession(AuthPrincipal principal) {
        AuthUserDO user = requireActiveUser(principal.getUserId());
        StudioProfileDO studio = requireStudioProfile(user.getId());
        log.info("studio session loaded userId={} studioId={} active={}", user.getId(), studio.getId(), studio.getActive());
        return mapOf(
            "userId", user.getId(),
            "nickname", safeText(user.getNickname()),
            "phone", safeText(user.getPhone()),
            "studioId", studio.getId(),
            "studioName", safeText(studio.getStudioName()),
            "reviewStrategy", safeText(studio.getReviewStrategy()),
            "active", Boolean.TRUE.equals(studio.getActive())
        );
    }

    public void requireStudioAccess(AuthPrincipal principal) {
        requireActiveUser(principal.getUserId());
        requireStudioProfile(principal.getUserId());
    }

    private AuthUserDO requireActiveUser(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        if (!"ACTIVE".equalsIgnoreCase(defaultText(user.getAccountStatus(), "ACTIVE"))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号已被禁用，无法进入后台");
        }
        return user;
    }

    private StudioProfileDO requireStudioProfile(Long userId) {
        StudioProfileDO studio = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery().eq(StudioProfileDO::getOwnerUserId, userId).last("LIMIT 1")
        );
        if (studio == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前账号未开通工作室后台权限");
        }
        return studio;
    }

    private void ensureDefaultAdminMember(Long userId) {
        if (userId == null || userId.longValue() != 1L) {
            return;
        }
        AdminRoleDO role = adminRoleMapper.selectOne(
            Wrappers.<AdminRoleDO>lambdaQuery().eq(AdminRoleDO::getRoleCode, "SUPER_ADMIN").last("LIMIT 1")
        );
        if (role == null) {
            return;
        }
        AdminRoleMemberDO existing = adminRoleMemberMapper.selectOne(
            Wrappers.<AdminRoleMemberDO>lambdaQuery()
                .eq(AdminRoleMemberDO::getRoleId, role.getId())
                .eq(AdminRoleMemberDO::getUserId, userId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        AdminRoleMemberDO member = new AdminRoleMemberDO();
        member.setRoleId(role.getId());
        member.setUserId(userId);
        member.setCreatedAt(now);
        member.setUpdatedAt(now);
        adminRoleMemberMapper.insert(member);
        log.info("admin default super role granted userId={} roleId={}", userId, role.getId());
    }

    private List<AdminRoleDO> loadAdminRoles(Long userId) {
        List<AdminRoleMemberDO> members = adminRoleMemberMapper.selectList(
            Wrappers.<AdminRoleMemberDO>lambdaQuery().eq(AdminRoleMemberDO::getUserId, userId)
        );
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> roleIds = new LinkedHashSet<Long>();
        for (AdminRoleMemberDO member : members) {
            roleIds.add(member.getRoleId());
        }
        List<AdminRoleDO> roles = adminRoleMapper.selectList(
            Wrappers.<AdminRoleDO>lambdaQuery()
                .in(AdminRoleDO::getId, roleIds)
                .eq(AdminRoleDO::getStatus, "ACTIVE")
        );
        return roles == null ? Collections.<AdminRoleDO>emptyList() : roles;
    }

    private Set<String> collectPermissions(List<AdminRoleDO> roles) {
        Set<String> permissions = new LinkedHashSet<String>();
        for (AdminRoleDO role : roles) {
            permissions.addAll(readPermissions(role.getPermissionsJson()));
        }
        return permissions;
    }

    private List<String> readPermissions(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Collections.emptyList();
        }
        try {
            List<String> rows = objectMapper.readValue(payload, STRING_LIST_TYPE);
            return rows == null ? Collections.<String>emptyList() : rows;
        } catch (Exception exception) {
            log.warn("admin role permissions parse failed payload={}", payload, exception);
            return Collections.emptyList();
        }
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String renderRoleName(String roleCode, String roleName) {
        if ("SUPER_ADMIN".equalsIgnoreCase(roleCode)) {
            return "超级管理员";
        }
        if ("OPS_ADMIN".equalsIgnoreCase(roleCode)) {
            return "运营管理员";
        }
        if ("SERVICE_ADMIN".equalsIgnoreCase(roleCode)) {
            return "客服管理员";
        }
        if ("FINANCE_ADMIN".equalsIgnoreCase(roleCode)) {
            return "财务管理员";
        }
        return safeText(roleName);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
