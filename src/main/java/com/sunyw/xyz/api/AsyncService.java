package com.sunyw.xyz.api;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sunyw.xyz.util.LocalCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 名称: XX定义
 * 功能: <功能详细描述>
 * 方法: <方法简述-方法描述>
 * 版本: 1.0
 * 作者: sunyw
 * 说明: 说明描述
 * 时间: 2023/02/07 15:29
 */
@Component
@Slf4j
public class AsyncService {

    @Value("${openai.token}")
    private String openAiToken;

    @Value("${openai.timeout}")
    private Integer timeOut;

    @Value("${feishu.gpt.appid}")
    private String gptAppId;

    @Value("${feishu.gpt.appSecret}")
    private String gptAppSecret;


    @Async("threadPoolTaskExecutor")
    public void listen(String body) {
        log.info("请求信息:[{}]", body);
        JSONObject messageJson = getMessageJson(body);
        String messageId = messageJson.getString("message_id");
        String text = getText(messageJson);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        JSONObject json = new JSONObject();
        //选择模型
        json.put("model", "text-davinci-003");
        //添加我们需要输入的内容
        json.put("prompt", text);
        json.put("temperature", 0.9);
        json.put("max_tokens", 2048);
        json.put("top_p", 1);
        json.put("frequency_penalty", 0.0);
        json.put("presence_penalty", 0.6);
        String respText;
        try {
            HttpResponse response = HttpRequest.post("https://api.openai.com/v1/completions")
                    .headerMap(headers, false)
                    .bearerAuth(openAiToken)
                    .body(String.valueOf(json))
                    .timeout(timeOut)
                    .execute();
            JSONObject jsonObject = JSON.parseObject(response.body());
            Object choices = jsonObject.get("choices");
            JSONArray objects = JSON.parseArray(JSON.toJSONString(choices));
            JSONObject object = JSON.parseObject(JSON.toJSONString(objects.get(0)));
            log.info("回答:[{}]", object.get("text").toString());
            respText = object.get("text").toString();
        } catch (Exception e) {
            respText = "我出现了一些错误,这可能是因为网络不稳定导致的,重新提问一下或许就可以获取到你想要的回答!";
        }
        respMessage(respText, messageId, gptAppId, gptAppSecret);
    }


    private JSONObject getMessageJson(String body) {
        JSONObject requestJson = JSON.parseObject(body);
        JSONObject eventJson = JSON.parseObject(JSON.toJSONString(requestJson.get("event")));
        return JSON.parseObject(JSON.toJSONString(eventJson.get("message")));
    }

    /**
     * 获取消息正文
     *
     * @param messageJson
     * @return
     */
    private String getText(JSONObject messageJson) {
        String text;
        try {
            JSONObject contentJson = JSON.parseObject(JSON.toJSONString(messageJson.get("content")));
            text = contentJson.getString("text");
            String txt = "";
            if (text.startsWith("@")) {
                String[] split = text.split(" ");
                for (int i = 0; i < split.length; i++) {
                    if (i == 0) {
                        continue;
                    }
                    if (i == split.length - 1) {
                        txt = txt.concat(split[i]);
                    } else {
                        txt = txt.concat(split[i] + " ");
                    }
                }
            }
            text = txt;
        } catch (Exception e) {
            text = messageJson.getString("content");
        }
        log.info("请求问题:[{}]", text);
        return text;
    }


    @Async("threadPoolTaskExecutor")
    public void respMessage(String respText, String messageId, String appId, String appSecret) {
        String requestUrl = "https://open.feishu.cn/open-apis/im/v1/messages/" + messageId + "/reply";
        String token = getToken(appId, appSecret);
        JSONObject jsonObject = new JSONObject();
        JSONObject text = new JSONObject();
        text.put("text", respText);
        jsonObject.put("content", text.toJSONString());
        jsonObject.put("msg_type", "text");
        jsonObject.put("uuid", IdUtil.objectId());
        String body = HttpUtil.createPost(requestUrl).auth("Bearer " + token).body(jsonObject.toJSONString()).execute().body();
        log.info("飞书返回信息:[{}]", body);
    }


    private String getToken(String appId, String appSecret) {

        Object tenantAccessToken = LocalCacheUtils.get(appId + "tenant_access_token");
        if (ObjectUtils.isEmpty(tenantAccessToken)) {
            String requestUrl = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
            String post = HttpUtil.post(requestUrl, "{\"app_id\": \"" + appId + "\",\"app_secret\": \"" + appSecret + "\"}");
            JSONObject jsonObject = JSON.parseObject(post);
            if (0 == jsonObject.getInteger("code")) {
                tenantAccessToken = jsonObject.getString("tenant_access_token");
                log.info("获取token为:[{}]", tenantAccessToken);
                LocalCacheUtils.put(appId + "tenant_access_token", tenantAccessToken);
            }
        }
        return tenantAccessToken.toString();
    }
}
