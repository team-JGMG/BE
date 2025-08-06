package org.bobj.common.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, boolean isPublicRead) {
        final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String sanitizedFileName = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_")
                : "unknown";

        String folder = isPublicRead ? "uploads/photo/" : "uploads/document/";
        String key = folder + UUID.randomUUID() + "_" + sanitizedFileName;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            if (originalFilename != null) {
                metadata.addUserMetadata("original-filename", originalFilename);
            }

            if (isPublicRead) {
                amazonS3.putObject(
                        new PutObjectRequest(bucket, key, file.getInputStream(), metadata)
                );
            } else {
                amazonS3.putObject(bucket, key, file.getInputStream(), metadata);
            }

            return amazonS3.getUrl(bucket, key).toString();
        } catch (IOException e) {
            log.error("S3 업로드 실패", e);
            throw new RuntimeException("파일 업로드 실패");
        }
    }

    public String generatePresignedUrl(String key, String originalFilename) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 3; // 3분간 유효
        expiration.setTime(expTimeMillis);

        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        if (originalFilename != null && !originalFilename.isEmpty()) {
            responseHeaders.setContentDisposition("attachment; filename=\"" + originalFilename + "\"");
        }

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration)
                        .withResponseHeaders(responseHeaders);

        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public String getOriginalFilenameFromS3(String key) {
        ObjectMetadata metadata = amazonS3.getObjectMetadata(bucket, key);
        String originalFilename = metadata.getUserMetaDataOf("original-filename");
        return originalFilename != null ? originalFilename : key.substring(key.lastIndexOf('/') + 1);
    }
}
