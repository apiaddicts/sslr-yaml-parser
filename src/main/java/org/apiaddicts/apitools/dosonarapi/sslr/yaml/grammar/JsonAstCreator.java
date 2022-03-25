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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.impl.matcher.RuleDefinition;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.impl.SyntaxNode;
import org.sonar.sslr.internal.matchers.ParseNode;

public class JsonAstCreator {

  public static JsonNode create(ParseNode node, List<Token> tokens) {
    JsonNode astNode = new JsonAstCreator(tokens).visit(node);
    // Unwrap AstNodeType for root node:
    astNode.hasToBeSkippedFromAst();
    return astNode;
  }

  private final List<Token> tokens;

  private JsonAstCreator(List<Token> tokens) {
    this.tokens = tokens;
  }

  private JsonNode visit(ParseNode node) {
    if (node.getMatcher() instanceof RuleDefinition) {
      return visitNonTerminal(node);
    } else {
      return visitTerminal(node);
    }
  }

  private JsonNode visitNonTerminal(ParseNode node) {
    List<JsonNode> astNodes = new ArrayList<>();
    RuleDefinition ruleMatcher = (RuleDefinition) node.getMatcher();

    if (ruleMatcher.getRuleKey() != YamlGrammar.SCALAR) {
      for (ParseNode child : node.getChildren()) {
        JsonNode astNode = visit(child);
        if (astNode == null) {
          // skip
        } else if (astNode.hasToBeSkippedFromAst()) {
          astNodes.addAll(astNode.getJsonChildren());
        } else {
          astNodes.add(astNode);
        }
      }
    }

    Token token = node.getStartIndex() < tokens.size() ? tokens.get(node.getStartIndex()) : null;
    JsonNode astNode = createNonSyntaxNode(ruleMatcher, token);
    for (AstNode child : astNodes) {
      astNode.addChild(child);
    }
    astNode.setFromIndex(node.getStartIndex());
    astNode.setToIndex(node.getEndIndex());

    return astNode;
  }

  private JsonNode createNonSyntaxNode(RuleDefinition ruleMatcher, @Nullable Token token) {
    YamlGrammar ruleKey = (YamlGrammar)ruleMatcher.getRuleKey();
    Class<? extends JsonNode> nodeClass = ruleKey.getNodeClass();
    if (nodeClass == null) {
      return new SyntaxNode(ruleMatcher, ruleMatcher.getName(), token);
    }
    try {
      return nodeClass.getConstructor(AstNodeType.class, String.class, Token.class).newInstance(ruleMatcher, ruleMatcher.getName(), token);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalArgumentException("Node class " + nodeClass + " misses the expected constructor");
    }
  }

  private JsonNode visitTerminal(ParseNode node) {
    Token token = tokens.get(node.getStartIndex());
    TokenType type = token.getType();
    JsonNode astNode = new SyntaxNode(type, type.getName(), token);
    astNode.setFromIndex(node.getStartIndex());
    astNode.setToIndex(node.getEndIndex());
    return astNode;
  }

}
