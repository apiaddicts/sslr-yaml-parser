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

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationIssue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BooleanValidationTest extends ValidationTestBase {

  @Test
  public void matches_boolean_value() {
    BooleanValidation validation = new BooleanValidation(null);
    JsonNode node = parseText("y");

    validation.visit(node, context);

    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void fails_if_wrong_value() {
    BooleanValidation validation = new BooleanValidation(true);
    JsonNode node = parseText("false");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: true, got: \"false\"");
  }

  @Test
  public void fails_if_not_boolean() {
    BooleanValidation validation = new BooleanValidation(null);
    JsonNode node = parseText("potatoes");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: BOOLEAN, got: \"potatoes\"");
  }
}
