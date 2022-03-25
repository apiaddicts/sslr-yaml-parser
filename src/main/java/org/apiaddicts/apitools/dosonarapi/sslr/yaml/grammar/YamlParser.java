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
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Rule;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.LexerException;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.RuleDefinition;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.SyntaxNode;
import org.sonar.sslr.internal.vm.CompilableGrammarRule;
import org.sonar.sslr.internal.vm.CompiledGrammar;
import org.sonar.sslr.internal.vm.Machine;
import org.sonar.sslr.internal.vm.MutableGrammarCompiler;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Lexer;

public class YamlParser {
  public static final GrammarValidator NULL_VALIDATOR = node -> Collections.emptyList();
  private final Charset charset;
  private final boolean strict;
  private final GrammarValidator validator;
  private final List<ValidationIssue> issues = new ArrayList<>();

  public YamlParser(Charset charset) {
    this(charset, null, false);
  }

  public YamlParser(Charset charset, RuleDefinition rootRule) {
    this(charset, rootRule, false);
  }

  public YamlParser(Charset charset, @Nullable RuleDefinition rootRule, boolean strict) {
    this.charset = charset;
    this.strict = strict;
    if (rootRule != null) {
      this.validator = new ASTValidator(rootRule);
    } else {
      this.validator = NULL_VALIDATOR;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public JsonNode parse(File file) {
    try {
      com.sonar.sslr.impl.Lexer lexer = Lexer.create(charset);
      return parseAndValidate(lexer.lex(file));
    } catch (LexerException e) {
      throw new RecognitionException(e);
    }
  }

  public JsonNode parse(String text) {
    try {
      com.sonar.sslr.impl.Lexer lexer = Lexer.create(charset);
      return parseAndValidate(lexer.lex(text));
    } catch (LexerException e) {
      throw new RecognitionException(e);
    }
  }

  public List<ValidationIssue> getIssues() {
    return Collections.unmodifiableList(issues);
  }

  private JsonNode parseAndValidate(List<Token> tokens) {
    JsonNode rootNode = parseAst(tokens);
    issues.addAll(validator.validate(rootNode));
    if (!strict || issues.isEmpty()) {
      // Add the EOF node to preserve comments on last line
      AstNode eof = rootNode.getNextSibling();
      rootNode.addChild(new SyntaxNode(eof.getType(), eof.getName(), eof.getToken()));
      return rootNode;
    } else {
      throw new ValidationException(rootNode, "Validation errors", issues.stream().map(ValidationException::toException).collect(Collectors.toList()));
    }
  }

  private JsonNode parseAst(List<Token> tokens) {
    Grammar grammar = YamlGrammar.create().build();
    Rule rootRule = grammar.getRootRule();
    CompiledGrammar g = MutableGrammarCompiler.compile((CompilableGrammarRule) rootRule);
    return (JsonNode) JsonAstCreator.create(Machine.parse(tokens, g), tokens).getFirstChild();
  }

  public static final class Builder {

    private Charset charset = Charset.defaultCharset();
    private RuleDefinition rootRule;
    private boolean strict = false;

    public Builder withCharset(Charset charset) {
      this.charset = charset;
      return this;
    }

    public Builder withStrictValidation(boolean validate) {
      this.strict = validate;
      return this;
    }

    public Builder withGrammar(YamlGrammarBuilder b) {
      this.rootRule = b.build();
      return this;
    }

    public Builder withGrammar(RuleDefinition rule) {
      this.rootRule = rule;
      return this;
    }

    public YamlParser build() {
      return new YamlParser(this.charset, rootRule, strict);
    }

  }
}
