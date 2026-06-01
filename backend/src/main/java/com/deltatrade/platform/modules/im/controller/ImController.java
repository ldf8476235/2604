package com.deltatrade.platform.modules.im.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.im.service.ImService;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/im")
public class ImController {

    private final ImService imService;

	    public ImController(ImService imService) {
	        this.imService = imService;
	    }

	    @GetMapping("/conversations/latest")
	    public ApiResponse<ImService.LatestConversationResult> latestConversation() {
	        return ApiResponse.success(
	            imService.getLatestConversation(AuthContext.requirePrincipal().getUserId()),
	            MDC.get("traceId")
	        );
	    }

	    @GetMapping("/conversations/{conversationNo}")
    public ApiResponse<ImService.ConversationPayload> conversation(@PathVariable("conversationNo") String conversationNo) {
        return ApiResponse.success(
            imService.getConversation(AuthContext.requirePrincipal().getUserId(), conversationNo),
            MDC.get("traceId")
        );
    }

    @PostMapping("/conversations/{conversationNo}/messages")
    public ApiResponse<ImService.ConversationPayload> sendMessage(
        @PathVariable("conversationNo") String conversationNo,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.success(
            imService.sendMessage(
                AuthContext.requirePrincipal().getUserId(),
                conversationNo,
                new ImService.SendMessageCommand(request.getText(), request.getFileKey(), request.getFileName())
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/conversations/{conversationNo}/read")
    public ApiResponse<ImService.ReadMarkResult> markRead(@PathVariable("conversationNo") String conversationNo) {
        return ApiResponse.success(
            imService.markRead(AuthContext.requirePrincipal().getUserId(), conversationNo),
            MDC.get("traceId")
        );
    }

    @PostMapping("/listings/{listingNo}/consultation")
    public ApiResponse<ImService.ConversationPayload> createListingConsultation(
        @PathVariable("listingNo") String listingNo,
        @Valid @RequestBody CreateListingConsultationRequest request
    ) {
        return ApiResponse.success(
            imService.openListingConsultation(AuthContext.requirePrincipal().getUserId(), listingNo, request.getPresetText()),
            MDC.get("traceId")
        );
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

    public static class CreateListingConsultationRequest {
        @Size(max = 500, message = "预设消息不能超过500字")
        private String presetText;

        public String getPresetText() { return presetText; }
        public void setPresetText(String presetText) { this.presetText = presetText; }
    }
}
