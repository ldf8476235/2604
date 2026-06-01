package com.deltatrade.platform.modules.oss.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import java.io.IOException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oss")
public class OssController {

    private final OssStorageService ossStorageService;

    public OssController(OssStorageService ossStorageService) {
        this.ossStorageService = ossStorageService;
    }

    @PostMapping("/upload-ticket")
    public ApiResponse<OssStorageService.OssFileTicket> createUploadTicket(@Valid @RequestBody UploadTicketRequest request) {
        return ApiResponse.success(
            ossStorageService.createUploadTicket(request.getBusinessScope(), request.getFilename(), request.getContentType()),
            MDC.get("traceId")
        );
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OssStorageService.OssUploadedFile> upload(
        @RequestParam("businessScope") String businessScope,
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        return ApiResponse.success(
            ossStorageService.uploadFile(
                businessScope,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
            ),
            MDC.get("traceId")
        );
    }

    @GetMapping("/preview")
    public ApiResponse<Map<String, String>> preview(@RequestParam("objectKey") String objectKey) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("previewUrl", ossStorageService.previewUrl(objectKey));
        result.put("objectKey", objectKey);
        return ApiResponse.success(result, MDC.get("traceId"));
    }

    public static class UploadTicketRequest {
        @NotBlank(message = "业务域不能为空")
        private String businessScope;
        @NotBlank(message = "文件名不能为空")
        private String filename;
        private String contentType;

        public String getBusinessScope() {
            return businessScope;
        }

        public void setBusinessScope(String businessScope) {
            this.businessScope = businessScope;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }
}
