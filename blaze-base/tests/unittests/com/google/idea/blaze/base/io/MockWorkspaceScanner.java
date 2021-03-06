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
package com.google.idea.blaze.base.io;

import com.google.common.collect.Sets;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.settings.Blaze.BuildSystem;
import com.google.idea.blaze.base.sync.projectview.ImportRoots;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Mocks the file system.
 */
public final class MockWorkspaceScanner implements WorkspaceScanner {

  Set<WorkspacePath> files = Sets.newHashSet();
  Set<WorkspacePath> directories = Sets.newHashSet();

  public MockWorkspaceScanner addFile(@NotNull WorkspacePath file) {
    files.add(file);
    return this;
  }

  public MockWorkspaceScanner addDirectory(@NotNull WorkspacePath file) {
    addFile(file);
    directories.add(file);
    return this;
  }

  public MockWorkspaceScanner addPackage(@NotNull WorkspacePath file) {
    addFile(new WorkspacePath(file + "/BUILD"));
    addDirectory(file);
    return this;
  }

  public MockWorkspaceScanner addPackages(@NotNull Iterable<WorkspacePath> files) {
    for (WorkspacePath workspacePath : files) {
      addPackage(workspacePath);
    }
    return this;
  }

  public MockWorkspaceScanner addImportRoots(@NotNull ImportRoots importRoots) {
    addPackages(importRoots.rootDirectories());
    addPackages(importRoots.excludeDirectories());
    return this;
  }

  public MockWorkspaceScanner addProjectView(WorkspaceRoot workspaceRoot, ProjectViewSet projectViewSet) {
    ImportRoots importRoots = ImportRoots.builder(workspaceRoot, BuildSystem.Blaze).add(projectViewSet).build();
    return addImportRoots(importRoots);
  }

  @Override
  public boolean exists(WorkspaceRoot workspaceRoot, WorkspacePath file) {
    return files.contains(file);
  }
}
