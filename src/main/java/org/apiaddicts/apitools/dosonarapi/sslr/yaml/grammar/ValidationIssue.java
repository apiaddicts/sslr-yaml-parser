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

import java.util.Collections;
import java.util.List;

public class ValidationIssue {
  private final JsonNode node;
  private final List<ValidationIssue> causes;
  private final String message;
  private final Severity severity;

  public JsonNode getNode() {
    return node;
  }

  public ValidationIssue(JsonNode node, String errorMessage) {
    this.node = node;
    this.message = errorMessage;
    this.severity = Severity.ERROR;
    this.causes = Collections.emptyList();
  }
  public ValidationIssue(JsonNode node, String errorMessage, Severity severity, List<ValidationIssue> causes) {
    this.node = node;
    this.message = errorMessage;
    this.severity = severity;
    this.causes = causes;
  }

  public static String formatMessage(String prefix, ValidationIssue e) {
    StringBuilder b = new StringBuilder();
    b.append(prefix);
    if (!prefix.isEmpty()) {
      b.append("Caused by: on line ").append(e.node.getTokenLine()).append(": ");
    }
    b.append(e.severity).append(" ").append(e.message);
    for (ValidationIssue cause : e.causes) {
      b.append('\n').append(formatMessage(prefix + "  ", cause));
    }
    return b.toString();
  }

  public String getMessage() {
    return message;
  }

  public String formatMessage() {
    return formatMessage("", this);
  }

  public List<ValidationIssue> getCauses() {
    return causes;
  }

  public enum Severity {
    ERROR,
    WARNING
  }
}
