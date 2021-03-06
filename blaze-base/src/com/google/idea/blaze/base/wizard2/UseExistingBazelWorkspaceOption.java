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
package com.google.idea.blaze.base.wizard2;

import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.settings.Blaze.BuildSystem;
import com.intellij.openapi.vfs.VirtualFile;
import icons.BlazeIcons;

import javax.swing.*;
import java.io.File;

class UseExistingBazelWorkspaceOption extends UseExistingWorkspaceOption {

  UseExistingBazelWorkspaceOption(BlazeNewProjectBuilder builder) {
    super(builder, BuildSystem.Bazel);
  }

  @Override
  protected boolean isWorkspaceRoot(VirtualFile file) {
    return BuildSystemProvider.getWorkspaceRootProvider(BuildSystem.Bazel).isWorkspaceRoot(new File(file.getPath()));
  }

  @Override
  public String getOptionName() {
    return "use-existing-bazel-workspace";
  }
  @Override
  public String getOptionText() {
    return "Use existing bazel workspace";
  }

  @Override
  protected String getWorkspaceName(File workspaceRoot) {
    return workspaceRoot.getName();
  }

  @Override
  protected String fileChooserDescription() {
    return "Select the directory of the workspace you want to use.";
  }

  @Override
  protected Icon getBuildSystemIcon() {
    return BlazeIcons.BazelLeaf;
  }
}
