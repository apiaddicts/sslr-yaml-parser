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

import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends RecognitionException {
  private final JsonNode node;
  private final List<ValidationException> causes;

  static ValidationException toException(ValidationIssue issue) {
    return new ValidationException(issue.getNode(), issue.getMessage(), issue.getCauses().stream().map(ValidationException::toException).collect(Collectors.toList()));
  }

  public JsonNode getNode() {
    return node;
  }

  public ValidationException(JsonNode node, String errorMessage) {
    super(node.getTokenLine(), errorMessage);
    this.node = node;
    this.causes = Collections.emptyList();
  }
  public ValidationException(JsonNode node, String errorMessage, ValidationException cause) {
    super(node.getTokenLine(), errorMessage);
    this.node = node;
    this.causes = Lists.newArrayList(cause);
  }
  public ValidationException(JsonNode node, String errorMessage, List<ValidationException> causes) {
    super(node.getTokenLine(), errorMessage);
    this.node = node;
    this.causes = causes;
  }

  public static String formatMessage(String prefix, ValidationException e) {
    StringBuilder b = new StringBuilder();
    b.append(prefix);
    if (!prefix.isEmpty()) {
      b.append("Caused by: on line ").append(e.node.getTokenLine()).append(": ");
    }
    b.append(e.getMessage()).append('\n');
    for (ValidationException cause : e.causes) {
      b.append(formatMessage(prefix + " ", cause));
    }
    return b.toString();
  }

  public String formatMessage() {
    return formatMessage("", this);
  }

  public List<ValidationException> getCauses() {
    return causes;
  }

  @Override
  public String toString() {
    return this.getClass().getName() + ": " + formatMessage();
  }
}
