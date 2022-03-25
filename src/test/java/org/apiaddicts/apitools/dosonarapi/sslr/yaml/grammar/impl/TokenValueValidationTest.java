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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenValueValidationTest extends ValidationTestBase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void matches_any_string() {
    TokenValueValidation textual = new TokenValueValidation("some string");
    boolean valid = textual.visit(parseText("some string"), context);
    assertThat(valid).isTrue();
  }

  @Test
  public void matches_any_float() {
    TokenValueValidation numeric = new TokenValueValidation("42.036");
    boolean valid = numeric.visit(parseText("42.036"), context);
    assertThat(valid).isTrue();
  }

  @Test
  public void matches_any_boolean() {
    TokenValueValidation bool = new TokenValueValidation("yes");
    boolean valid = bool.visit(parseText("yes"), context);
    assertThat(valid).isTrue();
  }

  @Test
  public void fails_on_wrong_value() {
    TokenValueValidation validation = new TokenValueValidation("some string");
    JsonNode node = parseText("wrong string");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: \"some string\", got: \"wrong string\"");
  }

  @Test
  public void fails_on_non_scalar() {
    TokenValueValidation validation = new TokenValueValidation("some string");
    JsonNode node = parseText("p1: v1");

    validation.visit(node, context);

    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: \"some string\", got: \"{\"");
  }
}
