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
package com.google.idea.blaze.java.sync.jdeps;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.idea.blaze.base.async.FutureUtil;
import com.google.idea.blaze.base.async.executor.BlazeExecutor;
import com.google.idea.blaze.base.ideinfo.ArtifactLocation;
import com.google.idea.blaze.base.ideinfo.JavaRuleIdeInfo;
import com.google.idea.blaze.base.ideinfo.RuleIdeInfo;
import com.google.idea.blaze.base.model.SyncState;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.prefetch.PrefetchService;
import com.google.idea.blaze.base.scope.BlazeContext;
import com.google.idea.blaze.base.scope.Scope;
import com.google.idea.blaze.base.scope.output.PrintOutput;
import com.google.idea.blaze.base.scope.scopes.TimingScope;
import com.google.idea.blaze.base.sync.filediff.FileDiffService;
import com.google.repackaged.devtools.build.lib.view.proto.Deps;
import com.intellij.openapi.diagnostic.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reads jdeps from the ide info result.
 */
public class JdepsFileReader {
  private static final Logger LOG = Logger.getInstance(JdepsFileReader.class);
  private final FileDiffService fileDiffService = new FileDiffService();

  static class JdepsState implements Serializable {
    private static final long serialVersionUID = 2L;
    private FileDiffService.State fileState = null;
    private Map<File, Label> fileToLabelMap = Maps.newHashMap();
    private Map<Label, List<String>> labelToJdeps = Maps.newHashMap();
  }

  private static class Result {
    File file;
    Label label;
    List<String> dependencies;
    public Result(File file, Label label, List<String> dependencies) {
      this.file = file;
      this.label = label;
      this.dependencies = dependencies;
    }
  }

  /**
   * Loads any updated jdeps files since the last invocation of this method.
   */
  @Nullable
  public JdepsMap loadJdepsFiles(BlazeContext parentContext,
                                 ImmutableMap<Label, RuleIdeInfo> ruleMap,
                                 SyncState.Builder syncStateBuilder,
                                 @Nullable SyncState previousSyncState) {
    JdepsState oldState = previousSyncState != null ? previousSyncState.get(JdepsState.class) : null;
    JdepsState jdepsState = Scope.push(parentContext, (context) -> {
      context.push(new TimingScope("LoadJdepsFiles"));
      return doLoadJdepsFiles(context, oldState, ruleMap);
    });
    if (jdepsState == null) {
      return null;
    }
    syncStateBuilder.put(JdepsState.class, jdepsState);
    return label -> jdepsState.labelToJdeps.get(label);
  }

  private JdepsState doLoadJdepsFiles(BlazeContext context,
                                      @Nullable JdepsState oldState,
                                      ImmutableMap<Label, RuleIdeInfo> ruleMap) {
    JdepsState state = new JdepsState();
    if (oldState != null) {
      state.labelToJdeps = Maps.newHashMap(oldState.labelToJdeps);
      state.fileToLabelMap = Maps.newHashMap(oldState.fileToLabelMap);
    }

    List<File> files = Lists.newArrayList();
    for (RuleIdeInfo ruleIdeInfo : ruleMap.values()) {
      JavaRuleIdeInfo javaRuleIdeInfo = ruleIdeInfo.javaRuleIdeInfo;
      if (javaRuleIdeInfo != null) {
        ArtifactLocation jdepsFile = javaRuleIdeInfo.jdepsFile;
        if (jdepsFile != null) {
          files.add(jdepsFile.getFile());
        }
      }
    }

    List<File> updatedFiles = Lists.newArrayList();
    List<File> removedFiles = Lists.newArrayList();
    state.fileState = fileDiffService.updateFiles(oldState != null ? oldState.fileState : null, files, updatedFiles, removedFiles);

    ListenableFuture<?> fetchFuture = PrefetchService.getInstance().prefetchFiles(updatedFiles, true);
    if (!FutureUtil.waitForFuture(context, fetchFuture)
      .timed("FetchJdeps")
      .run()
      .success()) {
      return null;
    }

    for (File removedFile : removedFiles) {
      Label label = state.fileToLabelMap.remove(removedFile);
      if (label != null) {
        state.labelToJdeps.remove(label);
      }
    }

    AtomicLong totalSizeLoaded = new AtomicLong(0);

    List<ListenableFuture<Result>> futures = Lists.newArrayList();
    for (File updatedFile : updatedFiles) {
      futures.add(submit(() -> {
        totalSizeLoaded.addAndGet(updatedFile.length());
        try (InputStream inputStream = new FileInputStream(updatedFile)){
          Deps.Dependencies dependencies = Deps.Dependencies.parseFrom(inputStream);
          if (dependencies != null) {
            if (dependencies.hasRuleLabel()) {
              Label label = new Label(dependencies.getRuleLabel());
              List<String> dependencyStringList = Lists.newArrayList();
              for (Deps.Dependency dependency : dependencies.getDependencyList()) {
                dependencyStringList.add(dependency.getPath());
              }
              return new Result(updatedFile, label, dependencyStringList);
            }
          }
        } catch (FileNotFoundException e) {
          LOG.info("Could not open jdeps file: " + updatedFile);
        }
        return null;
      }));
    }
    try {
      for (Result result : Futures.allAsList(futures).get()) {
        if (result != null) {
          state.fileToLabelMap.put(result.file, result.label);
          state.labelToJdeps.put(result.label, result.dependencies);
        }
      }
      context.output(new PrintOutput(String.format(
        "Loaded %d jdeps files, total size %dkB", updatedFiles.size(), totalSizeLoaded.get() / 1024
      )));
    }
    catch (InterruptedException | ExecutionException e) {
      LOG.error(e);
      return null;
    }
    return state;
  }

  private static <T> ListenableFuture<T> submit(Callable<T> callable) {
    return BlazeExecutor.getInstance().submit(callable);
  }
}
