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
package com.google.idea.blaze.android.run.test;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.run.*;
import com.android.tools.idea.run.editor.AndroidDebugger;
import com.android.tools.idea.run.editor.AndroidDebuggerState;
import com.android.tools.idea.run.tasks.DebugConnectorTask;
import com.android.tools.idea.run.tasks.DeployApkTask;
import com.android.tools.idea.run.tasks.LaunchTask;
import com.android.tools.idea.run.tasks.LaunchTasksProvider;
import com.android.tools.idea.run.util.ProcessHandlerLaunchStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.idea.blaze.android.run.BlazeAndroidRunConfigurationCommonState;
import com.google.idea.blaze.android.run.deployinfo.BlazeApkProvider;
import com.google.idea.blaze.android.run.runner.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Run context for android_test.
 */
class BlazeAndroidTestRunContext implements BlazeAndroidRunContext {
  private final Project project;
  private final AndroidFacet facet;
  private final RunConfiguration runConfiguration;
  private final ExecutionEnvironment env;
  private final BlazeAndroidRunConfigurationCommonState commonState;
  private final BlazeAndroidTestRunConfigurationState configState;
  private final ImmutableList<String> buildFlags;
  private final List<Runnable> launchTaskCompleteListeners = Lists.newArrayList();
  private final ConsoleProvider consoleProvider;
  private final BlazeApkBuildStepNormalBuild buildStep;
  private final ApplicationIdProvider applicationIdProvider;
  private final ApkProvider apkProvider;

  public BlazeAndroidTestRunContext(Project project,
                                    AndroidFacet facet,
                                    RunConfiguration runConfiguration,
                                    ExecutionEnvironment env,
                                    BlazeAndroidRunConfigurationCommonState commonState,
                                    BlazeAndroidTestRunConfigurationState configState,
                                    ImmutableList<String> buildFlags) {
    this.project = project;
    this.facet = facet;
    this.runConfiguration = runConfiguration;
    this.env = env;
    this.commonState = commonState;
    this.configState = configState;
    this.buildFlags = buildFlags;
    this.consoleProvider = new AndroidTestConsoleProvider(project, runConfiguration, configState);
    this.buildStep = new BlazeApkBuildStepNormalBuild(project, commonState, buildFlags);
    this.applicationIdProvider = new BlazeAndroidTestApplicationIdProvider(project, buildStep.getDeployInfo());
    this.apkProvider = new BlazeApkProvider(project, buildStep.getDeployInfo());
  }

  @Override
  public void augmentEnvironment(ExecutionEnvironment env) {
  }

  @Override
  public BlazeAndroidDeviceSelector getDeviceSelector() {
    return new BlazeAndroidDeviceSelector.NormalDeviceSelector();
  }

  @Override
  public void augmentLaunchOptions(LaunchOptions.Builder options) {
    options.setDeploy(!configState.isRunThroughBlaze());
  }

  @Override
  public ConsoleProvider getConsoleProvider() {
    return consoleProvider;
  }

  @Override
  public ApplicationIdProvider getApplicationIdProvider() throws ExecutionException {
    return applicationIdProvider;
  }

  @Nullable
  @Override
  public BlazeApkBuildStep getBuildStep() {
    return buildStep;
  }

  @Override
  public LaunchTasksProvider getLaunchTasksProvider(
    LaunchOptions launchOptions,
    BlazeAndroidRunConfigurationDebuggerManager debuggerManager) throws ExecutionException {
    return new BlazeAndroidLaunchTasksProvider(project, this, applicationIdProvider, launchOptions, debuggerManager);
  }

  @Override
  public ImmutableList<LaunchTask> getDeployTasks(IDevice device, LaunchOptions launchOptions) throws ExecutionException {
    Collection<ApkInfo> apks;
    try {
      apks = apkProvider.getApks(device);
    }
    catch (ApkProvisionException e) {
      throw new ExecutionException(e);
    }
    return ImmutableList.of(new DeployApkTask(project, launchOptions, apks));
  }

  @Nullable
  @Override
  public LaunchTask getApplicationLaunchTask(LaunchOptions launchOptions,
                                             AndroidDebugger androidDebugger,
                                             AndroidDebuggerState androidDebuggerState,
                                             ProcessHandlerLaunchStatus processHandlerLaunchStatus) throws ExecutionException {
    if (configState.isRunThroughBlaze()) {
      return new BlazeAndroidTestLaunchTask(
        project,
        commonState.getTarget(),
        buildFlags,
        new BlazeAndroidTestFilter(configState.TESTING_TYPE,
                                   configState.CLASS_NAME,
                                   configState.METHOD_NAME,
                                   configState.PACKAGE_NAME),
        this,
        launchOptions.isDebug()
      );
    }
    return StockAndroidTestLaunchTask.getStockTestLaunchTask(
      configState,
      applicationIdProvider,
      launchOptions.isDebug(),
      facet,
      processHandlerLaunchStatus
    );
  }

  @Override
  public DebugConnectorTask getDebuggerTask(LaunchOptions launchOptions,
                                            AndroidDebugger androidDebugger,
                                            AndroidDebuggerState androidDebuggerState,
                                            @NotNull Set<String> packageIds) throws ExecutionException {
    if (configState.isRunThroughBlaze()) {
      return new ConnectBlazeTestDebuggerTask(env.getProject(), androidDebugger, packageIds, applicationIdProvider, this);
    }
    //noinspection unchecked
    return androidDebugger.getConnectDebuggerTask(env, null, packageIds, facet, androidDebuggerState, runConfiguration.getType().getId());
  }

  void onLaunchTaskComplete() {
    for (Runnable runnable : launchTaskCompleteListeners) {
      runnable.run();
    }
  }

  void addLaunchTaskCompleteListener(Runnable runnable) {
    launchTaskCompleteListeners.add(runnable);
  }
}
