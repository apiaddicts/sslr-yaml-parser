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
import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.sslr.grammar.GrammarRuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammarBuilderTest.TestGrammar.CHILD1;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammarBuilderTest.TestGrammar.CHILD2;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammarBuilderTest.TestGrammar.ROOT;

public class YamlGrammarBuilderTest {

  private YamlGrammarBuilder b;
  private YamlGrammarBuilder yb;
  private List<ValidationIssue> issues;

  @Before
  public void resetIssues() {
    issues = Collections.emptyList();
  }

  @Test
  public void can_parse_simple_objects() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.string()),
      yb.mandatoryProperty("p2", b.string()),
      yb.mandatoryProperty("p3", b.string())));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb, "p1: some content\np2: other content\np3: some content");

    Assert.assertEquals(ROOT, node.getType());
    assertThat(node.propertyMap().keySet()).containsExactly("p1", "p2", "p3");
    assertEquals("some content", node.at("/p1").getTokenValue());
    assertEquals("other content", node.at("/p2").getTokenValue());
  }

  @Test
  public void can_parse_generic_arrays() {
    b.setRootRule(ROOT);
    b.rule(ROOT).is(b.anyArray());

    JsonNode node = parseDocument(b, "/parser/array.yaml");

    List<JsonNode> elements = node.elements();
    assertThat(elements).hasSize(5);
    assertTrue(elements.get(0).booleanValue());
    assertEquals(1.0, elements.get(1).floatValue(), 0.001);
    assertEquals(-23, elements.get(2).intValue());
    assertEquals("some value", elements.get(3).at("/p1").getTokenValue());
    assertEquals("some other value", elements.get(3).at("/p2").getTokenValue());
    assertTrue(elements.get(4).isObject());
    assertThat(elements.get(4).propertyMap()).isEmpty();
  }

  @Test
  public void can_parse_typed_arrays() {
    b.rule(CHILD1).is(yb.object(yb.mandatoryProperty("p1", b.string())));
    b.rule(ROOT).is(yb.array(CHILD1));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb, "-\n  p1: some string\n-\n  p1: other string");

    assertTrue(node.isArray());
    assertThat(node.elements()).extracting(o -> o.at("/p1").getTokenValue()).containsExactly("some string", "other string");
  }

  @Test
  public void can_parse_patterns_in_object() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.integer()),
      yb.patternProperty("x-.+", b.string()),
      yb.mandatoryProperty("p2", b.integer())));
    b.setRootRule(ROOT);

    JsonNode astNode = parseDocument(yb, "/parser/patterns.yaml");

    assertThat(astNode.propertyMap().keySet()).containsExactlyInAnyOrder("x-first", "x-second", "p1", "p2");
  }

  @Test
  public void can_parse_missing_patterns() {
    b.rule(ROOT).is(yb.object(
      yb.patternProperty("x-.+", b.string()),
      yb.mandatoryProperty("p1", b.integer())));
    b.setRootRule(ROOT);

    JsonNode astNode = parseText(yb, "p1: 10");

    assertThat(astNode.propertyMap().keySet()).containsExactlyInAnyOrder("p1");
  }

  @Test
  public void can_parse_missing_optionals() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.integer()),
      yb.property("p2", b.integer())));
    b.setRootRule(ROOT);

    JsonNode astNode = parseText(yb, "p1: 10");

    assertThat(astNode.propertyMap().keySet()).containsExactlyInAnyOrder("p1");
  }

  @Test
  public void can_parse_present_optionals() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.integer()),
      yb.property("p2", b.integer())));
    b.setRootRule(ROOT);

    JsonNode astNode = parseText(yb, "p1: 10\n" +
      "p2: 11");

    assertThat(astNode.propertyMap().keySet()).containsExactlyInAnyOrder("p1", "p2");
  }

  @Test
  public void can_parse_empty_object() {
    b.rule(ROOT).is(yb.object(
      yb.property("p1", b.integer())));
    b.setRootRule(ROOT);

    JsonNode jsonNode = parseText(yb, "{}");

    Assertions.assertThat(jsonNode.is(ROOT)).isTrue();
  }

  @Test
  public void can_parse_pattern_after_mandatory() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.string()),
      yb.patternProperty("^x-.*", b.string())));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb, "p1: some content\nx-p1: other content");
    assertThat(node.propertyMap().keySet()).containsExactlyInAnyOrder("p1", "x-p1");
  }

  @Test
  public void can_parse_empty_array() {
    b.rule(ROOT).is(yb.array(b.integer()));
    b.setRootRule(ROOT);

    JsonNode jsonNode = parseText(yb, "[]");

    Assertions.assertThat(jsonNode.is(ROOT)).isTrue();
  }

  @Test(expected = ValidationException.class)
  public void fails_if_missing_property() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.integer())));
    b.setRootRule(ROOT);

    parseText(yb, "{}");
  }

  @Test
  public void can_parse_fixed_values() {
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.firstOf("v1", "v2"))));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb, "\"p1\": \"v1\"");
    assertThat(node.propertyMap().keySet()).containsExactlyInAnyOrder("p1");
  }

  @Before
  public void prepareBuilders() {
    b = new YamlGrammarBuilder();
    yb = b;
  }

  @Test
  public void can_parse_array_of_objects() {
    b.rule(ROOT).is(yb.array(b.firstOf(CHILD1, CHILD2)));
    b.rule(CHILD1).is(yb.object(yb.mandatoryProperty("p1", b.bool()), yb.mandatoryProperty("p2", b.floating())));
    b.rule(CHILD2).is(yb.object(yb.mandatoryProperty("p1", b.integer())));
    b.setRootRule(ROOT);

    JsonNode node = parseDocument(yb, "/parser/array-compound.yaml");
    assertThat(node.elements()).extracting(AstNode::getType).containsExactly(CHILD1, CHILD2);
  }

  @Test
  public void can_recurse_object_definitions() {
    b.rule(ROOT).is(yb.object(
      yb.property("p1", b.string()),
      yb.property("p2", b.string()),
      yb.property("recurse", yb.object(yb.patternProperty(".*", ROOT)))));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb, "p1: v1\nrecurse:\n  s1:\n    p2: v1\np2: v2");
    assertThat(node.propertyMap().keySet()).containsExactlyInAnyOrder("p1", "recurse", "p2");
  }

  @Test
  public void can_recurse_object_definitions_with_options() {
    b.rule(CHILD1).is(yb.object(yb.property("c1", b.string())));
    b.rule(ROOT).is(yb.object(
      yb.mandatoryProperty("p1", b.string()),
      yb.property("p2", b.string()),
      // Important - ROOT is coming first, to fail before matching CHILD1
      yb.property("recurse", yb.object(yb.patternProperty(".*", b.firstOf(ROOT, CHILD1))))));
    b.setRootRule(ROOT);

    JsonNode node = parseText(yb,
           "p1: v1\n" +
           "recurse:\n" +
            "  s1:\n" +
            "    c1: v1\n" +
            "p2: v2");
    assertThat(node.propertyMap().keySet()).containsExactlyInAnyOrder("p1", "recurse", "p2");
  }

  @Test
  public void can_report_dual_property_errors() {
    b.rule(ROOT).is(b.object(
      b.mandatoryProperty("p1", b.string()),
      b.mandatoryProperty("p2", b.string())));
    b.setRootRule(ROOT);

    try {
      parseText(b, "p1: v1\n'p1': v2");
    } catch (RecognitionException e) {
      assertThat(e.getLine()).isEqualTo(2);
      assertThat(firstLineOf(e)).isEqualTo("Property \"p1\" is already defined in this object");
    }
  }

  @Test
  public void can_report_missing_property_errors() {
    b.rule(ROOT).is(b.object(
      b.mandatoryProperty("p1", b.string()),
      b.mandatoryProperty("p2", b.string()),
      b.mandatoryProperty("p3", b.string())));
    b.setRootRule(ROOT);

    try {
      parseText(b, "p1: v1\np2: v2");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(1);
        assertThat(firstLineOf(e)).isEqualTo("Missing required properties: [p3]");
      });
    }
  }

  @Test
  public void can_report_element_type_mismatch_error() {
    b.rule(ROOT).is(b.array(b.string()));
    b.setRootRule(ROOT);

    try {
      parseText(b, "- a string\n-\n p1: a property");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(3);
        assertThat(firstLineOf(e)).isEqualTo("Expected: One of [STRING, NULL], got: BLOCK_MAPPING");
      });
    }
  }

  @Test
  public void can_report_property_type_mismatch_error() {
    b.rule(ROOT).is(b.object(
        b.property("p1", b.string()),
        b.property("p2", "fixed value")
    ));
    b.setRootRule(ROOT);

    try {
      parseText(b, "p1: a string\np2: incorrect value");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(2);
        assertThat(firstLineOf(e)).isEqualTo("p2: Expected: \"fixed value\", got: \"incorrect value\"");
      });
    }
  }

  private static void assertSingleCause(ValidationException e, Consumer<ValidationException> assertion) {
    assertThat(e.getCauses()).hasSize(1);
    assertion.accept(e.getCauses().get(0));
  }

  @Test
  public void can_report_incorrect_token_type_error() {
    b.rule(ROOT).is(YamlGrammar.SCALAR);
    b.setRootRule(ROOT);

    try {
      parseText(b, "p1: a string");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(1);
        assertThat(firstLineOf(e)).isEqualTo("Expected: SCALAR, got: BLOCK_MAPPING");
      });
    }
  }

  @Test
  public void can_report_incorrect_value_error() {
    b.rule(ROOT).is("fixed value");
    b.setRootRule(ROOT);

    try {
      parseText(b, "other value");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(1);
        assertThat(firstLineOf(e)).isEqualTo("Expected: \"fixed value\", got: \"other value\"");
      });
    }
  }

  @Test
  public void can_report_first_of_error() {
    b.rule(ROOT).is(b.firstOf("fixed value", "second value"));
    b.setRootRule(ROOT);

    try {
      parseText(b, "other value");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(1);
        assertThat(firstLineOf(e)).isEqualTo("Expected one of [\"fixed value\", \"second value\"]");
      });
    }
  }

  @Test
  public void rule_definitions_reports_delegate_error() {
    b.rule(ROOT).is(CHILD1);
    b.rule(CHILD1).is("fixed value");
    b.setRootRule(ROOT);

    try {
      parseText(b, "other value");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertSingleCause(ex, e -> {
        assertThat(e.getLine()).isEqualTo(1);
        assertThat(firstLineOf(e)).isEqualTo("Expected: \"fixed value\", got: \"other value\"");
      });
    }
  }

  @Test
  public void captures_all_warnings() {
    b.rule(ROOT).is(b.object(
        b.property("p1", CHILD1),
        b.property("p2", b.integer())
    ));
    b.rule(CHILD1).is(b.object(
        b.property("a", b.integer()),
        b.property("b", b.string())
    ));
    b.setRootRule(ROOT);

    try {
      parseText(b, "p1:\n" +
          "  a: stuff\n" +
          "  b: text\n" +
          "  c: unexpected\n" +
          "p2: 42");
      fail("Should throw");
    } catch (ValidationException ex) {
      assertThat(ex.getCauses()).hasSize(2);
      assertThat(ex.getCauses()).extracting(Exception::getMessage).containsExactlyInAnyOrder(
          "a: Expected: INTEGER, got: \"stuff\"",
          "Unexpected property: \"c\""
      );
    }
  }


  private static String firstLineOf(RecognitionException e) {
    return e.getMessage().split("\n")[0];
  }

  private JsonNode parseDocument(YamlGrammarBuilder b, String s) {
    YamlParser parser = YamlParser.builder().withCharset(Charset.forName("UTF-8")).withGrammar(b).build();
    URL resource = this.getClass().getResource(s);
    return parser.parse(new File(resource.getFile()));
  }

  private JsonNode parseText(YamlGrammarBuilder b, String s) {
    YamlParser parser = YamlParser.builder().withCharset(Charset.forName("UTF-8")).withGrammar(b).withStrictValidation(true).build();
    JsonNode parse = parser.parse(s);
    this.issues = parser.getIssues();
    return parse;
  }

  enum TestGrammar implements GrammarRuleKey {
    ROOT,
    CHILD1,
    CHILD2,
  }
}
