package org.bobj.funding.controller;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fundings")
@RequiredArgsConstructor
@Log4j2
@Api(tags="펀딩 API")
public class FundingController {
}
