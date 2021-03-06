/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.PlatformUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * Test utilities specific to running in a blaze/bazel environment.
 */
public class BlazeTestSystemProperties {

  /**
   * The absolute path to the runfiles directory.
   */
  private static final String RUNFILES_PATH = getUserValue("TEST_SRCDIR");

  public static boolean isRunThroughBlaze() {
    return System.getenv("JAVA_RUNFILES") != null;
  }

  /**
   * Sets up the necessary system properties for running IntelliJ tests via blaze/bazel.
   */
  public static void configureSystemProperties() throws IOException {
    if (!isRunThroughBlaze()) {
      return;
    }
    File sandbox = new File(getTmpDirFile(), "_intellij_test_sandbox");

    setSandboxPath("idea.home.path", new File(sandbox, "home"));
    setSandboxPath("idea.config.path", new File(sandbox, "config"));
    setSandboxPath("idea.system.path", new File(sandbox, "system"));
    setIfEmpty(PlatformUtils.PLATFORM_PREFIX_KEY, "Idea");
    setIfEmpty("idea.classpath.index.enabled", "false");

    VfsRootAccess.allowRootAccess(RUNFILES_PATH);

    List<String> pluginJars = Lists.newArrayList();
    try {
      Enumeration<URL> urls = BlazeTestSystemProperties.class.getClassLoader().getResources("META-INF/plugin.xml");
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        addArchiveFile(url, pluginJars);
      }
    } catch (IOException e) {
      System.err.println("Cannot find plugin.xml resources");
      e.printStackTrace();
    }

    setIfEmpty("idea.plugins.path", Joiner.on(File.pathSeparator).join(pluginJars));
  }

  private static void addArchiveFile(URL url, List<String> files) {
    if ("jar".equals(url.getProtocol())) {
      String path = url.getPath();
      int index = path.indexOf("!/");
      if (index > 0) {
        String jarPath = path.substring(0, index);
        if (jarPath.startsWith("file:")) {
          files.add(jarPath.substring(5));
        }
      }
    }
  }

  private static void setSandboxPath(String property, File path) {
    path.mkdirs();
    setIfEmpty(property, path.getPath());
  }

  private static void setIfEmpty(String property, String value) {
    if (System.getProperty(property) == null) {
      System.setProperty(property, value);
    }
  }

  /**
   * Gets directory that should be used for all files created during testing.
   *
   * <p>This method will return a directory that's common to all tests run
   * within the same <i>build target</i>.
   *
   * @return standard file, for example the File representing "/tmp/zogjones/foo_unittest/".
   */
  private static File getTmpDirFile() {
    File tmpDir;

    // Flag value specified in environment?
    String tmpDirStr = getUserValue("TEST_TMPDIR");
    if ((tmpDirStr != null) && (tmpDirStr.length() > 0)) {
      tmpDir = new File(tmpDirStr);
    } else {
      // Fallback default $TEMP/$USER/tmp/$TESTNAME
      String baseTmpDir = System.getProperty("java.io.tmpdir");
      tmpDir = new File(baseTmpDir).getAbsoluteFile();

      // .. Add username
      String username = System.getProperty("user.name");
      username = username.replace('/', '_');
      username = username.replace('\\', '_');
      username = username.replace('\000', '_');
      tmpDir = new File(tmpDir, username);
      tmpDir = new File(tmpDir, "tmp");
    }

    // Ensure tmpDir exists
    if (!tmpDir.isDirectory()) {
      tmpDir.mkdirs();
    }
    return tmpDir;
  }

  /**
   * Returns the value for system property <code>name</code>, or if that is
   * not found the value of the user's environment variable <code>name</code>.
   * If neither is found, null is returned.
   *
   * @param name the name of property to get
   * @return the value of the property or null if it is not found
   */
  private static String getUserValue(String name) {
    String propValue = System.getProperty(name);
    if (propValue == null) {
      return System.getenv(name);
    }
    return propValue;
  }

}
