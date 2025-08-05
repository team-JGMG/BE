package org.bobj.notification.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/*
메시지 구조
validateOnly: 메시지를 실제로 보내는 대신, 유효성 검사만 실행하려면 true로 설정 -> 보통 false로 고정함
*/
@Builder
@AllArgsConstructor
@Getter
public class FcmMessage {

    private boolean validateOnly;
    private Message message;
    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        private Notification notification;
        private String token;
    }
    @Builder
    @AllArgsConstructor
    @Getter
    public static class Notification {
        private String title;
        private String body;
    }
}
