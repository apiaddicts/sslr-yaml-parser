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
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;
import java.nio.charset.Charset;

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Lexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;

public class YamlGrammarTest {
  private Parser<Grammar> parser() {
    return Parser.builder(YamlGrammar.create().buildWithMemoizationOfMatchesForAllRules()).withLexer(Lexer.create(Charset.forName("UTF-8"))).build();
  }
  @Test
  public void can_parse_simple_block_objects() {
    AstNode node = parser().parse("p1: some content\np2: other content\np3: some content");

    Assert.assertEquals(YamlGrammar.ROOT, node.getType());
    Assertions.assertThat(node.getDescendants(YamlGrammar.BLOCK_PROPERTY)).extracting(this::getPropertyKey, this::getPropertyValue)
      .containsExactly(
        tuple("p1", "some content"),
        tuple("p2", "other content"),
        tuple("p3", "some content"));
  }

  @Test
  public void can_parse_simple_flow_objects() {
    AstNode node = parser().parse("{ \"p1\": \"some content\",\n\"p2\": \"other content\",\n\"p3\": \"some content\"}");

    Assert.assertEquals(YamlGrammar.ROOT, node.getType());
    Assertions.assertThat(node.getDescendants(YamlGrammar.FLOW_PROPERTY)).extracting(this::getPropertyKey, this::getPropertyValue)
      .containsExactly(
        tuple("p1", "some content"),
        tuple("p2", "other content"),
        tuple("p3", "some content"));
  }

  @Test
  public void can_parse_simple_block_arrays() {
    AstNode node = parser().parse("- 12\n- some string\n-\n  p1: some string");

    Assertions.assertThat(node.getFirstChild().getChildren(YamlGrammar.BLOCK_ARRAY_ELEMENT))
        .extracting(n -> n.getFirstChild().getType())
        .containsExactly(YamlGrammar.SCALAR, YamlGrammar.SCALAR, YamlGrammar.BLOCK_MAPPING);
  }

  @Test
  public void can_parse_simple_flow_arrays() {
    AstNode node = parser().parse("[\n  { p1: some string },\n {p1: other string}]");

    Assertions.assertThat(node.getFirstChild().getChildren(YamlGrammar.FLOW_ARRAY_ELEMENT))
        .extracting(n -> n.getFirstChild().getType())
        .containsExactly(YamlGrammar.FLOW_MAPPING, YamlGrammar.FLOW_MAPPING);
  }

  @Test
  public void can_parse_empty_flow_objects() {
    AstNode node = parser().parse("{}");

    Assertions.assertThat(node.getDescendants(YamlGrammar.FLOW_PROPERTY)).isEmpty();
  }

  @Test
  public void can_parse_empty_flow_arrays() {
    AstNode node = parser().parse("[]");

    assertThat(node.getDescendants()).isEmpty();
  }

  @Test
  public void can_parse_empty_objects_with_comments() {
    AstNode node = parser().parse("p1: {} # some comment{{}}\np2: s2");

    Assertions.assertThat(node.getDescendants(YamlGrammar.BLOCK_PROPERTY)).extracting(this::getPropertyKey).containsExactly("p1", "p2");

  }

  @Test
  public void can_parse_sets() {
    AstNode node = parser().parse("? one\n" +
        "? two\n" +
        "? three");
    Assertions.assertThat(node.getDescendants(YamlGrammar.BLOCK_PROPERTY)).extracting(this::getPropertyKey).containsExactly("one", "two", "three");
  }

  private String getPropertyKey(AstNode n) {
    return n.getFirstChild(Tokens.KEY).getNextSibling().getTokenValue();
  }

  private String getPropertyValue(AstNode n) {
    return n.getFirstChild(Tokens.VALUE).getNextSibling().getTokenValue();
  }
}
