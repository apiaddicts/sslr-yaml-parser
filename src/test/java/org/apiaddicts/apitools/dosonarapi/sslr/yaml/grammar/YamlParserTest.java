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

import com.sonar.sslr.api.AstNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens;
import org.junit.Test;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ArrayNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ObjectNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.PropertyNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ScalarNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.SyntaxNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ValidationTestBase;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class YamlParserTest extends ValidationTestBase {

  @Test
  public void generates_object_structure() {
    JsonNode jsonNode = parseText("p1: v1");

    assertThat(jsonNode).isInstanceOf(ObjectNode.class);
    assertThat(jsonNode.getChildren()).extracting(AstNode::getClass, AstNode::getType).containsExactly(
        tuple(SyntaxNode.class, Tokens.BLOCK_MAPPING_START),
        tuple(PropertyNode.class, YamlGrammar.BLOCK_PROPERTY),
        tuple(SyntaxNode.class, Tokens.BLOCK_END),
        tuple(SyntaxNode.class, EOF)
    );
    JsonNode valueNode = jsonNode.at("/p1");
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.propertyMap()).containsOnlyKeys("p1");
    assertThat(jsonNode.properties()).extracting(JsonNode::stringValue).containsOnly("v1");
  }

  @Test
  public void generates_array_structure() {
    JsonNode jsonNode = parseText("[ v1, v2 ]");

    assertThat(jsonNode).isInstanceOf(ArrayNode.class);
    assertThat(jsonNode.getChildren()).extracting(AstNode::getClass, AstNode::getType).containsExactly(
        tuple(SyntaxNode.class, Tokens.FLOW_SEQUENCE_START),
        tuple(SyntaxNode.class, YamlGrammar.FLOW_ARRAY_ELEMENT),
        tuple(SyntaxNode.class, Tokens.FLOW_ENTRY),
        tuple(SyntaxNode.class, YamlGrammar.FLOW_ARRAY_ELEMENT),
        tuple(SyntaxNode.class, Tokens.FLOW_SEQUENCE_END),
        tuple(SyntaxNode.class, EOF)
    );
    JsonNode valueNode = jsonNode.elements().get(0);
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(valueNode.stringValue()).isEqualTo("v1");
    valueNode = jsonNode.elements().get(1);
    assertThat(valueNode).isInstanceOf(ScalarNode.class);
    assertThat(valueNode.stringValue()).isEqualTo("v2");
  }


  @Test
  public void generates_integer_scalar_node() {
    JsonNode jsonNode = parseText("42");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(jsonNode.intValue()).isEqualTo(42);
    assertThat(jsonNode.floatValue()).isEqualTo(42.0);
  }

  @Test
  public void generates_float_scalar_node() {
    JsonNode jsonNode = parseText("42.0");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(jsonNode.floatValue()).isEqualTo(42.0);
  }

  @Test
  public void generates_boolean_scalar_node() {
    JsonNode jsonNode = parseText("y");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(jsonNode.booleanValue()).isEqualTo(true);
  }

  @Test
  public void generates_null_scalar_node() {
    JsonNode jsonNode = parseText("null");

    assertThat(jsonNode).isInstanceOf(ScalarNode.class);
    assertThat(jsonNode.getType()).isEqualTo(YamlGrammar.SCALAR);
    assertThat(jsonNode.isNull()).isTrue();
  }
}
