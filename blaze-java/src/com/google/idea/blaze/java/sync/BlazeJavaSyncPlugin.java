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
package com.google.idea.blaze.java.sync;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.idea.blaze.base.experiments.BoolExperiment;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.LibraryArtifact;
import com.google.idea.blaze.base.ideinfo.RuleIdeInfo;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.SyncState;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.LanguageClass;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.model.primitives.WorkspaceType;
import com.google.idea.blaze.base.projectview.ProjectViewSet;
import com.google.idea.blaze.base.projectview.section.Glob;
import com.google.idea.blaze.base.projectview.section.SectionParser;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.scope.Scope;
import com.google.idea.blaze.base.scope.output.IssueOutput;
import com.google.idea.blaze.base.scope.output.PerformanceWarning;
import com.google.idea.blaze.base.scope.output.PrintOutput;
import com.google.idea.blaze.base.scope.scopes.TimingScope;
import com.google.idea.blaze.base.settings.BlazeUserSettings;
import com.google.idea.blaze.base.sync.BlazeSyncPlugin;
import com.google.idea.blaze.base.sync.projectview.WorkspaceLanguageSettings;
import com.google.idea.blaze.base.sync.workspace.ArtifactLocationDecoder;
import com.google.idea.blaze.base.sync.workspace.BlazeRoots;
import com.google.idea.blaze.base.sync.workspace.WorkingSet;
import com.google.idea.blaze.base.sync.workspace.WorkspacePathResolver;
import com.google.idea.blaze.java.projectview.ExcludeLibrarySection;
import com.google.idea.blaze.java.projectview.ExcludedLibrarySection;
import com.google.idea.blaze.java.projectview.JavaLanguageLevelSection;
import com.google.idea.blaze.java.sync.importer.BlazeJavaWorkspaceImporter;
import com.google.idea.blaze.java.sync.jdeps.JdepsFileReader;
import com.google.idea.blaze.java.sync.jdeps.JdepsMap;
import com.google.idea.blaze.java.sync.model.BlazeJavaImportResult;
import com.google.idea.blaze.java.sync.model.BlazeJavaSyncData;
import com.google.idea.blaze.java.sync.model.BlazeLibrary;
import com.google.idea.blaze.java.sync.projectstructure.Jdks;
import com.google.idea.blaze.java.sync.projectstructure.LibraryEditor;
import com.google.idea.blaze.java.sync.projectstructure.SourceFolderEditor;
import com.google.idea.blaze.java.sync.workingset.JavaWorkingSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ui.UIUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sync support for Java.
 */
public class BlazeJavaSyncPlugin extends BlazeSyncPlugin.Adapter {
  private static final BoolExperiment USE_WORKING_SET = new BoolExperiment("use.working.set", true);
  private static final Logger LOG = Logger.getInstance(BlazeJavaSyncPlugin.class);
  private final JdepsFileReader jdepsFileReader = new JdepsFileReader();

  @Nullable
  @Override
  public WorkspaceType getDefaultWorkspaceType() {
    return WorkspaceType.JAVA;
  }

  @Override
  public Set<LanguageClass> getSupportedLanguagesInWorkspace(WorkspaceType workspaceType) {
    if (workspaceType == WorkspaceType.JAVA) {
      return ImmutableSet.of(LanguageClass.JAVA);
    }
    return ImmutableSet.of();
  }

  @Nullable
  @Override
  public ModuleType getWorkspaceModuleType(WorkspaceType workspaceType) {
    if (workspaceType == WorkspaceType.JAVA) {
      return StdModuleTypes.JAVA;
    }
    return null;
  }

  @Override
  public void updateSyncState(Project project,
                              BlazeContext context,
                              WorkspaceRoot workspaceRoot,
                              ProjectViewSet projectViewSet,
                              WorkspaceLanguageSettings workspaceLanguageSettings,
                              BlazeRoots blazeRoots,
                              @Nullable WorkingSet workingSet,
                              WorkspacePathResolver workspacePathResolver,
                              ImmutableMap<Label, RuleIdeInfo> ruleMap,
                              @Deprecated @Nullable File androidPlatformDirectory,
                              SyncState.Builder syncStateBuilder,
                              @Nullable SyncState previousSyncState) {
    JavaWorkingSet javaWorkingSet = null;
    if (USE_WORKING_SET.getValue() && workingSet != null) {
      javaWorkingSet = new JavaWorkingSet(workspaceRoot, workingSet);
    }

    JdepsMap jdepsMap = jdepsFileReader.loadJdepsFiles(context, ruleMap, syncStateBuilder, previousSyncState);

    BlazeJavaWorkspaceImporter blazeJavaWorkspaceImporter = new BlazeJavaWorkspaceImporter(
      project,
      workspaceRoot,
      projectViewSet,
      ruleMap,
      jdepsMap,
      javaWorkingSet,
      new ArtifactLocationDecoder(blazeRoots, workspacePathResolver)
    );
    BlazeJavaImportResult importResult = Scope.push(context, (childContext) -> {
      childContext.push(new TimingScope("JavaWorkspaceImporter"));
      return blazeJavaWorkspaceImporter.importWorkspace(childContext);
    });
    Glob.GlobSet excludedLibraries = new Glob.GlobSet(
      ImmutableList.<Glob>builder()
        .addAll(projectViewSet.listItems(ExcludeLibrarySection.KEY))
        .addAll(projectViewSet.listItems(ExcludedLibrarySection.KEY))
        .build()
    );
    for (BlazeJavaSyncAugmenter syncAugmenter : BlazeJavaSyncAugmenter.EP_NAME.getExtensions()) {
      syncAugmenter.addLibraryFilter(excludedLibraries);
    }
    BlazeJavaSyncData syncData = new BlazeJavaSyncData(
      importResult,
      excludedLibraries,
      BlazeUserSettings.getInstance().getAttachSourcesByDefault()
    );
    syncStateBuilder.put(BlazeJavaSyncData.class, syncData);
  }

  @Override
  public void updateSdk(Project project,
                        BlazeContext context,
                        ProjectViewSet projectViewSet,
                        BlazeProjectData blazeProjectData) {
    if (!blazeProjectData.workspaceLanguageSettings.isWorkspaceType(WorkspaceType.JAVA)) {
      return;
    }
    updateJdk(project, context, projectViewSet, blazeProjectData);
  }

  @Override
  public void updateContentEntries(Project project,
                                   BlazeContext context,
                                   WorkspaceRoot workspaceRoot,
                                   ProjectViewSet projectViewSet,
                                   BlazeProjectData blazeProjectData,
                                   Collection<ContentEntry> contentEntries) {
    if (!blazeProjectData.workspaceLanguageSettings.isLanguageActive(LanguageClass.JAVA)) {
      return;
    }
    BlazeJavaSyncData syncData = blazeProjectData.syncState.get(BlazeJavaSyncData.class);
    if (syncData == null) {
      return;
    }

    SourceFolderEditor.modifyContentEntries(
      syncData.importResult,
      contentEntries
    );
  }

  @Override
  public void updateProjectStructure(Project project,
                                     BlazeContext context,
                                     WorkspaceRoot workspaceRoot,
                                     ProjectViewSet projectViewSet,
                                     BlazeProjectData blazeProjectData,
                                     @Nullable BlazeProjectData oldBlazeProjectData,
                                     ModuleEditor moduleEditor,
                                     Module workspaceModule,
                                     ModifiableRootModel workspaceModifiableModel) {
    BlazeJavaSyncData syncData = blazeProjectData.syncState.get(BlazeJavaSyncData.class);
    if (syncData == null) {
      return;
    }

    @Nullable BlazeJavaSyncData oldSyncData = oldBlazeProjectData != null
                                              ? oldBlazeProjectData.syncState.get(BlazeJavaSyncData.class)
                                              : null;

    if (syncData.attachSourceJarsByDefault) {
      context.output(PrintOutput.output(
        "Attaching source jars by default. This may lead to significant increases in indexing time,"
        + " project opening time, and IDE memory usage. To turn off go to"
        + " Settings > Other Settings > Blaze."
      ));
    }

    List<BlazeLibrary> newLibraries = getLibraries(blazeProjectData, syncData);
    final List<BlazeLibrary> oldLibraries;
    if (oldSyncData != null && oldSyncData.attachSourceJarsByDefault == syncData.attachSourceJarsByDefault) {
      oldLibraries = getLibraries(oldBlazeProjectData, oldSyncData);
    } else {
      oldLibraries = ImmutableList.of();
    }

    LibraryEditor.updateProjectLibraries(
      project,
      context,
      blazeProjectData,
      newLibraries,
      oldLibraries
    );

    LibraryEditor.configureDependencies(
      project,
      context,
      workspaceModifiableModel,
      newLibraries
    );
  }

  private static List<BlazeLibrary> getLibraries(BlazeProjectData blazeProjectData,
                                                 BlazeJavaSyncData syncData) {
    Glob.GlobSet excludedLibraries = syncData.excludedLibraries;

    List<BlazeLibrary> libraries = Lists.newArrayList();
    libraries.addAll(syncData.importResult.libraries.values());
    for (BlazeJavaSyncAugmenter syncAugmenter : BlazeJavaSyncAugmenter.EP_NAME.getExtensions()) {
      libraries.addAll(syncAugmenter.getAdditionalLibraries(blazeProjectData));
    }
    return libraries
      .stream()
      .filter(blazeLibrary -> !isExcluded(excludedLibraries, blazeLibrary.getLibraryArtifact()))
      .collect(Collectors.toList());
  }

  private static boolean isExcluded(Glob.GlobSet excludedLibraries, @Nullable LibraryArtifact libraryArtifact) {
    if (libraryArtifact == null) {
      return false;
    }
    ArtifactLocation jar = libraryArtifact.jar;
    ArtifactLocation runtimeJar = libraryArtifact.runtimeJar;
    return excludedLibraries.matches(jar.getRelativePath())
           || (runtimeJar != null && excludedLibraries.matches(runtimeJar.getRelativePath()));
  }

  private static void updateJdk(
    Project project,
    BlazeContext context,
    ProjectViewSet projectViewSet,
    BlazeProjectData blazeProjectData) {

    LanguageLevel javaLanguageLevel = JavaLanguageLevelHelper
      .getJavaLanguageLevel(projectViewSet, blazeProjectData, LanguageLevel.JDK_1_7);

    final Sdk sdk = Jdks.chooseOrCreateJavaSdk(javaLanguageLevel);
    if (sdk == null) {
      String msg = String.format(
        "Unable to find a JDK %1$s installed.\n", javaLanguageLevel.getPresentableText());
      msg += "After configuring a suitable JDK in the \"Project Structure\" dialog, "
             + "sync the project again.";
      IssueOutput.error(msg).submit(context);
      return;
    }
    setProjectSdkAndLanguageLevel(project, sdk, javaLanguageLevel);
  }

  private static void setProjectSdkAndLanguageLevel(
    final Project project,
    final Sdk sdk,
    final LanguageLevel javaLanguageLevel) {
    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> ApplicationManager.getApplication().runWriteAction(() -> {
      ProjectRootManagerEx rootManager = ProjectRootManagerEx.getInstanceEx(project);
      rootManager.setProjectSdk(sdk);
      LanguageLevelProjectExtension ext = LanguageLevelProjectExtension.getInstance(project);
      ext.setLanguageLevel(javaLanguageLevel);
    }));
  }

  @Override
  public boolean validate(Project project,
                          BlazeContext context,
                          BlazeProjectData blazeProjectData) {
    BlazeJavaSyncData syncData = blazeProjectData.syncState.get(BlazeJavaSyncData.class);
    if (syncData == null) {
      return true;
    }
    warnAboutDeployJars(context, syncData);
    return true;
  }

  @Override
  public Collection<SectionParser> getSections() {
    return ImmutableList.of(
      ExcludedLibrarySection.PARSER,
      ExcludeLibrarySection.PARSER,
      JavaLanguageLevelSection.PARSER
    );
  }

  @Override
  public boolean requiresResolveIdeArtifacts() {
    return true;
  }

  /**
   * Looks at your jars for anything that seems to be a deploy jar and
   * warns about it. This often turns out to be a duplicate copy of
   * all your application's code, so you don't want it in your project.
   */
  private static void warnAboutDeployJars(
    BlazeContext context,
    BlazeJavaSyncData syncData) {
    for (BlazeLibrary library : syncData.importResult.libraries.values()) {
      LibraryArtifact libraryArtifact = library.getLibraryArtifact();
      if (libraryArtifact == null) {
        continue;
      }
      ArtifactLocation artifactLocation = libraryArtifact.jar;
      if (artifactLocation.getRelativePath().endsWith("deploy.jar")
          || artifactLocation.getRelativePath().endsWith("deploy-ijar.jar")
          || artifactLocation.getRelativePath().endsWith("deploy-hjar.jar")) {
        context.output(new PerformanceWarning(
          "Performance warning: You have added a deploy jar as a library. "
          + "This can lead to poor indexing performance, and the debugger may "
          + "become confused and step into the deploy jar instead of your code. "
          + "Consider redoing the rule to not use deploy jars, exclude the target "
          + "from your .blazeproject, or exclude the library.\n"
          + "Library path: " + artifactLocation.getRelativePath()
        ));
      }
    }
  }

  @Override
  public Set<String> prefetchSrcFileExtensions() {
    return ImmutableSet.of("java");
  }
}
