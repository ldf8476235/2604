package com.deltatrade.platform.modules.im.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.modules.console.service.ConsoleAccessService;
import com.deltatrade.platform.modules.im.service.ImService;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/im")
public class ImWorkbenchController {

    private final ImService imService;
    private final ConsoleAccessService consoleAccessService;

    public ImWorkbenchController(ImService imService, ConsoleAccessService consoleAccessService) {
        this.imService = imService;
        this.consoleAccessService = consoleAccessService;
    }

    @GetMapping("/conversations")
    public ApiResponse<ImService.WorkbenchPayload> conversations(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "sceneCode", required = false) String sceneCode
    ) {
        Long userId = requireImReadAccess();
        return ApiResponse.success(
            imService.loadWorkbench(userId, keyword, sceneCode),
            MDC.get("traceId")
        );
    }

    @GetMapping("/conversations/{conversationNo}")
    public ApiResponse<ImService.ConversationPayload> conversation(@PathVariable("conversationNo") String conversationNo) {
        Long userId = requireImReadAccess();
        return ApiResponse.success(
            imService.getWorkbenchConversation(userId, conversationNo),
            MDC.get("traceId")
        );
    }

    @PostMapping("/conversations/{conversationNo}/messages")
    public ApiResponse<ImService.ConversationPayload> sendMessage(
        @PathVariable("conversationNo") String conversationNo,
        @Valid @RequestBody SendMessageRequest request
    ) {
        Long userId = requireSupportAccess();
        return ApiResponse.success(
            imService.sendWorkbenchMessage(
                userId,
                conversationNo,
                new ImService.SendMessageCommand(request.getText(), request.getFileKey(), request.getFileName())
            ),
            MDC.get("traceId")
        );
    }

    private Long requireSupportAccess() {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        consoleAccessService.requireAdminAccess(principal, "support");
        return principal.getUserId();
    }

    private Long requireImReadAccess() {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        consoleAccessService.requireAnyAdminAccess(principal, "support", "order");
        return principal.getUserId();
    }

    public static class SendMessageRequest {
        @Size(max = 1000, message = "消息内容不能超过1000字")
        private String text;

        @Size(max = 255, message = "文件 key 过长")
        private String fileKey;

        @Size(max = 255, message = "文件名过长")
        private String fileName;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getFileKey() { return fileKey; }
        public void setFileKey(String fileKey) { this.fileKey = fileKey; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}
