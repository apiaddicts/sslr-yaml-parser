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
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleDefinitionTest extends ValidationTestBase {

  @Test
  public void matches_if_delegate_matches() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());

    validation.visit(parseText("42"), context);
    Assertions.assertThat(context.captured()).isEmpty();

    context.capture();
    validation.visit(parseText("some string"), context);
    Assertions.assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Expected: INTEGER, got: \"some string\"");
  }

  @Test
  public void reuse_delegate_result_with_original_type_if_is_skipped() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());
    validation.skip();

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(node).isInstanceOf(ScalarNode.class);
    Assertions.assertThat(context.captured()).isEmpty();
  }

  @Test
  public void reuse_delegate_and_overrides_type_if_not_skipped() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.setValidation(new IntegerValidation());

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(FAKE_RULE);
    assertThat(node).isInstanceOf(ScalarNode.class);
    Assertions.assertThat(context.captured()).isEmpty();
  }

  @Test
  public void reuse_delegate_and_overrides_type_if_skipped_and_overridden() {
    RuleDefinition validation = new RuleDefinition(FAKE_RULE);
    validation.skip();
    validation.setValidation(new IntegerValidation());

    JsonNode node = parseText("42");
    validation.visit(node, context);

    assertThat(node.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(node).isInstanceOf(ScalarNode.class);
    Assertions.assertThat(context.captured()).isEmpty();
  }
}
