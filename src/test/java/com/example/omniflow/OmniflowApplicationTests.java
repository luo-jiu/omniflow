package com.example.omniflow;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class OmniflowApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
        /**
         * Hash
         * Key: login_用户名
         * Value:
         *   key: token 标识
         *   value: JSON 字符串(用户信息)
         */
        String userDO = "{这是一个对象}";

        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(uuid, JSON.toJSONString(userDO), 30L, TimeUnit.DAYS);

        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("token", JSON.toJSONString(userDO));

        stringRedisTemplate.opsForHash().put("login_" + "LJ", uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire("login_" + "LJ", 30L, TimeUnit.DAYS);
    }

}
