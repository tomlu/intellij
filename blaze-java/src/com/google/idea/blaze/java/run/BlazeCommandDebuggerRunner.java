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
package com.google.idea.blaze.java.run;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A runner that adapts the GenericDebuggerRunner to work with Blaze run configurations.
 */
public class BlazeCommandDebuggerRunner extends GenericDebuggerRunner {
  @Override
  @NotNull
  public String getRunnerId() {
    return "Blaze-Debug";
  }

  @Override
  public boolean canRun(@NotNull final String executorId, @NotNull final RunProfile profile) {
    return executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)
           && profile instanceof BlazeCommandRunConfiguration;
  }

  @Override
  public void patch(JavaParameters javaParameters, RunnerSettings runnerSettings,
                    RunProfile runProfile, final boolean beforeExecution) {
    // We don't want to support Java run configuration patching.
  }

  @Override
  @Nullable
  public RunContentDescriptor createContentDescriptor(
    @NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    if (!(state instanceof BlazeCommandRunProfileState)) {
      return null;
    }
    BlazeCommandRunProfileState blazeState = (BlazeCommandRunProfileState)state;
    RemoteConnection connection = blazeState.getRemoteConnection();
    return attachVirtualMachine(state, environment, connection, true /* pollConnection */);
  }
}
