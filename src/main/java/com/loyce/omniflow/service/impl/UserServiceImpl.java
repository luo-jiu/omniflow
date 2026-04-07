package com.loyce.omniflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.biz.user.UserContext;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.common.enums.RedisPrefixCodeEnum;
import com.loyce.omniflow.common.enums.UserErrorCodeEnum;
import com.loyce.omniflow.dao.entity.UserDO;
import com.loyce.omniflow.dao.mapper.UserMapper;
import com.loyce.omniflow.dto.req.UserLoginReqDTO;
import com.loyce.omniflow.dto.req.UserPasswordUpdateReqDTO;
import com.loyce.omniflow.dto.req.UserRegisterReqDTO;
import com.loyce.omniflow.dto.req.UserUpdateReqDTO;
import com.loyce.omniflow.dto.resp.UserLoginRespDTO;
import com.loyce.omniflow.dto.resp.UserLoginUserInfoRespDTO;
import com.loyce.omniflow.dto.resp.UserRespDTO;
import com.loyce.omniflow.service.UserService;
import com.loyce.omniflow.util.MinioUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.mindrot.jbcrypt.BCrypt;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private static final String EXT_AVATAR_KEY = "avatarKey";
    private static final String EXT_AVATAR_URL_LEGACY = "avatar";
    private static final int AVATAR_URL_EXPIRE_MINUTES = 24 * 60;
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "avif", "heic", "heif"
    );

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    private final StringRedisTemplate stringRedisTemplate;
    private final MinioUtils minioUtils;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)
                .isNull(UserDO::getDeletedAt);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        return toUserResp(userDO);
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
        if (StrUtil.isBlank(requestParam.getNickname())) {
            requestParam.setNickname(requestParam.getUsername());
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
        if (requestParam.getId() == null) {
            throw new ClientException("用户ID不能为空");
        }
        assertCurrentUserPermission(requestParam.getId());

        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, requestParam.getId())
                .isNull(UserDO::getDeletedAt);

        boolean hasChanges = false;

        if (requestParam.getNickname() != null) {
            String nickname = requestParam.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new ClientException("昵称不能为空");
            }
            updateWrapper.set(UserDO::getNickname, nickname);
            hasChanges = true;
        }

        if (requestParam.getPhone() != null) {
            updateWrapper.set(UserDO::getPhone, normalizeNullableText(requestParam.getPhone()));
            hasChanges = true;
        }

        if (requestParam.getEmail() != null) {
            updateWrapper.set(UserDO::getEmail, normalizeNullableText(requestParam.getEmail()));
            hasChanges = true;
        }

        if (requestParam.getExt() != null) {
            updateWrapper.set(UserDO::getExt, requestParam.getExt());
            hasChanges = true;
        }

        if (!hasChanges) {
            return;
        }

        int updated = baseMapper.update(null, updateWrapper);
        if (updated < 1) {
            throw new ClientException("用户信息更新失败");
        }
    }

    @Override
    public void updatePassword(Long userId, UserPasswordUpdateReqDTO requestParam) {
        if (userId == null) {
            throw new ClientException("用户ID不能为空");
        }
        assertCurrentUserPermission(userId);
        if (requestParam == null) {
            throw new ClientException("参数不能为空");
        }

        String oldPassword = requestParam.getOldPassword() == null ? "" : requestParam.getOldPassword();
        String newPassword = requestParam.getNewPassword() == null ? "" : requestParam.getNewPassword();
        if (oldPassword.trim().isEmpty()) {
            throw new ClientException("请输入旧密码");
        }
        if (newPassword.trim().isEmpty()) {
            throw new ClientException("请输入新密码");
        }

        UserDO currentUser = baseMapper.selectOne(
                Wrappers.lambdaQuery(UserDO.class)
                        .eq(UserDO::getId, userId)
                        .isNull(UserDO::getDeletedAt)
                        .last("LIMIT 1")
        );
        if (currentUser == null) {
            throw new ClientException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, currentUser.getPassword())) {
            throw new ClientException("旧密码错误");
        }
        if (BCrypt.checkpw(newPassword, currentUser.getPassword())) {
            throw new ClientException("新密码不能与旧密码相同");
        }

        int updated = baseMapper.update(
                null,
                Wrappers.lambdaUpdate(UserDO.class)
                        .eq(UserDO::getId, userId)
                        .isNull(UserDO::getDeletedAt)
                        .set(UserDO::getPassword, BCrypt.hashpw(newPassword, BCrypt.gensalt(10)))
        );
        if (updated < 1) {
            throw new ClientException("密码更新失败");
        }
    }

    @Override
    public UserRespDTO uploadAvatar(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new ClientException("用户ID不能为空");
        }
        assertCurrentUserPermission(userId);
        if (file == null || file.isEmpty()) {
            throw new ClientException("请选择头像文件");
        }
        String contentType = StrUtil.blankToDefault(file.getContentType(), "application/octet-stream");
        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "");
        String ext = FilenameUtils.getExtension(originalFilename).toLowerCase();
        String detectedMimeType;
        try {
            detectedMimeType = new Tika().detect(file.getInputStream(), originalFilename);
        } catch (Exception ignored) {
            detectedMimeType = contentType;
        }
        boolean imageMime = StrUtil.isNotBlank(detectedMimeType) && detectedMimeType.startsWith("image/");
        boolean imageExt = StrUtil.isNotBlank(ext) && ALLOWED_AVATAR_EXTENSIONS.contains(ext);
        if (!imageMime && !imageExt) {
            throw new ClientException("头像仅支持图片文件（jpg/png/webp 等）");
        }
        String uploadContentType = resolveAvatarContentType(ext, detectedMimeType, contentType);

        UserDO currentUser = baseMapper.selectById(userId);
        if (currentUser == null || currentUser.getDeletedAt() != null) {
            throw new ClientException("用户不存在");
        }

        String newAvatarKey = buildAvatarStorageKey(userId, originalFilename);
        JSONObject extJson = parseExt(currentUser.getExt());
        String oldAvatarKey = extJson.getString(EXT_AVATAR_KEY);

        try {
            minioUtils.uploadFile(newAvatarKey, file.getInputStream(), file.getSize(), uploadContentType);
            extJson.put(EXT_AVATAR_KEY, newAvatarKey);
            extJson.remove(EXT_AVATAR_URL_LEGACY);

            int updated = baseMapper.update(
                    null,
                    Wrappers.lambdaUpdate(UserDO.class)
                            .eq(UserDO::getId, userId)
                            .isNull(UserDO::getDeletedAt)
                            .set(UserDO::getExt, extJson.toJSONString())
            );
            if (updated < 1) {
                try {
                    minioUtils.deleteFile(newAvatarKey);
                } catch (Exception ignored) {
                    // ignore cleanup failure
                }
                throw new ClientException("头像更新失败");
            }

            // 清理旧头像对象（忽略删除异常，避免影响主流程）
            if (StrUtil.isNotBlank(oldAvatarKey) && !oldAvatarKey.equals(newAvatarKey)) {
                try {
                    minioUtils.deleteFile(oldAvatarKey);
                } catch (Exception ignored) {
                    // ignore cleanup failure
                }
            }

            UserDO refreshed = baseMapper.selectById(userId);
            if (refreshed == null) {
                throw new ClientException("头像更新失败");
            }
            return toUserResp(refreshed);
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("头像上传失败");
        }
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        String loginKey = RedisPrefixCodeEnum.USER_LOGIN_CODING + requestParam.getUsername();
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
        Boolean hasLogin = stringRedisTemplate.hasKey(loginKey);
        if (Boolean.TRUE.equals(hasLogin)) {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(loginKey);
            if (!entries.isEmpty()) {
                String existToken = entries.keySet().iterator().next().toString();
                return new UserLoginRespDTO(existToken, buildLoginUserInfo(userDO));
            }
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
        stringRedisTemplate.opsForHash().put(loginKey, uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire(RedisPrefixCodeEnum.USER_LOGIN_CODING + requestParam.getUsername(), 30L, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid, buildLoginUserInfo(userDO));
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(token)) {
            return false;
        }
        String loginKey = RedisPrefixCodeEnum.USER_LOGIN_CODING + username;
        Object loginInfo = stringRedisTemplate.opsForHash().get(loginKey, token);
        return loginInfo != null;
    }

    @Override
    public void logout(String username, String token) {
        if (!checkLogin(username, token)) {
            throw new ClientException("用户Token 不存在或用户未登录");
        }
        String loginKey = RedisPrefixCodeEnum.USER_LOGIN_CODING + username;
        Long removed = stringRedisTemplate.opsForHash().delete(loginKey, token);
        if (removed == null || removed < 1) {
            throw new ClientException("用户Token 不存在或用户未登录");
        }
        Long remainCount = stringRedisTemplate.opsForHash().size(loginKey);
        if (remainCount != null && remainCount == 0) {
            stringRedisTemplate.delete(loginKey);
        }
    }

    private UserLoginUserInfoRespDTO buildLoginUserInfo(UserDO userDO) {
        UserLoginUserInfoRespDTO userInfo = new UserLoginUserInfoRespDTO();
        userInfo.setId(userDO.getId());
        userInfo.setUsername(userDO.getUsername());
        userInfo.setNickname(StrUtil.blankToDefault(userDO.getNickname(), userDO.getUsername()));
        userInfo.setExt(userDO.getExt());
        userInfo.setAvatar(resolveAvatarUrl(userDO.getExt()));
        return userInfo;
    }

    private UserRespDTO toUserResp(UserDO userDO) {
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        result.setNickname(StrUtil.blankToDefault(userDO.getNickname(), userDO.getUsername()));
        result.setAvatar(resolveAvatarUrl(userDO.getExt()));
        return result;
    }

    private JSONObject parseExt(String ext) {
        if (StrUtil.isBlank(ext)) {
            return new JSONObject();
        }
        try {
            JSONObject jsonObject = JSON.parseObject(ext);
            return jsonObject == null ? new JSONObject() : jsonObject;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private String resolveAvatarUrl(String ext) {
        JSONObject extJson = parseExt(ext);
        String avatarKey = extJson.getString(EXT_AVATAR_KEY);
        if (StrUtil.isNotBlank(avatarKey)) {
            try {
                return minioUtils.getPreignedUrl(avatarKey, AVATAR_URL_EXPIRE_MINUTES);
            } catch (Exception ignored) {
                // fallback to legacy avatar field
            }
        }
        String legacyAvatarUrl = extJson.getString(EXT_AVATAR_URL_LEGACY);
        if (StrUtil.isBlank(legacyAvatarUrl)) {
            return null;
        }
        return legacyAvatarUrl;
    }

    private String buildAvatarStorageKey(Long userId, String originalFilename) {
        String ext = FilenameUtils.getExtension(StrUtil.blankToDefault(originalFilename, ""));
        String normalizedExt = StrUtil.blankToDefault(ext, "bin").toLowerCase();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("user/%d/avatar/%s/%s.%s", userId, datePath, uuid, normalizedExt);
    }

    private void assertCurrentUserPermission(Long targetUserId) {
        String currentUserId = UserContext.getUserId();
        if (StrUtil.isBlank(currentUserId) || !String.valueOf(targetUserId).equals(currentUserId)) {
            throw new ClientException("无权限修改当前用户");
        }
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveAvatarContentType(String ext, String detectedMimeType, String fallbackContentType) {
        if (StrUtil.isNotBlank(detectedMimeType) && detectedMimeType.startsWith("image/")) {
            return detectedMimeType;
        }
        return switch (StrUtil.blankToDefault(ext, "").toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "avif" -> "image/avif";
            case "heic" -> "image/heic";
            case "heif" -> "image/heif";
            default -> fallbackContentType;
        };
    }

}
