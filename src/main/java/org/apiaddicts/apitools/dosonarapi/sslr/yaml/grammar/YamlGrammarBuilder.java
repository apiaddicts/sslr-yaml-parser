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

import com.sonar.sslr.api.TokenType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.sonar.sslr.grammar.GrammarException;
import org.sonar.sslr.grammar.GrammarRuleKey;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.AlwaysTrueValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ArrayValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.BooleanValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.FirstOfValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.FloatValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.IntegerValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.NodeTypeValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.ObjectValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.PropertyDescriptionImpl;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.RuleDefinition;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.TokenTypeValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.TokenValueValidation;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar.SCALAR;

/**
 * A builder for creating <a href="http://en.wikipedia.org/wiki/Parsing_expression_grammar">Parsing Expression Grammars</a>
 * for YAML-based grammars. Use it in combination with a {@link YamlParser} to parse and validate YAML documents.
 * <p>
 * Objects of following types can be used as an atomic parsing expressions:
 * <ul>
 * <li>GrammarRuleKey</li>
 * <li>TokenType</li>
 * <li>String</li>
 * </ul>
 */
public class YamlGrammarBuilder {
  private final Map<GrammarRuleKey, RuleDefinition> definitions = new HashMap<>();
  private GrammarRuleKey rootRuleKey;

  /**
   * Matches an object whose properties match the provided {@link #property(String, Object)} sub-expressions.
   * During the execution of this expression on an object, parser will try to match every property with one of the
   * provided sub-expressions, ensuring that no property is defined in double, that all {@link #mandatoryProperty(String, Object)}
   * and {@link #discriminant(String, Object)} are supplied.
   *
   * @param first first sub-expression
   * @param second second sub-expression
   * @param rest rest of sub-expressions
   * @return the built rule
   */
  public ValidationRule object(PropertyDescription first, PropertyDescription second, PropertyDescription... rest) {
    ObjectValidation masterRule = new ObjectValidation();
    masterRule.addProperty(first);
    masterRule.addProperty(second);
    for (PropertyDescription otherRule : rest) {
      masterRule.addProperty(otherRule);
    }
    return masterRule;
  }

  /**
   * Matches an object. For more details, see {@link #object(PropertyDescription, PropertyDescription, PropertyDescription...)}.
   * @param rule the description of the properties to match
   * @return the built rule
   */
  public ValidationRule object(PropertyDescription rule) {
    ObjectValidation masterRule = new ObjectValidation();
    masterRule.addProperty(rule);
    return masterRule;
  }

  /**
   * Matches any YAML object. Equivalent for {@code object(patternProperty(".*", anything()))}.
   * @return the built rule
   */
  public ValidationRule anyObject() {
    ObjectValidation validation = new ObjectValidation();
    validation.addProperty(patternProperty(".*", new AlwaysTrueValidation()));
    return validation;
  }

  /**
   * Matches any valid YAML content. Equivalent for {@code firstOf(scalar(), anyArray(), anyObject())}.
   * @return the built rule
   */
  public ValidationRule anything() {
    return firstOf(
        SCALAR,
        anyArray(),
        anyObject()
    );
  }

  /**
   * Creates a parsing expression - "array".
   * During the execution of this expression, parser will apply the sub-expression to all entries of the array. This
   * expression succeeds only if the sub-expression succeeds on all entries of the array.
   *
   * @param rule the sub-expression
   * @return the built rule
   */
  public ValidationRule array(Object rule) {
    return new ArrayValidation(convertToRule(rule));
  }

  /**
   * Creates a parsing expression - "any array".
   * This expression matches any array, whatever the type of objects or the number of entries. Equivalent of
   * {@code array(anything())}.
   * @return the built rule
   */
  public ValidationRule anyArray() {
    return new ArrayValidation(new AlwaysTrueValidation());
  }

  /**
   * Describes an optional property that can appear in an {@link #object(PropertyDescription)}.
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  public PropertyDescription property(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, false, false, convertToRule(rule));
  }

  /**
   * Describes a mandatory property that can appear in an {@link #object(PropertyDescription)}.
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  public PropertyDescription mandatoryProperty(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, true, false, convertToRule(rule));
  }

  /**
   * Describes a rule to match properties by name in an {@link #object(PropertyDescription)}. When an object's property
   * key doesn't match a named property, it is evaluated against any declared pattern property, in the order in which
   * they have been declared (so order is important).
   *
   * Be aware that in {@code object(patternProperty(".*", anything()), patternProperty("^x-.*", anything())}, the second sub-expression will
   * never be executed.
   *
   * @param pattern the pattern to match against the property names
   * @param rule the sub-expression
   * @return the built rule
   */
  public PropertyDescription patternProperty(String pattern, Object rule) {
    return new PropertyDescriptionImpl(pattern, true, false, false, convertToRule(rule));
  }

  /**
   * Describes a mandatory property that can appear in an {@link #object(PropertyDescription)}, and whose value is a
   * discriminant to identify the type of objects.
   * The object will be recognised only if the field exists and the validation rule is matched.
   *
   * @param key the key of the property
   * @param rule the type of the property (can be any valid rule)
   * @return the built rule
   */
  public PropertyDescription discriminant(String key, Object rule) {
    return new PropertyDescriptionImpl(key, false, true, true, convertToRule(rule));
  }

  /**
   * Creates parsing expression - "first of".
   * During the execution of this expression parser execute sub-expressions in order until one succeeds.
   * This expressions succeeds if any sub-expression succeeds.
   * <p>
   * Be aware that in expression {@code firstOf("foo", firstOf("foo", "bar"))} second sub-expression will never be executed.
   *
   * @param first  first sub-expression
   * @param second  second sub-expression
   * @throws IllegalArgumentException if any of given arguments is not a parsing expression
   * @return the built rule
   */
  public ValidationRule firstOf(Object first, Object second) {
    return new FirstOfValidation(convertToRule(first), convertToRule(second));
  }

  /**
   * Creates parsing expression - "first of".
   * See {@link #firstOf(Object, Object)} for more details.
   *
   * @param first  first sub-expression
   * @param second  second sub-expression
   * @param rest  rest of sub-expressions
   * @throws IllegalArgumentException if any of given arguments is not a parsing expression
   * @return the built rule
   */
  public ValidationRule firstOf(Object first, Object second, Object... rest) {
    return new FirstOfValidation(convertToRules(first, second, rest));
  }

  /**
   * Allows to describe rule.
   * Result of this method should be used only for execution of methods in it, i.e. you should not save reference on it.
   * No guarantee that this method always returns the same instance for the same key of rule.
   * @param ruleKey the key that will be used to refer to this rule in other rules
   * @return the builder, for continuation
   */
  public GrammarRuleBuilder rule(GrammarRuleKey ruleKey) {
    RuleDefinition rule = definitions.computeIfAbsent(ruleKey, RuleDefinition::new);
    return new RuleBuilder(this, rule);
  }

  /**
   * Allows to specify that given rule should be root for grammar.
   * @param rootRuleKey a key that needs to be declared with {@link #rule(GrammarRuleKey)} before building
   */
  public void setRootRule(GrammarRuleKey rootRuleKey) {
    this.rootRuleKey = rootRuleKey;
  }

  /**
   * Matches any scalar.
   * @return the built rule
   */
  public ValidationRule scalar() {
    return new NodeTypeValidation(YamlGrammar.SCALAR);
  }

  /**
   * Matches any string scalar.
   * @return the built rule
   */
  public ValidationRule string() {
    return new TokenTypeValidation(Tokens.STRING, Tokens.NULL);
  }

  /**
   * Matches an integer scalar.
   * @return the built rule
   */
  public Object integer() {
    return new IntegerValidation();
  }

  /**
   * Matches a boolean scalar.
   * @return the built rule
   */
  public Object bool() {
    return new BooleanValidation(null);
  }

  /**
   * Matches a boolean scalar with a specific value.
   * @param value the exact boolean to match
   * @return the built rule
   */
  public Object bool(boolean value) {
    return new BooleanValidation(value);
  }

  /**
   * Matches a floating-point scalar.
   * @return the built rule
   */
  public Object floating() {
    return new FloatValidation();
  }

  public RuleDefinition build() {
    return definitions.get(rootRuleKey);
  }

  private ValidationRule convertToRule(Object e) {
    Objects.requireNonNull(e, "Validation rule can't be null");
    final ValidationRule result;
    if (e instanceof ValidationRule) {
      result = (ValidationRule) e;
    } else if (e instanceof YamlGrammar) {
      result = new NodeTypeValidation((YamlGrammar)e);
    } else if (e instanceof GrammarRuleKey) {
      GrammarRuleKey ruleKey = (GrammarRuleKey) e;
      rule(ruleKey);
      result = definitions.get(ruleKey);
    } else if (e instanceof TokenType) {
      result = new TokenTypeValidation((TokenType) e);
    } else if (e instanceof String) {
      result = new TokenValueValidation((String) e);
    } else {
      throw new IllegalArgumentException("Incorrect type of validation rule: " + e.getClass().toString());
    }
    return result;
  }

  private ValidationRule[] convertToRules(Object e1, Object e2, Object[] rest) {
    ValidationRule[] result = new ValidationRule[2 + rest.length];
    result[0] = convertToRule(e1);
    result[1] = convertToRule(e2);
    for (int i = 0; i < rest.length; i++) {
      result[2 + i] = convertToRule(rest[i]);
    }
    return result;
  }

  private static class RuleBuilder implements GrammarRuleBuilder {

    private final YamlGrammarBuilder b;
    private final RuleDefinition delegate;

    public RuleBuilder(YamlGrammarBuilder b, RuleDefinition delegate) {
      this.b = b;
      this.delegate = delegate;
    }

    @Override
    public GrammarRuleBuilder is(Object e) {
      if (delegate.getValidation() != null) {
        throw new GrammarException("The rule '" + delegate.getRuleKey() + "' has already been defined somewhere in the grammar.");
      }
      delegate.setValidation(b.convertToRule(e));
      return this;
    }

    @Override
    public void skip() {
      delegate.skip();
    }
  }
}
