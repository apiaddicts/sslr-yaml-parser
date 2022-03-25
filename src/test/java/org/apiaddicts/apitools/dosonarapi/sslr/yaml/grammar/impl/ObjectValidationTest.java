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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ParsingException;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.PropertyDescription;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationIssue;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

public class ObjectValidationTest extends ValidationTestBase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void matches_required_properties() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, true, false, new AlwaysTrueValidation()));
    validation.addProperty(new PropertyDescriptionImpl("p2", false, true, false, new AlwaysTrueValidation()));

    JsonNode rootNode = parseText("p1: v1\np2: v2");

    validation.visit(rootNode, context);

    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void matches_pattern_property() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("p.*", true, false, false, new AlwaysTrueValidation()));

    JsonNode rootNode = parseText("p1: v1");

    validation.visit(rootNode, context);

    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void records_failure_on_unexpected_property() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("t", false, false, false, new AlwaysTrueValidation()));
    validation.addProperty(new PropertyDescriptionImpl("p.*", true, false, false, new AlwaysTrueValidation()));
    JsonNode rootNode = parseText("u1: v1");

    boolean valid = validation.visit(rootNode, context);

    assertThat(valid).isTrue();
    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Unexpected property: \"u1\"");
  }

  @Test
  public void matches_on_missing_optional_property() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, false, false, new AlwaysTrueValidation()));

    JsonNode rootNode = parseText("{}");

    boolean valid = validation.visit(rootNode, context);

    assertThat(valid).isTrue();
    assertThat(context.captured()).isEmpty();
  }

  @Test
  public void fails_if_missing_property() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, true, false, new AlwaysTrueValidation()));
    validation.addProperty(new PropertyDescriptionImpl("p2", false, true, false, new AlwaysTrueValidation()));
    JsonNode rootNode = parseText("p1: v1");

    boolean valid = validation.visit(rootNode, context);

    assertThat(valid).isFalse();
    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Missing required properties: [p2]");
  }

  @Test()
  public void throws_if_same_property_seen_twice() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, false, false, new AlwaysTrueValidation()));
    validation.addProperty(new PropertyDescriptionImpl("x-.*", true, false, false, new AlwaysTrueValidation()));
    JsonNode rootNode = parseText("p1: v1\np1: v2");

    exception.expect(ParsingException.class);
    exception.expectMessage("Property \"p1\" is already defined in this object");

    validation.visit(rootNode, context);

    // Don't put assertions here: nothing gets executed past that point
  }

  @Test
  public void registers_violations_and_succeeds_if_property_validation_fails() {
    ObjectValidation validation = new ObjectValidation();
    PropertyDescription delegate = failingPropertyValueValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, false, false, delegate));

    JsonNode rootNode = parseText("p1: v1");

    boolean valid = validation.visit(rootNode, context);

    assertThat(valid).isTrue(); // structurally correct
    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Test violation message");
  }

  @Test
  public void faild_if_discriminant_fails() {
    ObjectValidation validation = new ObjectValidation();
    PropertyDescription delegate = failingPropertyValueValidation();
    validation.addProperty(new PropertyDescriptionImpl("p1", false, true, true, delegate));

    JsonNode rootNode = parseText("p1: v1");

    boolean valid = validation.visit(rootNode, context);

    assertThat(valid).isFalse();
    assertThat(context.captured()).extracting(ValidationIssue::getMessage)
        .containsExactly("Test violation message");
  }

  private static PropertyDescription failingPropertyValueValidation() {
    PropertyDescription delegate = Mockito.mock(PropertyDescription.class);
    doAnswer((Answer<Object>) invocation -> {
      ValidationRule.Context context = (ValidationRule.Context) invocation.getArguments()[1];
      context.recordFailure((JsonNode) invocation.getArguments()[0], "Test violation message");
      return false;
    }).when(delegate).visit(any(JsonNode.class), any(ValidationRule.Context.class));
    return delegate;
  }
}
