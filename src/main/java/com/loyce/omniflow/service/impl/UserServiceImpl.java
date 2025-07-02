package com.loyce.omniflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.common.enums.RedisPrefixCodeEnum;
import com.loyce.omniflow.common.enums.UserErrorCodeEnum;
import com.loyce.omniflow.dao.entity.UserDO;
import com.loyce.omniflow.dao.mapper.UserMapper;
import com.loyce.omniflow.dto.req.UserLoginReqDTO;
import com.loyce.omniflow.dto.req.UserRegisterReqDTO;
import com.loyce.omniflow.dto.req.UserUpdateReqDTO;
import com.loyce.omniflow.dto.resp.UserLoginRespDTO;
import com.loyce.omniflow.dto.resp.UserRespDTO;
import com.loyce.omniflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public Boolean hasUsername(String username) {
        // 从Bloom过滤器里面去拿取
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if (!hasUsername(requestParam.getUsername())) {
            // 用户名存在
            throw new ClientException(UserErrorCodeEnum.USER_NAME_EXIST);
        }
        // 密码加密
        requestParam.setPassword(BCrypt.hashpw(requestParam.getPassword(), BCrypt.gensalt(10)));
        try {
            int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, UserDO.class));
            if (inserted < 1) {
                // 用户新增失败
                throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
            }
        } catch (DuplicateKeyException ex) {
            throw new ClientException(UserErrorCodeEnum.USER_EXIST);
        }
        // 添加到Bloom过滤器
        userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // TODO 验证当前用户是否为登录用户
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam, UserDO.class), updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getStatus, 1);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户不存在");
        }
        if (!BCrypt.checkpw(requestParam.getPassword(), userDO.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        Boolean hasLogin = stringRedisTemplate.hasKey("login:" + requestParam.getUsername());
        if (Boolean.TRUE.equals(hasLogin)) {
            throw new ClientException("用户已登录");
        }
        /**
         * Hash
         * Key: login:用户名
         * Value:
         *   key: token 标识
         *   value: JSON 字符串(用户信息)
         */
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> userInfoMap = new HashMap<>();
        userInfoMap.put("token", JSON.toJSONString(userDO));

        stringRedisTemplate.opsForHash().put(RedisPrefixCodeEnum.USER_LOGIN_CODING + requestParam.getUsername(), uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(RedisPrefixCodeEnum.USER_LOGIN_CODING + requestParam.getUsername(), 30L, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        stringRedisTemplate.opsForValue().get(token);
        return true;
    }

    @Override
    public void logout(String username, String token) {
        // 先验证是否登录
        if (checkLogin(username, token)) {
            stringRedisTemplate.delete("login_" + username);
            return;
        }
        throw new ClientException("用户Token 不存在或用户未登录");
    }
}
