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
package com.google.idea.blaze.java.sync.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.File;
import java.io.Serializable;

/**
 * The result of a blaze import operation.
 */
@Immutable
public class BlazeJavaImportResult implements Serializable {
  private static final long serialVersionUID = 3L;

  public final ImmutableList<BlazeContentEntry> contentEntries;
  public final ImmutableMap<LibraryKey, BlazeLibrary> libraries;
  public final ImmutableCollection<File> buildOutputJars;
  public final ImmutableSet<File> javaSourceFiles;
  @Nullable public final String sourceVersion;

  public BlazeJavaImportResult(ImmutableList<BlazeContentEntry> contentEntries,
                               ImmutableMap<LibraryKey, BlazeLibrary> libraries,
                               ImmutableCollection<File> buildOutputJars,
                               ImmutableSet<File> javaSourceFiles,
                               @Nullable String sourceVersion) {
    this.contentEntries = contentEntries;
    this.libraries = libraries;
    this.buildOutputJars = buildOutputJars;
    this.javaSourceFiles = javaSourceFiles;
    this.sourceVersion = sourceVersion;
  }
}
