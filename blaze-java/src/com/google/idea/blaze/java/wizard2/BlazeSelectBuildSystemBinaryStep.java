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
package com.google.idea.blaze.java.wizard2;

import com.google.idea.blaze.base.settings.Blaze.BuildSystem;
import com.google.idea.blaze.base.settings.BlazeUserSettings;
import com.google.idea.blaze.base.ui.BlazeValidationResult;
import com.google.idea.blaze.base.wizard2.BlazeNewProjectBuilder;
import com.google.idea.blaze.base.wizard2.ui.SelectBazelBinaryControl;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.projectImport.ProjectImportWizardStep;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class BlazeSelectBuildSystemBinaryStep extends ProjectImportWizardStep {

  private final JPanel component = new JPanel(new BorderLayout());
  private SelectBazelBinaryControl control;
  private boolean settingsInitialized = false;

  public BlazeSelectBuildSystemBinaryStep(@NotNull WizardContext context) {
    super(context);
  }

  @Override
  public boolean isStepVisible() {
    updateStep();
    if (control.builder.getBuildSystem() != BuildSystem.Bazel) {
      return false;
    }
    String currentBinaryPath = BlazeUserSettings.getInstance().getBazelBinaryPath();
    return currentBinaryPath == null;
  }

  @Override
  public JComponent getComponent() {
    return component;
  }

  @Override
  public void updateStep() {
    if (!settingsInitialized) {
      init();
    }
  }

  private void init() {
    control = new SelectBazelBinaryControl(getProjectBuilder());
    component.add(control.getUiComponent());
    settingsInitialized = true;
  }

  @Override
  public boolean validate() throws ConfigurationException {
    BlazeValidationResult result = control.validate();
    if (!result.success) {
      throw new ConfigurationException(result.error.getError());
    }
    return true;
  }

  @Override
  public void updateDataModel() {
  }

  @Override
  public void onWizardFinished() throws CommitStepException {
    control.commit();
  }

  private BlazeNewProjectBuilder getProjectBuilder() {
    BlazeProjectImportBuilder builder = (BlazeProjectImportBuilder)getWizardContext().getProjectBuilder();
    assert builder != null;
    return builder.builder();
  }
}
