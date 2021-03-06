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

import com.google.idea.blaze.base.settings.Blaze;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;


public class BlazeImportProjectAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    BlazeNewProjectWizard wizard = new BlazeNewProjectWizard(
      new BlazeNewProjectImportProvider(new BlazeProjectImportBuilder()));
    if (!wizard.showAndGet()) {
      return;
    }
    //noinspection ConstantConditions
    NewProjectUtil.createFromWizard(wizard, null);
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation().setText(String.format("Import %s Project...", Blaze.defaultBuildSystemName()));
  }
}
