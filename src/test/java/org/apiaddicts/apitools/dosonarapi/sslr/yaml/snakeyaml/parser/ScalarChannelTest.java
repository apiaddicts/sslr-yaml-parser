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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static com.sonar.sslr.api.GenericTokenType.EOF;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.ScalarChannel.decodePlainScalar;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_END;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_ENTRY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_MAPPING_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.BLOCK_SEQUENCE_START;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.KEY;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.STRING;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens.VALUE;

public class ScalarChannelTest {

  private com.sonar.sslr.impl.Lexer lexer;

  @Before
  public void setUp() {
    lexer = Lexer.create(Charset.forName("UTF-8"));
  }

  @Test
  public void multiline_litteral() {
    URL resource = ScalarChannelTest.class.getResource("/newlexer/multiline-literal.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(tuple(STRING, 1, 1, "multi\n  line\n value\n", "| \n    multi\n      line\n     value\n"));
  }

  @Test
  public void multiline_folded() {
    URL resource = ScalarChannelTest.class.getResource("/newlexer/multiline-folded.yaml");

    List<Token> tokens = lexer.lex(resource);

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(tuple(STRING, 1, 1, "multi line value\n", ">\n multi\n line\n value\n"));
  }

  @Test
  public void multiline_plain() {
    List<Token> tokens = lexer
      .lex("multi\n \n\nline");
    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(tuple(STRING, 1, 0, "multi\n\nline", "multi\n \n\nline"));
  }

  @Test
  public void block_multiline_with_space() {
    List<Token> tokens = lexer
      .lex("  >\n" +
        "\n" +
        "    multi\n" +
        "     line");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(tuple(STRING, 1, 2, "\nmulti\n line", ">\n\n    multi\n     line"));
  }

  @Test
  public void block_with_chomping() {
    List<Token> tokens = lexer
      .lex("strip: |-\n" +
        "  true\n" +
        "clip: |\n" +
        "  text\n" +
        "keep: |+1\n" +
        "  text\u2028");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(
        tuple(BLOCK_MAPPING_START, 1, 0, "{", ""),
        tuple(KEY, 1, 0, "?", ""),
        tuple(STRING, 1, 0, "strip", "strip"),
        tuple(VALUE, 1, 5, ":", ":"),
        tuple(STRING, 1, 7, "true", "|-\n  true\n"),
        tuple(KEY, 3, 0, "?", ""),
        tuple(STRING, 3, 0, "clip", "clip"),
        tuple(VALUE, 3, 4, ":", ":"),
        tuple(STRING, 3, 6, "text\n", "|\n  text\n"),
        tuple(KEY, 5, 0, "?", ""),
        tuple(STRING, 5, 0, "keep", "keep"),
        tuple(VALUE, 5, 4, ":", ":"),
        tuple(STRING, 5, 6, " text\u2028", "|+1\n  text\u2028"),
        tuple(BLOCK_END, 6, 7, "}", ""),
        tuple(EOF, 6, 7, "EOF", "EOF"));
  }

  @Test
  public void block_with_indentation_indicator() {
    List<Token> tokens = lexer
      .lex("- |\n" +
        " detected\n" +
        "- >\n" +
        " \n" +
        "  \n" +
        "  # detected\n" +
        "- |1\n" +
        "  explicit\n" +
        "- >\n" +
        " \t\n" +
        " detected");

    assertThat(tokens).extracting("type", "line", "column", "value", "originalValue")
      .contains(
        tuple(BLOCK_SEQUENCE_START, 1, 0, "[", ""),
        tuple(BLOCK_ENTRY, 1, 0, "-", "-"),
        tuple(STRING, 1, 2, "detected\n", "|\n detected\n"),
        tuple(BLOCK_ENTRY, 3, 0, "-", "-"),
        tuple(STRING, 3, 2, "\n\n# detected\n", ">\n \n  \n  # detected\n"),
        tuple(BLOCK_ENTRY, 7, 0, "-", "-"),
        tuple(STRING, 7, 2, " explicit\n", "|1\n  explicit\n"),
        tuple(BLOCK_ENTRY, 9, 0, "-", "-"),
        tuple(STRING, 9, 2, "\t\ndetected", ">\n \t\n detected"),
        tuple(BLOCK_END, 11, 9, "}", ""),
        tuple(EOF, 11, 9, "EOF", "EOF"));
  }

  @Test
  public void double_quoted() {
    List<Token> tokens = lexer.lex("\"Some text \\\"in quotes\\\"\"");

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    assertEquals(STRING, token.getType());
    assertEquals("Some text \"in quotes\"", token.getValue());
    assertEquals("\"Some text \\\"in quotes\\\"\"", token.getOriginalValue());
    assertEquals(1, token.getLine());
    assertEquals(0, token.getColumn());
  }

  @Test
  public void double_quotes_escape() {
    List<Token> tokens = lexer.lex("\"Some \\_ \\x34\"");

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    int decimal = 0x34;
    String unicode = new String(Character.toChars(decimal));

    assertEquals("Some \u00A0 " + unicode, token.getValue());
    assertEquals("\"Some \\_ \\x34\"", token.getOriginalValue());
  }

  @Test
  public void double_quotes_swallow_breaks() {
    String original = "\"Some \r\n\t  \nquoted with breaks\"";
    List<Token> tokens = lexer.lex(original);

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    assertEquals("Some\nquoted with breaks", token.getValue());
    assertEquals("\"Some \n\t  \nquoted with breaks\"", token.getOriginalValue());
  }

  @Test
  public void single_quoted() {
    List<Token> tokens = lexer.lex("'Some text ''in quotes'''");

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    assertEquals(STRING, token.getType());
    assertEquals("Some text 'in quotes'", token.getValue());
    assertEquals("'Some text ''in quotes'''", token.getOriginalValue());
    assertEquals(1, token.getLine());
    assertEquals(0, token.getColumn());
  }

  @Test
  public void single_quotes_doesnt_escape() {
    List<Token> tokens = lexer.lex("'Some \\u00A0 \\x34'");

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    assertEquals("Some \\u00A0 \\x34", token.getValue());
    assertEquals("'Some \\u00A0 \\x34'", token.getOriginalValue());
  }

  @Test
  public void single_quotes_swallow_breaks() {
    String original = "'Some \r\n\t  \nquoted with breaks'";
    List<Token> tokens = lexer.lex(original);

    assertThat(tokens).hasSize(2); // for EOF
    Token token = tokens.get(0);
    assertEquals("Some\nquoted with breaks", token.getValue());
    assertEquals("'Some \n\t  \nquoted with breaks'", token.getOriginalValue());
  }

  @Test
  public void decodes_booleans() {
    assertEquals(Tokens.TRUE, decodePlainScalar("on"));
    assertEquals(Tokens.TRUE, decodePlainScalar("yes"));
    assertEquals(Tokens.TRUE, decodePlainScalar("y"));
    assertEquals(Tokens.TRUE, decodePlainScalar("Y"));
    assertEquals(Tokens.TRUE, decodePlainScalar("true"));
    assertEquals(Tokens.FALSE, decodePlainScalar("off"));
    assertEquals(Tokens.FALSE, decodePlainScalar("no"));
    assertEquals(Tokens.FALSE, decodePlainScalar("n"));
    assertEquals(Tokens.FALSE, decodePlainScalar("N"));
    assertEquals(Tokens.FALSE, decodePlainScalar("false"));
    assertEquals(Tokens.STRING, decodePlainScalar("onboard"));
    assertEquals(Tokens.STRING, decodePlainScalar("yes-card"));
    assertEquals(Tokens.STRING, decodePlainScalar("yank"));
    assertEquals(Tokens.STRING, decodePlainScalar("Yankee"));
    assertEquals(Tokens.STRING, decodePlainScalar("truelle"));
    assertEquals(Tokens.STRING, decodePlainScalar("offload"));
    assertEquals(Tokens.STRING, decodePlainScalar("note"));
    assertEquals(Tokens.STRING, decodePlainScalar("Note"));
    assertEquals(Tokens.STRING, decodePlainScalar("falsehood"));
  }

}
