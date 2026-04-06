package com.loyce.omniflow.service.impl.tag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.biz.user.UserContext;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.dao.entity.TagDO;
import com.loyce.omniflow.dao.mapper.TagMapper;
import com.loyce.omniflow.dto.req.TagCreateReqDTO;
import com.loyce.omniflow.dto.req.TagUpdateReqDTO;
import com.loyce.omniflow.dto.resp.TagRespDTO;
import com.loyce.omniflow.service.TagService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MysqlTagServiceImpl extends ServiceImpl<TagMapper, TagDO> implements TagService {

    private static final Set<String> TAG_TYPES = Set.of("ASMR", "FILE_TAB", "COMIC", "GENERAL");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    private static final Pattern TARGET_KEY_PATTERN = Pattern.compile("^[A-Z0-9_-]{1,64}$");
    private static final String DEFAULT_TAG_COLOR = "#4F8CFF";
    private static final String FILE_TAB_TYPE = "FILE_TAB";

    @Override
    public String print() {
        return "MySQL";
    }

    @Override
    public List<TagRespDTO> listTags(String type) {
        Long ownerUserId = getCurrentUserId();
        String normalizedType = normalizeType(type, false);

        LambdaQueryWrapper<TagDO> queryWrapper = Wrappers.lambdaQuery(TagDO.class)
                .and(wrapper -> wrapper.eq(TagDO::getOwnerUserId, ownerUserId).or().isNull(TagDO::getOwnerUserId))
                .eq(normalizedType != null, TagDO::getType, normalizedType)
                .orderByAsc(TagDO::getType, TagDO::getSortOrder, TagDO::getTargetKey, TagDO::getId);
        return list(queryWrapper).stream().map(this::toResp).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagRespDTO createTag(TagCreateReqDTO requestParam) {
        Long ownerUserId = getCurrentUserId();
        String normalizedName = normalizeName(requestParam.getName());
        String normalizedType = normalizeType(requestParam.getType(), true);
        String normalizedTargetKey = normalizeTargetKey(requestParam.getTargetKey(), normalizedType);
        String normalizedColor = normalizeColor(requestParam.getColor(), true);
        String normalizedTextColor = normalizeColor(requestParam.getTextColor(), false);
        Integer sortOrder = normalizeSortOrder(requestParam.getSortOrder());
        Integer enabled = normalizeEnabled(requestParam.getEnabled());
        String description = normalizeDescription(requestParam.getDescription());

        ensureNameUnique(ownerUserId, normalizedType, normalizedName, null);
        ensureTargetKeyUnique(ownerUserId, normalizedType, normalizedTargetKey, null);

        TagDO tag = new TagDO();
        tag.setName(normalizedName);
        tag.setType(normalizedType);
        tag.setTargetKey(normalizedTargetKey);
        tag.setOwnerUserId(ownerUserId);
        tag.setColor(normalizedColor);
        tag.setTextColor(normalizedTextColor);
        tag.setSortOrder(sortOrder);
        tag.setEnabled(enabled);
        tag.setDescription(description);
        try {
            boolean saved = save(tag);
            if (!saved) {
                throw new ClientException("创建标签失败，请稍后重试");
            }
        } catch (DuplicateKeyException ex) {
            throw convertDuplicateKeyException(normalizedType, ex);
        }

        return toResp(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TagRespDTO updateTag(Long tagId, TagUpdateReqDTO requestParam) {
        if (tagId == null || tagId <= 0) {
            throw new ClientException("标签ID非法");
        }

        Long ownerUserId = getCurrentUserId();
        TagDO existing = getOwnerTagOrThrow(tagId, ownerUserId);

        String normalizedName = normalizeName(requestParam.getName());
        String normalizedType = normalizeType(requestParam.getType(), true);
        String normalizedTargetKey = normalizeTargetKey(requestParam.getTargetKey(), normalizedType);
        String normalizedColor = normalizeColor(requestParam.getColor(), true);
        String normalizedTextColor = normalizeColor(requestParam.getTextColor(), false);
        Integer sortOrder = normalizeSortOrder(requestParam.getSortOrder());
        Integer enabled = normalizeEnabled(requestParam.getEnabled());
        String description = normalizeDescription(requestParam.getDescription());

        ensureNameUnique(ownerUserId, normalizedType, normalizedName, tagId);
        ensureTargetKeyUnique(ownerUserId, normalizedType, normalizedTargetKey, tagId);

        LambdaUpdateWrapper<TagDO> updateWrapper = Wrappers.lambdaUpdate(TagDO.class)
                .eq(TagDO::getId, tagId)
                .eq(TagDO::getOwnerUserId, ownerUserId)
                .set(TagDO::getName, normalizedName)
                .set(TagDO::getType, normalizedType)
                .set(TagDO::getTargetKey, normalizedTargetKey)
                .set(TagDO::getColor, normalizedColor)
                .set(TagDO::getTextColor, normalizedTextColor)
                .set(TagDO::getSortOrder, sortOrder)
                .set(TagDO::getEnabled, enabled)
                .set(TagDO::getDescription, description);
        try {
            boolean updated = update(updateWrapper);
            if (!updated) {
                throw new ClientException("标签不存在或无权限操作");
            }
        } catch (DuplicateKeyException ex) {
            throw convertDuplicateKeyException(normalizedType, ex);
        }

        existing.setName(normalizedName);
        existing.setType(normalizedType);
        existing.setTargetKey(normalizedTargetKey);
        existing.setColor(normalizedColor);
        existing.setTextColor(normalizedTextColor);
        existing.setSortOrder(sortOrder);
        existing.setEnabled(enabled);
        existing.setDescription(description);
        return toResp(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        if (tagId == null || tagId <= 0) {
            throw new ClientException("标签ID非法");
        }
        Long ownerUserId = getCurrentUserId();
        TagDO existing = getOwnerTagOrThrow(tagId, ownerUserId);

        // 先释放 FILE_TAB 目标键，避免逻辑删除后仍占用唯一键导致无法重建。
        if (FILE_TAB_TYPE.equals(existing.getType()) && existing.getTargetKey() != null) {
            LambdaUpdateWrapper<TagDO> releaseTargetKeyWrapper = Wrappers.lambdaUpdate(TagDO.class)
                    .eq(TagDO::getId, tagId)
                    .eq(TagDO::getOwnerUserId, ownerUserId)
                    .set(TagDO::getTargetKey, null);
            boolean released = update(releaseTargetKeyWrapper);
            if (!released) {
                throw new ClientException("标签不存在或无权限操作");
            }
        }

        LambdaQueryWrapper<TagDO> queryWrapper = Wrappers.lambdaQuery(TagDO.class)
                .eq(TagDO::getId, tagId)
                .eq(TagDO::getOwnerUserId, ownerUserId);
        boolean removed = remove(queryWrapper);
        if (!removed) {
            throw new ClientException("标签不存在或无权限操作");
        }
    }

    private TagDO getOwnerTagOrThrow(Long tagId, Long ownerUserId) {
        LambdaQueryWrapper<TagDO> queryWrapper = Wrappers.lambdaQuery(TagDO.class)
                .eq(TagDO::getId, tagId)
                .eq(TagDO::getOwnerUserId, ownerUserId);
        TagDO existing = getOne(queryWrapper, false);
        if (existing == null) {
            throw new ClientException("标签不存在或无权限操作");
        }
        return existing;
    }

    private void ensureNameUnique(Long ownerUserId, String type, String name, Long excludeTagId) {
        LambdaQueryWrapper<TagDO> queryWrapper = Wrappers.lambdaQuery(TagDO.class)
                .eq(TagDO::getOwnerUserId, ownerUserId)
                .eq(TagDO::getType, type)
                .eq(TagDO::getName, name);
        if (excludeTagId != null) {
            queryWrapper.ne(TagDO::getId, excludeTagId);
        }
        Long count = count(queryWrapper);
        if (count != null && count > 0) {
            throw new ClientException("同场景下已存在同名标签");
        }
    }

    private void ensureTargetKeyUnique(Long ownerUserId, String type, String targetKey, Long excludeTagId) {
        if (!FILE_TAB_TYPE.equals(type) || targetKey == null) {
            return;
        }
        LambdaQueryWrapper<TagDO> queryWrapper = Wrappers.lambdaQuery(TagDO.class)
                .eq(TagDO::getOwnerUserId, ownerUserId)
                .eq(TagDO::getType, type)
                .eq(TagDO::getTargetKey, targetKey);
        if (excludeTagId != null) {
            queryWrapper.ne(TagDO::getId, excludeTagId);
        }
        Long count = count(queryWrapper);
        if (count != null && count > 0) {
            throw new ClientException("该顶部标签目标已绑定，请先编辑原有配置");
        }
    }

    private ClientException convertDuplicateKeyException(String type, DuplicateKeyException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage();
        if (message.contains("uniq_tags_owner_type_target_active")
                || message.contains("uniq_tags_owner_type_target")) {
            if (FILE_TAB_TYPE.equals(type)) {
                return new ClientException("该顶部标签目标已绑定，请先编辑原有配置");
            }
            return new ClientException("存在重复标签，请检查名称与绑定目标");
        }
        return new ClientException("存在重复数据，请检查后重试");
    }

    private Long getCurrentUserId() {
        String userId = UserContext.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new ClientException("未获取到登录用户信息");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ClientException("用户ID格式非法");
        }
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new ClientException("标签名称不能为空");
        }
        if (normalized.length() > 64) {
            throw new ClientException("标签名称长度不能超过64");
        }
        return normalized;
    }

    private String normalizeType(String type, boolean required) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            if (!required) {
                return null;
            }
            normalized = "GENERAL";
        }
        if (!TAG_TYPES.contains(normalized)) {
            throw new ClientException("不支持的标签类型: " + normalized);
        }
        return normalized;
    }

    private String normalizeTargetKey(String targetKey, String normalizedType) {
        String normalized = targetKey == null ? "" : targetKey.trim().toUpperCase(Locale.ROOT);
        if (!FILE_TAB_TYPE.equals(normalizedType)) {
            return null;
        }
        if (normalized.isEmpty()) {
            throw new ClientException("FILE_TAB 类型必须指定目标键");
        }
        if (!TARGET_KEY_PATTERN.matcher(normalized).matches()) {
            throw new ClientException("目标键格式非法，仅支持 A-Z、0-9、_、-，长度 1-64");
        }
        return normalized;
    }

    private String normalizeColor(String color, boolean fallbackDefault) {
        String normalized = color == null ? "" : color.trim();
        if (normalized.isEmpty()) {
            return fallbackDefault ? DEFAULT_TAG_COLOR : null;
        }
        if (!HEX_COLOR_PATTERN.matcher(normalized).matches()) {
            throw new ClientException("颜色格式非法，必须为 HEX（#RRGGBB / #RRGGBBAA）");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private Integer normalizeSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return 0;
        }
        return sortOrder;
    }

    private Integer normalizeEnabled(Integer enabled) {
        if (enabled == null) {
            return 1;
        }
        if (enabled != 0 && enabled != 1) {
            throw new ClientException("enabled 仅支持 0 或 1");
        }
        return enabled;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 255) {
            throw new ClientException("描述长度不能超过255");
        }
        return normalized;
    }

    private TagRespDTO toResp(TagDO tag) {
        TagRespDTO dto = new TagRespDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setType(tag.getType());
        dto.setTargetKey(tag.getTargetKey());
        dto.setOwnerUserId(tag.getOwnerUserId());
        dto.setColor(tag.getColor());
        dto.setTextColor(tag.getTextColor());
        dto.setSortOrder(tag.getSortOrder());
        dto.setEnabled(tag.getEnabled());
        dto.setDescription(tag.getDescription());
        dto.setCreatedAt(tag.getCreatedAt());
        dto.setUpdatedAt(tag.getUpdatedAt());
        return dto;
    }
}
