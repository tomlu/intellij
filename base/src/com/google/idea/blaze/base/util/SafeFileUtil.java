package com.google.idea.blaze.base.util;

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
}
