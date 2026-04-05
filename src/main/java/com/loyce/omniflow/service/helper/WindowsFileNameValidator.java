package com.loyce.omniflow.service.helper;

import com.loyce.omniflow.common.convention.exception.ClientException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class WindowsFileNameValidator {

    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[<>:\"/\\\\|?*\\x00-\\x1F]");
    private static final Set<String> RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

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
        String normalizedName = normalizeName(name);
        String normalizedExt = normalizeExt(ext);
        String fullName = StringUtils.hasText(normalizedExt)
                ? normalizedName + "." + normalizedExt
                : normalizedName;

        if (".".equals(fullName) || "..".equals(fullName)) {
            throw new ClientException("名称不能为 . 或 ..");
        }

        if (INVALID_CHAR_PATTERN.matcher(fullName).find()) {
            throw new ClientException("名称包含非法字符：< > : \" / \\ | ? *");
        }

        if (fullName.endsWith(" ") || fullName.endsWith(".")) {
            throw new ClientException("名称不能以空格或点结尾");
        }

        String[] segments = fullName.split("\\.", 2);
        String deviceNameCandidate = segments[0].toUpperCase();
        if (RESERVED_NAMES.contains(deviceNameCandidate)) {
            throw new ClientException("名称不能使用系统保留名（CON/PRN/AUX/NUL/COM1..9/LPT1..9）");
        }
    }
}
