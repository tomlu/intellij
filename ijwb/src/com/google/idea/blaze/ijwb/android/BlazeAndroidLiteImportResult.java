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
package com.google.idea.blaze.ijwb.android;

import com.google.common.collect.ImmutableCollection;
import com.google.idea.blaze.java.sync.model.BlazeLibrary;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

/**
 * The result of a blaze import operation.
 */
@Immutable
public class BlazeAndroidLiteImportResult implements Serializable {
  private static final long serialVersionUID = 1L;

  public final ImmutableCollection<BlazeLibrary> libraries;

  public BlazeAndroidLiteImportResult(
    ImmutableCollection<BlazeLibrary> libraries) {
    this.libraries = libraries;
  }
}
