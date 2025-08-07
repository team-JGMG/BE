package org.bobj.fcm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;
import org.bobj.fcm.dto.FcmMessage;
import org.bobj.fcm.dto.request.FcmRequestDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService{

    private final String API_URL = "https://fcm.googleapis.com/v1/projects/half-to-half/messages:send";
    private final ObjectMapper objectMapper;
    private final FirebaseMessaging firebaseMessaging;
    private static final int BATCH_SIZE = 500;


    // 메시지를 구성하고 토큰을 받아서 FCM으로 메시지를 처리한다.
    public void sendMessageTo(FcmRequestDto fcmRequestDto) throws IOException {
        String message = makeMessage(fcmRequestDto.getDeviceToken(), fcmRequestDto.getTitle(), fcmRequestDto.getBody());

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message, // 만든 message body에 넣기
                MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken()) // header에 포함
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                .build();
        Response response = client.newCall(request).execute(); // 요청 보냄

        System.out.println(response.body().string());
    }

    // FCM 전송 정보를 기반으로 메시지를 구성한다. (Object -> String)
    private String makeMessage(String targetToken, String title, String body) throws JsonProcessingException { // JsonParseException, JsonProcessingException
        FcmMessage fcmMessage = FcmMessage.builder()
                .message(FcmMessage.Message.builder()
                        .token(targetToken)
                        .data(FcmMessage.Data.builder()
                                .title(title)
                                .body(body)
                                .build()
                        ).build()).validateOnly(false).build();
        return objectMapper.writeValueAsString(fcmMessage);
    }

    // Firebase Admin SDK의 비공개 키를 참조하여 Bearer 토큰을 발급 받는다.
    private String getAccessToken() throws IOException {
        final String firebaseConfigPath = "firebase/serviceAccountKey.json";

        try {
            final GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

            googleCredentials.refreshIfExpired();
            log.info("access token: {}",googleCredentials.getAccessToken());
            return googleCredentials.getAccessToken().getTokenValue();

        } catch (IOException e) {
            throw new IOException("Failed to get Google Access Token", e);
        }
    }

    @Override
    public void sendMulticast(List<String> tokens, String title, String body) {
        // tokens를 500개씩 묶어서 배치 전송
        List<List<String>> batches = Lists.partition(tokens, BATCH_SIZE);

        for (List<String> batch : batches) {
            // Data 타입으로 메시지를 보냄
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .putAllData(Map.of(
                            "title", title,
                            "body", body
                    ))
                    .addAllTokens(batch)
                    .build();

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage, false);
                log.info("FCM 배치 발송 성공: {}, 실패: {}", response.getSuccessCount(), response.getFailureCount());

                // 실패한 응답 처리
//                if (response.getFailureCount() > 0) {
//                    handleFailedResponses(batch, response);
//                }
            } catch (FirebaseMessagingException e) {
                log.error("FCM 배치 발송 중 오류 발생", e);
            }
        }
    }

    // ... (필요하다면 실패한 응답 처리 로직 추가)
//    private void handleFailedResponses(List<String> tokens, BatchResponse response) {
//        for (int i = 0; i < response.getResponses().size(); i++) {
//            SendResponse sendResponse = response.getResponses().get(i);
//            if (!sendResponse.isSuccessful()) {
//                log.warn("FCM 발송 실패 토큰: {}, 오류: {}", tokens.get(i), sendResponse.getException().getMessage());
//                // 토큰 삭제 로직 등 추가 가능
//            }
//        }
//    }


}
