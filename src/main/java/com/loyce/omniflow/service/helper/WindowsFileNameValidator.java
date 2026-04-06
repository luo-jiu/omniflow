package com.loyce.omniflow.service.helper;

import com.loyce.omniflow.common.convention.exception.ClientException;
import org.springframework.stereotype.Component;

@Component
public class WindowsFileNameValidator {

    public String normalizeName(String rawName) {
        if (rawName == null) {
            throw new ClientException("名称不能为空");
        }
        String name = rawName.trim();
        if (name.isEmpty()) {
            throw new ClientException("名称不能为空");
        }
        return name;
    }

    public String normalizeExt(String rawExt) {
        if (rawExt == null) {
            return "";
        }
        String ext = rawExt.trim().replaceFirst("^\\.+", "");
        if (ext.contains(".")) {
            throw new ClientException("扩展名格式非法");
        }
        return ext;
    }

    public void validate(String name, String ext) {
        normalizeName(name);
        normalizeExt(ext);
    }
}
