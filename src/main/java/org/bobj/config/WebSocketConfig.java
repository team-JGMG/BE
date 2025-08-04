package org.bobj.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); //메세지 수신 /topic/토픽번호

        registry.setApplicationDestinationPrefixes("/app");  //메세지 발행  /app로 시작하는 url패턴으로 메시지가 발행되면
        //@Controller객체의 @MessageMapping메서드로 메세지가 라우팅된다.
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/order-book") // 접속 엔드포인트, ws://localhost:8080/order-book
                .setAllowedOriginPatterns("*")
//                .setAllowedOrigins("*") // CORS 허용 - 프론트엔드 도메인 주소로 나중에 변경 필요
                .withSockJS(); //ws://가 아닌 http:// 엔드포인트를 사용할 수 있도록 해줌
    }

}
