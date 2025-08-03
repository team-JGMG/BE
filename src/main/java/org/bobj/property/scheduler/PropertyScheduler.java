package org.bobj.property.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bobj.property.service.PropertyService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PropertyScheduler {
    private final PropertyService propertyService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void runPropertySold(){
        propertyService.soldProperties();
    }
}
