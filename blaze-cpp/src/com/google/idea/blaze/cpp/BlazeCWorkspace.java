/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.idea.blaze.cpp;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.metrics.Action;
import com.google.idea.blaze.base.metrics.LoggingService;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.settings.Blaze;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.cidr.lang.symbols.OCSymbol;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.lang.workspace.OCWorkspace;
import com.jetbrains.cidr.lang.workspace.OCWorkspaceModificationTrackers;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Main entry point for C/CPP configuration data.
 */
public final class BlazeCWorkspace implements OCWorkspace {
  private static final Logger LOG = Logger.getInstance(BlazeCWorkspace.class);

  @Nullable private final Project project;
  @Nullable private final OCWorkspaceModificationTrackers modTrackers;

  @Nullable private BlazeConfigurationResolver configurationResolver;

  private BlazeCWorkspace(Project project) {
    if (Blaze.isBlazeProject(project)) {
      this.project = project;
      this.modTrackers = new OCWorkspaceModificationTrackers(project);
      this.configurationResolver = new BlazeConfigurationResolver(project);
    } else {
      this.project = null;
      this.modTrackers = null;
    }
  }

  public static BlazeCWorkspace getInstance(Project project) {
    return ServiceManager.getService(project, BlazeCWorkspace.class);
  }

  public void update(BlazeContext context, BlazeProjectData blazeProjectData) {
    LOG.assertTrue(project != null);
    LOG.assertTrue(modTrackers != null);
    LOG.assertTrue(configurationResolver != null);

    long start = System.currentTimeMillis();
    // Non-incremental update to our c configurations.
    configurationResolver.update(context, blazeProjectData);
    long end = System.currentTimeMillis();

    LOG.info(String.format("Blaze OCWorkspace update took: %d ms", (end - start)));

    ApplicationManager.getApplication().runReadAction(() -> {
      if (project.isDisposed()) {
        return;
      }

      // TODO(salguarnieri) Avoid bumping all of these trackers; figure out what has changed.
      modTrackers.getProjectFilesListTracker().incModificationCount();
      modTrackers.getSourceFilesListTracker().incModificationCount();
      modTrackers.getBuildConfigurationChangesTracker().incModificationCount();
      modTrackers.getBuildSettingsChangesTracker().incModificationCount();
    });
  }

  @Override
  public Collection<VirtualFile> getLibraryFilesToBuildSymbols() {
  // This method should return all the header files themselves, not the head file directories.
  // (And not header files in the project; just the ones in the SDK and in any dependencies)
    return ImmutableList.of();
  }

  @Override
  public boolean areFromSameProject(@Nullable VirtualFile a, @Nullable VirtualFile b) {
    return false;
  }

  @Override
  public boolean areFromSamePackage(@Nullable VirtualFile a, @Nullable VirtualFile b) {
    return false;
  }

  @Override
  public boolean isInSDK(@Nullable VirtualFile file) {
    return false;
  }

  @Override
  public boolean isFromWrongSDK(OCSymbol symbol, @Nullable VirtualFile contextFile) {
    return false;
  }

  @Nullable
  @Override
  public OCResolveConfiguration getSelectedResolveConfiguration() {
    return null;
  }

  @Override
  public OCWorkspaceModificationTrackers getModificationTrackers() {
    LOG.assertTrue(modTrackers != null);
    return modTrackers;
  }

  @Override
  public List<? extends OCResolveConfiguration> getConfigurations() {
    return configurationResolver == null ? ImmutableList.of() : configurationResolver.getAllConfigurations();
  }

  @Override
  public List<? extends OCResolveConfiguration> getConfigurationsForFile(@Nullable VirtualFile sourceFile) {
    LoggingService.reportEvent(project, Action.C_RESOLVE_FILE);

    if (sourceFile == null || !sourceFile.isValid() || configurationResolver == null) {
      return ImmutableList.of();
    }
    OCResolveConfiguration config = configurationResolver.getConfigurationForFile(sourceFile);
    return config == null ? ImmutableList.of() : ImmutableList.of(config);
  }
}

