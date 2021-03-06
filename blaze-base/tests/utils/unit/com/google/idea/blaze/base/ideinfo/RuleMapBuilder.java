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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.idea.blaze.base.model.primitives.Label;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builds a rule map.
 */
public class RuleMapBuilder {
  private List<RuleIdeInfo> rules = Lists.newArrayList();

  public static RuleMapBuilder builder() {
    return new RuleMapBuilder();
  }

  @NotNull
  public RuleMapBuilder addRule(@NotNull RuleIdeInfo ruleOrLibrary) {
    rules.add(ruleOrLibrary);
    return this;
  }
  @NotNull
  public RuleMapBuilder addRule(@NotNull RuleIdeInfo.Builder ruleOrLibrary) {
    return addRule(ruleOrLibrary.build());
  }

  @NotNull
  public ImmutableMap<Label, RuleIdeInfo> build() {
    ImmutableMap.Builder<Label, RuleIdeInfo> ruleMap = ImmutableMap.builder();
    for (RuleIdeInfo rule : rules) {
      ruleMap.put(rule.label, rule);
    }
    return ruleMap.build();
  }
}
