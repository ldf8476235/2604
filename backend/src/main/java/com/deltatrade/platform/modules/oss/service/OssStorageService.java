package com.deltatrade.platform.modules.oss.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.oss.config.PlatformOssProperties;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class OssStorageService {

    private static final Logger log = LoggerFactory.getLogger(OssStorageService.class);

    private final PlatformOssProperties properties;

    public OssStorageService(PlatformOssProperties properties) {
        this.properties = properties;
    }

    public OssFileTicket createUploadTicket(String businessScope, String filename, String contentType) {
        String objectKey = buildObjectKey(businessScope, filename);
        if (!properties.isEnabled()) {
            log.warn("oss disabled, fallback upload ticket objectKey={}", objectKey);
            return new OssFileTicket(objectKey, properties.getPublicBaseUrl() + "/" + objectKey, Instant.now().plusSeconds(900));
        }
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(properties.getBucket(), objectKey, HttpMethod.PUT);
            if (contentType != null && !contentType.trim().isEmpty()) {
                request.setContentType(contentType.trim());
            }
            request.setExpiration(java.util.Date.from(Instant.now().plusSeconds(900)));
            URL url = ossClient.generatePresignedUrl(request);
            log.info("oss upload ticket generated bucket={} objectKey={} contentType={}", properties.getBucket(), objectKey, contentType);
            return new OssFileTicket(objectKey, url.toString(), Instant.now().plusSeconds(900));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "OSS 上传签名生成失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String previewUrl(String objectKey) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().trim().isEmpty()) {
            return buildPublicUrl(objectKey);
        }
        if (!properties.isEnabled()) {
            return buildPublicUrl(objectKey);
        }
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
            URL url = generatePreviewUrl(ossClient, objectKey);
            log.info("oss preview url generated bucket={} objectKey={} expireSeconds={}",
                properties.getBucket(), objectKey, properties.getPreviewExpireSeconds());
            return url.toString();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "OSS 预览地址生成失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public OssUploadedFile uploadFile(String businessScope, String filename, String contentType, long contentLength, InputStream inputStream) {
        String objectKey = buildObjectKey(businessScope, filename);
        if (!properties.isEnabled()) {
            log.warn("oss disabled, fallback upload objectKey={} contentType={} contentLength={}", objectKey, contentType, contentLength);
            return new OssUploadedFile(objectKey, properties.getPublicBaseUrl() + "/" + objectKey);
        }
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }
            if (contentType != null && !contentType.trim().isEmpty()) {
                metadata.setContentType(contentType.trim());
            }
            ossClient.putObject(properties.getBucket(), objectKey, inputStream, metadata);
            URL previewUrl = generatePreviewUrl(ossClient, objectKey);
            log.info("oss proxy upload success bucket={} objectKey={} contentType={} contentLength={}",
                properties.getBucket(), objectKey, contentType, contentLength);
            return new OssUploadedFile(objectKey, previewUrl.toString());
        } catch (Exception exception) {
            log.error("oss proxy upload failed bucket={} objectKey={} contentType={} contentLength={}",
                properties.getBucket(), objectKey, contentType, contentLength, exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "OSS 上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    private String buildObjectKey(String businessScope, String filename) {
        return String.format(
            "%s/%s/%s/%s",
            properties.getRootPath(),
            businessScope,
            LocalDate.now(ZoneOffset.UTC),
            filename
        );
    }

    private URL generatePreviewUrl(OSS ossClient, String objectKey) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(properties.getBucket(), objectKey, HttpMethod.GET);
        request.setExpiration(java.util.Date.from(Instant.now().plusSeconds(properties.getPreviewExpireSeconds())));
        return ossClient.generatePresignedUrl(request);
    }

    private String buildPublicUrl(String objectKey) {
        String baseUrl = properties.getPublicBaseUrl() == null ? "" : properties.getPublicBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + objectKey;
        }
        return baseUrl + "/" + objectKey;
    }

    public static class OssFileTicket {
        private final String objectKey;
        private final String uploadUrl;
        private final Instant expireAt;

        public OssFileTicket(String objectKey, String uploadUrl, Instant expireAt) {
            this.objectKey = objectKey;
            this.uploadUrl = uploadUrl;
            this.expireAt = expireAt;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public Instant getExpireAt() {
            return expireAt;
        }
    }

    public static class OssUploadedFile {
        private final String objectKey;
        private final String previewUrl;

        public OssUploadedFile(String objectKey, String previewUrl) {
            this.objectKey = objectKey;
            this.previewUrl = previewUrl;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getPreviewUrl() {
            return previewUrl;
        }
    }
}
