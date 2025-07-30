//package org.bobj.payment.client;
//
//
//import java.net.http.HttpHeaders;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class PortOneClient {
//    private final RestTemplate restTemplate = new RestTemplate();
//
//
//    @Value("${portOne.api.url}")
//    private String portOneApiUrl;
//
//    @Value("${portOne.api.key}")
//    private String apiKey;
//
//    @Value("${portOne.api.secret}")
//    private String apiSecret;
//
//
//    private String getAccessToken() {
//        String url = portOneApiUrl + "/users/getToken";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        String body = String.format("{\"imp_key\":\"%s\", \"imp_secret\":\"%s\"}", apiKey, apiSecret);
//
//        HttpEntity<String> entity = new HttpEntity<>(body, headers);
//
//        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
//        // TODO: 실제 응답 파싱해서 access_token 리턴
//        return "access_token_placeholder";
//    }
//
//    public PortOnePaymentResponse getPaymentInfo(String impUid) {
//        String accessToken = getAccessToken();
//
//        String url = portOneApiUrl + "/payments/" + impUid;
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<Void> entity = new HttpEntity<>(headers);
//
//        ResponseEntity<PortOnePaymentResponse> response = restTemplate.exchange(
//            url, HttpMethod.GET, entity, PortOnePaymentResponse.class
//        );
//
//        return response.getBody();
//    }
//
//
//
//}
