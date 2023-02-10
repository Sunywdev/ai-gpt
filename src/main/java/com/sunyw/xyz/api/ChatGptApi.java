package com.sunyw.xyz.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 * 名称: XX定义
 * 功能: <功能详细描述>
 * 方法: <方法简述-方法描述>
 * 版本: 1.0
 * 作者: sunyw
 * 说明: 说明描述
 * 时间: 2023/02/07 12:49
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ChatGptApi {

    @Autowired
    private AsyncService asyncService;

    @PostMapping("/test")
    public Server test(@RequestBody Server server) {

        return server;
    }


    @PostMapping("/question")
    public void question(HttpServletRequest request) {
        long l = System.currentTimeMillis();
        asyncService.listen(getBody(request));
        log.info("消息接收结束:耗时:[{}]毫秒", (System.currentTimeMillis() - l));
    }
    private String getBody(HttpServletRequest request) {
        try (InputStream inputStream = request.getInputStream()) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("get input error", e);
        }
        return null;
    }


}
