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

import com.google.common.collect.Sets;
import com.sonar.sslr.api.AstNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.*;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.ValidationRule;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.BLOCK_MAPPING;

/**
 * Validates the structure of an object. This rule returns {@code true} if the structure of the object is respected,
 * i.e. if all mandatory properties are present, and all the discriminant are valid. Non-discriminant properties that
 * fail to validate record errors but don't imped the validation of the rule. Unexpected properties are recorded as
 * warnings.
 *
 * @see YamlParser to understand how strictness of parsing influences the interpretation
 *      of recorded warnings.
 */
public class ObjectValidation extends ValidationBase {
  private Set<String> mandatoryProperties = new HashSet<>();
  private Map<String, PropertyDescription> namedRules = new HashMap<>();
  private Map<String, ValidationRule> patternRules = new LinkedHashMap<>();

  public void addProperty(PropertyDescription rule) {
    if (rule.isPattern()) {
      patternRules.put(rule.getKey(), rule);
    } else {
      boolean isNew = namedRules.put(rule.getKey(), rule) == null;
      if (!isNew) {
        throw new IllegalStateException("Property \"" + rule.getKey() + "\" has already been declared");
      }
      if (rule.isMandatory()) {
        mandatoryProperties.add(rule.getKey());
      }
    }
  }

  @Override
  public String toString() {
    return "OBJECT";
  }

  @Override
  protected boolean validate(JsonNode node, ValidationRule.Context context) {
    if (node.getType() != YamlGrammar.FLOW_MAPPING && node.getType() != BLOCK_MAPPING) {
      context.recordFailure(node, "Expected object, got: " + node.getType());
      return false;
    }
    Set<String> observedProperties = new HashSet<>();

    boolean valid = true;
    for (AstNode child : node.getChildren(YamlGrammar.FLOW_PROPERTY, YamlGrammar.BLOCK_PROPERTY)) {
      valid &= matchProperty(context, observedProperties, child);
    }
    if (!observedProperties.containsAll(mandatoryProperties)) {
      context.recordFailure(node, "Missing required properties: " + Sets.difference(mandatoryProperties, observedProperties));
      valid = false;
    }
    return valid;
  }

  private boolean matchProperty(ValidationRule.Context context, Set<String> observedProperties, AstNode property) {
    AstNode keyNode = property.getFirstChild(Tokens.KEY);
    AstNode actualKey = keyNode.getNextSibling();
    if (actualKey == null) {
      return true; // no key value: this is not a property
    }
    String key = actualKey.getTokenValue();
    AstNode valueMarker = property.getFirstChild(Tokens.VALUE);
    if (valueMarker == null) {
      return true; // no value marker: this is not a property
    }
    AstNode value = valueMarker.getNextSibling();
    if (value == null) {
      return true; // no value value: this is not a property
    }
    boolean isNew = observedProperties.add(key);
    if (!isNew) {
      throw new ParsingException("Property \"" + key + "\" is already defined in this object", property);
    }
    return validateProperty(context, (JsonNode)keyNode, key, (JsonNode)value);
  }

  private boolean validateProperty(ValidationRule.Context context, JsonNode keyNode, String key, JsonNode value) {
    PropertyDescription rule = namedRules.get(key);
    if (rule != null) {
      boolean valid = rule.visit(value, context);
      return valid || !rule.isDiscriminant();
    } else  {
      for (Map.Entry<String, ValidationRule> entry : patternRules.entrySet()) {
        if (Pattern.matches(entry.getKey(), key)) {
          entry.getValue().visit(value, context);
          return true;
        }
      }
      context.recordWarning(keyNode, "Unexpected property: \"" + key + "\"");
      return true; // non-blocking: the object still has the correct expected structure
    }
  }
}
