package com.google.idea.blaze.base.util;

import com.google.common.base.Strings;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SafeFileUtil {
    public static boolean isAncestor(@NotNull File ancestor, @NotNull File file, boolean strict) {
        return isAncestor(ancestor.getPath(), file.getPath(), strict);
    }

    public static boolean isAncestor(@NotNull String ancestor, @NotNull String file, boolean strict) {
        return FileUtil.isAncestor(normalize(ancestor), normalize(file), strict);
    }

    public static String normalize(String path) {
        return path.replace('\\', '/');
    }

    public static String join(String first, String second) {
        if (Strings.isNullOrEmpty(first)) {
            return second;
        } else if (Strings.isNullOrEmpty(second)) {
            return first;
        }
        return first + "/" + second;
    }
}
