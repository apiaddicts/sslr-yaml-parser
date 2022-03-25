/*
 * doSonarAPI SSLR :: YAML Parser
 * Copyright (C) 2021-2022 Apiaddicts
 * contacta AT apiaddicts DOT org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar;

import com.sonar.sslr.api.RecognitionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.sonar.sslr.internal.vm.lexerful.LexerfulParseErrorFormatter;

public class ASTValidator implements GrammarValidator {
  private final ValidationRule rootRule;

  public ASTValidator(ValidationRule rootRule) {
    this.rootRule = rootRule;
  }

  public List<ValidationIssue> validate(JsonNode node) {
    ContextImpl context = new ContextImpl();
    try {
      context.capture();
      rootRule.visit(node, context);
    } catch (ParsingException e) {
      String errorMsg = new LexerfulParseErrorFormatter().format(node.getTokens(), e.getErrorNode().getFromIndex());
      throw new RecognitionException(e.getErrorNode().getTokenLine(), e.getMessage() + "\n" + errorMsg);
    }
    return context.captured();
  }

  public static class ContextImpl implements ValidationRule.Context {
    private final Deque<List<ValidationIssue>> capturedErrors = new ArrayDeque<>();

    @Override
    public void recordFailure(JsonNode node, String message, ValidationIssue... causes) {
      if (causes.length > 0) {
        capturedErrors.peek().add(new ValidationIssue(node, message, ValidationIssue.Severity.ERROR, Arrays.asList(causes)));
      } else {
        capturedErrors.peek().add(new ValidationIssue(node, message));
      }
    }

    @Override
    public void recordWarning(JsonNode node, String message, ValidationIssue... causes) {
      if (causes.length > 0) {
        capturedErrors.peek().add(new ValidationIssue(node, message, ValidationIssue.Severity.WARNING, Arrays.asList(causes)));
      } else {
        capturedErrors.peek().add(new ValidationIssue(node, message, ValidationIssue.Severity.WARNING, Collections.emptyList()));
      }
    }

    @Override
    public void capture() {
      capturedErrors.push(new ArrayList<>());
    }

    @Override
    public List<ValidationIssue> captured() {
      return capturedErrors.pop();
    }
  }
}
