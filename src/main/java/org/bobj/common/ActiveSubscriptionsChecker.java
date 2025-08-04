package org.bobj.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class ActiveSubscriptionsChecker {

    private final SimpUserRegistry simpUserRegistry;

    public int getSubscriberCountForTopic(String destination) {
        int count = 0;

        // 연결된 사용자(세션)들을 순회
        for (var user : simpUserRegistry.getUsers()) {
            // 각 사용자의 세션들 순회
            for (var session : user.getSessions()) {
                // 각 세션의 구독 정보 순회
                boolean subscribed = session.getSubscriptions().stream()
                        .anyMatch(subscription -> destination.equals(subscription.getDestination()));

                if (subscribed) {
                    count++;
                }
            }
        }
        return count;
    }

}