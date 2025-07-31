//package org.bobj.socket;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.bobj.common.ActiveSubscriptionsChecker;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.simp.user.SimpUserRegistry;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//import org.springframework.web.socket.messaging.SessionSubscribeEvent;
//import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class WebSocketEventListener {
//
//    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
//    private final SimpUserRegistry simpUserRegistry;
//
//    @EventListener
//    public void handleSubscribeEvent(SessionSubscribeEvent event) {
//
//        log.info("WebSocketEventListener 주입된 SimpUserRegistry 해시코드: {}", System.identityHashCode(simpUserRegistry)); // <-- 추가
//
//
//        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
//        String destination = (String) event.getMessage().getHeaders().get("simpDestination");
//        log.info("✅ 세션 {} 이 {} 토픽을 구독했습니다.", sessionId, destination);
//
//        // 해당 세션의 구독 목록에 현재 토픽 추가
//        sessionSubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(destination);
//
//        // --- 여기에서 호출 ---
//        logCurrentSubscribers(destination);
//    }
//
//    @EventListener
//    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
//        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
//        String destination = (String) event.getMessage().getHeaders().get("simpDestination");
//        log.info("❌ 세션 {} 이 {} 토픽 구독을 해제했습니다.", sessionId, destination);
//
//        // 해당 세션의 구독 목록에서 현재 토픽 제거
//        if (sessionSubscriptions.containsKey(sessionId)) {
//            sessionSubscriptions.get(sessionId).remove(destination);
//            // 만약 해당 세션이 더 이상 어떤 토픽도 구독하고 있지 않다면 맵에서 제거
//            if (sessionSubscriptions.get(sessionId).isEmpty()) {
//                sessionSubscriptions.remove(sessionId);
//            }
//        }
//
//        // --- 여기에서 호출 ---
//        logCurrentSubscribers(destination);
//    }
//
//    @EventListener
//    public void handleDisconnectEvent(SessionDisconnectEvent event) {
//        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
//        log.info("🔴 세션 {} 연결이 끊어졌습니다.", sessionId);
//
//        // 연결이 끊어진 세션이 구독했던 모든 토픽에 대해 구독자 수 업데이트 로깅을 하려면
//        // 해당 세션이 구독했던 토픽 목록을 가져와서 각각 logCurrentSubscribers를 호출해야 합니다.
//        // 예를 들어:
//        Set<String> subscribedTopics = sessionSubscriptions.remove(sessionId);
//        if (subscribedTopics != null) {
//            for (String topic : subscribedTopics) {
//                logCurrentSubscribers(topic); // 연결 해제로 인해 구독자 수가 줄어든 토픽에 대해 로깅
//            }
//        }
//        // 이 부분은 연결 끊김 시점에 어떤 토픽에서 구독 해제되는지 정확히 파악하여 로그를 남기는 로직입니다.
//        // 단, 세션이 끊어질 때 모든 구독이 자동으로 해지되므로, 명시적인 UNSUBSCRIBE 이벤트가 발생하지 않을 수 있어
//        // DISCONNECT 이벤트에서 처리하는 것이 중요합니다.
//    }
//
//    // 특정 토픽의 현재 구독자 수를 로깅하는 헬퍼 메서드
//    private void logCurrentSubscribers(String destination) {
//        long count = sessionSubscriptions.entrySet().stream()
//                .filter(entry -> entry.getValue().contains(destination))
//                .count();
//        log.info("📊 토픽 {} 에 {} 개의 클라이언트가 구독 중입니다. (EventListener 내부 집계)", destination, count);
//    }
//
//    // 외부에서 특정 세션이 구독 중인지 확인하는 메서드 (필요시)
//    public boolean isSessionSubscribedToTopic(String sessionId, String topic) {
//        return sessionSubscriptions.containsKey(sessionId) && sessionSubscriptions.get(sessionId).contains(topic);
//    }
//
//    // 특정 토픽의 현재 구독자 수를 반환하는 메서드 (외부에서 호출할 경우)
//    public int getSubscriberCountForTopic(String destination) {
//        return (int) sessionSubscriptions.entrySet().stream()
//                .filter(entry -> entry.getValue().contains(destination))
//                .count();
//    }
//
//}
