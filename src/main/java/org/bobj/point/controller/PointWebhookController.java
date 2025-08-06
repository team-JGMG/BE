package org.bobj.point.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bobj.point.service.PointWebhookService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/point")
@Slf4j
public class PointWebhookController {
    private final PointWebhookService pointWebhookService;


}
