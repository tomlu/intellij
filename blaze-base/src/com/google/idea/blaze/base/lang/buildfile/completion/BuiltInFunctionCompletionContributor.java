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
package com.google.idea.blaze.base.lang.buildfile.completion;

import com.google.idea.blaze.base.lang.buildfile.language.BuildFileLanguage;
import com.google.idea.blaze.base.lang.buildfile.language.semantics.BuildLanguageSpec;
import com.google.idea.blaze.base.lang.buildfile.language.semantics.BuildLanguageSpecProvider;
import com.google.idea.blaze.base.lang.buildfile.psi.BuildFile;
import com.google.idea.blaze.base.lang.buildfile.psi.FunctionStatement;
import com.google.idea.blaze.base.lang.buildfile.psi.ReferenceExpression;
import com.google.idea.blaze.base.lang.buildfile.psi.StatementList;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import icons.BlazeIcons;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Completes built-in blaze function names.
 */
public class BuiltInFunctionCompletionContributor extends CompletionContributor {

  @Override
  public AutoCompletionDecision handleAutoCompletionPossibility(AutoCompletionContext context) {
    // auto-insert the obvious only case; else show other cases.
    final LookupElement[] items = context.getItems();
    if (items.length == 1) {
      return AutoCompletionDecision.insertItem(items[0]);
    }
    return AutoCompletionDecision.SHOW_LOOKUP;
  }

  public BuiltInFunctionCompletionContributor() {
    extend(
      CompletionType.BASIC,
      psiElement()
        .withLanguage(BuildFileLanguage.INSTANCE)
        .andOr(
          // Handles only top-level rules, and rules inside a function statement.
          // There are several other possibilities (e.g. inside top-level list comprehension), but leaving out less common cases
          // to avoid cluttering the autocomplete suggestions when it's not valid to enter a rule.
          psiElement().withParents(ReferenceExpression.class, BuildFile.class), // leaf node => BuildReference => BuildFile
          psiElement()
            .inside(psiElement(StatementList.class).inside(psiElement(FunctionStatement.class)))
            .afterLeaf(psiElement().withText(".").afterLeaf(psiElement().withText("native")))
        ),
      new CompletionProvider<CompletionParameters>() {
        @Override
        protected void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
          BuildLanguageSpec spec = BuildLanguageSpecProvider.getInstance().getLanguageSpec(parameters.getPosition().getProject());
          if (spec == null) {
            return;
          }
          for (String ruleName : spec.getKnownRuleNames()) {
            result.addElement(
              LookupElementBuilder
                .create(ruleName)
                .withIcon(BlazeIcons.BuildRule)
                .withInsertHandler(ParenthesesInsertHandler.getInstance(true)));
          }
        }
      }
    );
  }

}
