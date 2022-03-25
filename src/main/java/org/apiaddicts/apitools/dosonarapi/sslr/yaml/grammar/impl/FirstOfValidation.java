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
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationIssue;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationRule;

public class FirstOfValidation implements ValidationRule {
  private final ValidationRule[] delegates;

  public FirstOfValidation(ValidationRule... delegates) {
    this.delegates = delegates;
  }

  @Override
  public boolean visit(JsonNode node, Context context) {
    List<ValidationIssue> errorMessages = new ArrayList<>();
    for (ValidationRule delegate : delegates) {
      context.capture();
      boolean valid = delegate.visit(node, context);
      List<ValidationIssue> issues = context.captured();
      if (valid) {
        for (ValidationIssue issue : issues) {
          context.recordWarning(issue.getNode(), issue.getMessage());
        }
        return true;
      } else {
        errorMessages.add(new ValidationIssue(node, "Not " + delegate, ValidationIssue.Severity.WARNING, issues));
      }
    }
    String pointer = node.key().stringValue();
    if (!pointer.isEmpty()) {
      pointer = pointer + ": ";
    }
    context.recordFailure(node, pointer + "Expected " + this.toString(), errorMessages.toArray(new ValidationIssue[0]));
    return false;
  }

  @Override
  public String toString() {
    return "one of " + Arrays.toString(delegates);
  }
}
