package org.bobj.common.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
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

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    // 🔍 로컬 개발용 키 판별
    private boolean isDummyKey() {
        return "dummy-access-key".equals(accessKey);
    }

    public String upload(MultipartFile file) {
        // 🔐 더미 키일 경우 S3 업로드 우회
        if (isDummyKey()) {
            log.warn("S3 업로드 우회됨 - 로컬 개발용 dummy key 사용 중");
            return "https://example.com/dummy/" + UUID.randomUUID(); // 더미 URL 리턴
        }

        // ✅ 파일 크기 제한: 5MB 초과 시 예외 발생
        final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String key = "uploads/" + UUID.randomUUID() + ext;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(bucket, key, file.getInputStream(), metadata);

            return amazonS3.getUrl(bucket, key).toString();
        } catch (IOException e) {
            log.error("S3 업로드 실패", e);
            throw new RuntimeException("파일 업로드 실패");
        }
    }

    public String generatePresignedUrl(String fileName) {
        // 🔐 더미 키일 경우 presigned URL 생성 우회
        if (isDummyKey()) {
            log.warn("Presigned URL 생성 우회됨 - 로컬 개발용 dummy key 사용 중");
            return "https://example.com/dummy/presigned/" + fileName;
        }

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 3; // 3분간 유효
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
}
