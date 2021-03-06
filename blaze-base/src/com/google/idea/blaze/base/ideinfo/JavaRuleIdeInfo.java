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
package com.google.idea.blaze.base.ideinfo;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;

/**
 * Ide info specific to java rules.
 */
public final class JavaRuleIdeInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  /**
   * The main jar(s) produced by this java rule.
   *
   * <p>Usually this will be a single jar, but java_imports support importing multiple jars.
   */
  public final Collection<LibraryArtifact> jars;

  /**
   * A jar containing annotation processing.
   */
  public final Collection<LibraryArtifact> generatedJars;

  /**
   * File containing a map from .java files to their corresponding package.
   */
  @Nullable public final ArtifactLocation packageManifest;

  /**
   * File containing dependencies.
   */
  @Nullable public final ArtifactLocation jdepsFile;

  public JavaRuleIdeInfo(Collection<LibraryArtifact> jars,
                         Collection<LibraryArtifact> generatedJars,
                         @Nullable ArtifactLocation packageManifest,
                         @Nullable ArtifactLocation jdepsFile) {
    this.jars = jars;
    this.generatedJars = generatedJars;
    this.packageManifest = packageManifest;
    this.jdepsFile = jdepsFile;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    ImmutableList.Builder<LibraryArtifact> jars = ImmutableList.builder();
    ImmutableList.Builder<LibraryArtifact> generatedJars = ImmutableList.builder();

    public Builder addJar(LibraryArtifact.Builder jar) {
      jars.add(jar.build());
      return this;
    }

    public Builder addGeneratedJar(LibraryArtifact.Builder jar) {
      generatedJars.add(jar.build());
      return this;
    }

    public JavaRuleIdeInfo build() {
      return new JavaRuleIdeInfo(jars.build(), generatedJars.build(), null, null);
    }
  }
}
