package org.example.bobj.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Swagger 테스트 API")
public class TestController {

    @GetMapping("/test")
    @ApiOperation(value = "헬로 응답", notes = "단순한 연결 테스트용 API입니다.")
    public String hello() {
        return "Hello";
    }
}