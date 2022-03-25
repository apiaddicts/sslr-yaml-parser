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
package org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser;

import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_END;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_ENTRY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_MAPPING_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_SEQUENCE_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.FLOW_ENTRY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.FLOW_MAPPING_END;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.FLOW_MAPPING_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.FLOW_SEQUENCE_END;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.FLOW_SEQUENCE_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.KEY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.STRING;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.VALUE;

public class LexerTest {

  private com.sonar.sslr.impl.Lexer lexer;

  @Before
  public void setUp() {
    lexer = Lexer.create(Charset.forName("UTF-8"));
  }

  @Test
  public void comments() {
    URL resource = LexerTest.class.getResource("/newlexer/comments.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn, Token::getValue)
      .contains(
        tuple(STRING, 1, 11, "3.0.0"),
        tuple(STRING, 3, 5, "value"));
    List<Token> comments = tokensWithTrivia(tokens);

    assertThat(comments)
      .extracting(
        Token::getLine,
        Token::getColumn,
        Token::getValue)
      .containsExactly(
        tuple(2, 2, " some comment"),
        tuple(3, 11, " comment after value"),
        tuple(5, 2, ""),
        tuple(6, 2, " comment after block mapping start"));
  }

  @Test
  public void block_array_of_objects() {
    URL resource = LexerTest.class.getResource("/newlexer/block-array-objects.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn)
      .contains(
        tuple(BLOCK_SEQUENCE_START, 1, 0),
        tuple(BLOCK_ENTRY, 1, 0),
        tuple(BLOCK_MAPPING_START, 2, 2),
        tuple(KEY, 2, 2),
        tuple(VALUE, 2, 6),
        tuple(STRING, 2, 8),
        tuple(BLOCK_END, 3, 0),
        tuple(BLOCK_ENTRY, 3, 0),
        tuple(BLOCK_MAPPING_START, 4, 2),
        tuple(KEY, 4, 2),
        tuple(STRING, 4, 2),
        tuple(VALUE, 4, 6),
        tuple(STRING, 4, 8),
        tuple(BLOCK_END, 6, 0),
        tuple(BLOCK_END, 6, 0),
        tuple(EOF, 6, 0));
  }

  @Test
  public void flow_array_of_objects() {
    URL resource = LexerTest.class.getResource("/newlexer/flow-array-objects.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn)
      .contains(
        tuple(FLOW_SEQUENCE_START, 1, 0),
        tuple(FLOW_MAPPING_START, 2, 2),
        tuple(KEY, 2, 3),
        tuple(STRING, 2, 3),
        tuple(VALUE, 2, 7),
        tuple(STRING, 2, 9),
        tuple(FLOW_MAPPING_END, 2, 16),
        tuple(FLOW_ENTRY, 2, 17),
        tuple(FLOW_MAPPING_START, 3, 2),
        tuple(KEY, 3, 3),
        tuple(STRING, 3, 3),
        tuple(VALUE, 3, 7),
        tuple(STRING, 3, 9),
        tuple(FLOW_MAPPING_END, 4, 3),
        tuple(FLOW_SEQUENCE_END, 5, 0),
        tuple(EOF, 5, 1));
  }

  @Test
  public void nested_objects() {
    URL resource = LexerTest.class.getResource("/newlexer/nested-objects.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn)
      .contains(
        tuple(BLOCK_MAPPING_START, 1, 0),
        tuple(KEY, 1, 0),
        tuple(STRING, 1, 0),
        tuple(VALUE, 1, 4),
        tuple(BLOCK_MAPPING_START, 2, 2),
        tuple(KEY, 2, 2),
        tuple(STRING, 2, 2),
        tuple(VALUE, 2, 7),
        tuple(STRING, 2, 9),
        tuple(BLOCK_END, 3, 0),
        tuple(KEY, 3, 0),
        tuple(STRING, 3, 0),
        tuple(VALUE, 3, 4),
        tuple(BLOCK_MAPPING_START, 4, 2),
        tuple(KEY, 4, 2),
        tuple(STRING, 4, 2),
        tuple(VALUE, 4, 7),
        tuple(STRING, 4, 9),
        tuple(BLOCK_END, 5, 0),
        tuple(BLOCK_END, 5, 0),
        tuple(EOF, 5, 0));
  }

  @Test
  public void nested_array() {
    URL resource = LexerTest.class.getResource("/newlexer/nested-array.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn)
      .contains(
        tuple(BLOCK_MAPPING_START, 1, 0),
        tuple(KEY, 1, 0),
        tuple(STRING, 1, 0),
        tuple(VALUE, 1, 4),
        // First array, unindented -> no BLOCK_SEQUENCE_START
        tuple(BLOCK_ENTRY, 2, 0),
        tuple(BLOCK_MAPPING_START, 2, 2),
        tuple(KEY, 2, 2),
        tuple(STRING, 2, 2),
        tuple(VALUE, 2, 6),
        tuple(STRING, 2, 8),
        tuple(BLOCK_END, 3, 0),
        tuple(KEY, 3, 0),
        tuple(STRING, 3, 0),
        tuple(VALUE, 3, 4),
        // Second array, indented -> no BLOCK_SEQUENCE_ENTRY
        tuple(BLOCK_SEQUENCE_START, 4, 2),
        tuple(BLOCK_ENTRY, 4, 2),
        tuple(BLOCK_MAPPING_START, 4, 4),
        tuple(KEY, 4, 4),
        tuple(STRING, 4, 4),
        tuple(VALUE, 4, 8),
        tuple(STRING, 4, 10),
        tuple(BLOCK_END, 5, 0),
        tuple(BLOCK_END, 5, 0),
        tuple(BLOCK_END, 5, 0),
        tuple(EOF, 5, 0));
  }

  @Test
  public void nested_array_with_comments() {
    URL resource = LexerTest.class.getResource("/newlexer/nested-array-comments.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting(Token::getType, Token::getLine, Token::getColumn)
      .contains(
        tuple(BLOCK_MAPPING_START, 1, 0),
        tuple(KEY, 1, 0),
        tuple(STRING, 1, 0),
        tuple(VALUE, 1, 4),
        // First array, unindented -> no BLOCK_SEQUENCE_START
        tuple(BLOCK_ENTRY, 2, 0),
        tuple(BLOCK_MAPPING_START, 2, 2),
        tuple(KEY, 2, 2),
        tuple(STRING, 2, 2),
        tuple(VALUE, 2, 6),
        tuple(STRING, 2, 8),
        tuple(KEY, 3, 2),
        tuple(STRING, 3, 2),
        tuple(VALUE, 3, 6),
        tuple(STRING, 3, 8),
        tuple(BLOCK_END, 4, 0),
        tuple(BLOCK_ENTRY, 4, 0),
        tuple(BLOCK_MAPPING_START, 4, 2),
        tuple(KEY, 4, 2),
        tuple(STRING, 4, 2),
        tuple(VALUE, 4, 6),
        tuple(STRING, 4, 8),
        tuple(BLOCK_END, 5, 0),
        tuple(BLOCK_END, 5, 0),
        tuple(EOF, 5, 0));
    List<Token> comments = tokensWithTrivia(tokens);

    assertThat(comments)
      .extracting(
          Token::getLine,
          Token::getColumn,
          Token::getValue)
      .containsExactly(
        tuple(2, 19, " a comment"));

  }

  @Test
  public void explicit_simple_key() {
    List<Token> tokens = lexer
      .lex("? key\n: value");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .containsExactly(
        tuple(BLOCK_MAPPING_START, 1, 0, "{", ""),
        tuple(KEY, 1, 0, "?", "?"),
        tuple(STRING, 1, 2, "key", "key"),
        tuple(VALUE, 2, 0, ":", ":"),
        tuple(STRING, 2, 2, "value", "value"),
        tuple(BLOCK_END, 2, 7, "}", ""),
        tuple(EOF, 2, 7, "EOF", "EOF"));
  }

  @Test
  public void complex_key_as_list() {
    List<Token> tokens = lexer
      .lex("? - Detroit Tigers\n" +
        "  - Chicago cubs\n" +
        ":\n" +
        "  2001-07-23");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .containsExactly(
        tuple(BLOCK_MAPPING_START, 1, 0, "{", ""),
        tuple(KEY, 1, 0, "?", "?"),
        tuple(BLOCK_SEQUENCE_START, 1, 2, "[", ""),
        tuple(BLOCK_ENTRY, 1, 2, "-", "-"),
        tuple(STRING, 1, 4, "Detroit Tigers", "Detroit Tigers"),
        tuple(BLOCK_ENTRY, 2, 2, "-", "-"),
        tuple(STRING, 2, 4, "Chicago cubs", "Chicago cubs"),
        tuple(BLOCK_END, 3, 0, "}", ""),
        tuple(VALUE, 3, 0, ":", ":"),
        tuple(STRING, 4, 2, "2001-07-23", "2001-07-23"),
        tuple(BLOCK_END, 4, 12, "}", ""),
        tuple(EOF, 4, 12, "EOF", "EOF"));
  }

  @Test
  public void sets() {
    List<Token> tokens = lexer
      .lex("? one\n" +
        "? two\n" +
        "? three");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .containsExactly(
        tuple(BLOCK_MAPPING_START, 1, 0, "{", ""),
        tuple(KEY, 1, 0, "?", "?"),
        tuple(STRING, 1, 2, "one", "one"),
        tuple(KEY, 2, 0, "?", "?"),
        tuple(STRING, 2, 2, "two", "two"),
        tuple(KEY, 3, 0, "?", "?"),
        tuple(STRING, 3, 2, "three", "three"),
        tuple(BLOCK_END, 3, 7, "}", ""),
        tuple(EOF, 3, 7, "EOF", "EOF"));
  }

  @Test
  public void null_values() {
    List<Token> tokens = lexer
      .lex("{\n" +
        "Somekey:,\n" +
        "Other key:}");
    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .containsExactly(
        tuple(FLOW_MAPPING_START, 1, 0, "{", "{"),
        tuple(KEY, 2, 0, "?", ""),
        tuple(STRING, 2, 0, "Somekey", "Somekey"),
        tuple(VALUE, 2, 7, ":", ":"),
        tuple(FLOW_ENTRY, 2, 8, ",", ","),
        tuple(KEY, 3, 0, "?", ""),
        tuple(STRING, 3, 0, "Other key", "Other key"),
        tuple(VALUE, 3, 9, ":", ":"),
        tuple(FLOW_MAPPING_END, 3, 10, "}", "}"),
        tuple(EOF, 3, 11, "EOF", "EOF"));
  }

  private static List<Token> tokensWithTrivia(List<Token> tokens) {
    return tokens.stream().filter(t -> !t.getTrivia().isEmpty())
        .flatMap(t -> t.getTrivia().stream())
        .map(Trivia::getToken)
        .collect(Collectors.toList());
  }
}
