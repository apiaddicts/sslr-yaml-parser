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

import com.google.common.annotations.VisibleForTesting;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeBuffer;
import org.sonar.sslr.channel.CodeReader;

import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.LineBreakChannel.scanLineBreak;
import static org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Lexer.NULL_BL_T_LINEBR_S;

class ScalarChannel extends Channel<com.sonar.sslr.impl.Lexer> {

  private static final String SPECIAL = NULL_BL_T_LINEBR_S + "-?:,[]{}#&*!|>\'\"%@`";
  private static final Pattern PATTERN_FLOAT = Pattern.compile("[-+]?([0-9][0-9_]*)?\\.[0-9]*([eE][-+][0-9]+)?");

  /**
   * A regular expression matching characters which are not in the hexadecimal
   * set (0-9, A-F, a-f).
   */
  private static final Pattern NOT_HEXA = Pattern.compile("[^0-9A-Fa-f]");

  /**
   * A mapping from an escaped character in the input stream to the character
   * that they should be replaced with.
   *
   * YAML defines several common and a few uncommon escape sequences.
   *
   * @see <a href="http://www.yaml.org/spec/current.html#id2517668">4.1.6.
   *      Escape Sequences</a>
   */
  private static final Map<Character, String> ESCAPE_REPLACEMENTS = new HashMap<>();

  /**
   * A mapping from a character to a number of bytes to read-ahead for that
   * escape sequence. These escape sequences are used to handle unicode
   * escaping in the following formats, where H is a hexadecimal character:
   *
   * <pre>
   * &#92;xHH         : escaped 8-bit Unicode character
   * &#92;uHHHH       : escaped 16-bit Unicode character
   * &#92;UHHHHHHHH   : escaped 32-bit Unicode character
   * </pre>
   *
   * @see <a href="http://yaml.org/spec/1.1/current.html#id872840">5.6. Escape
   *      Sequences</a>
   */
  private static final Map<Character, Integer> ESCAPE_CODES = new HashMap<>();
  private static final String WHILE_SCANNING_BLOCK_SCALAR = "while scanning a block scalar";

  static {
    // ASCII null
    ESCAPE_REPLACEMENTS.put('0', "\0");
    // ASCII bell
    ESCAPE_REPLACEMENTS.put('a', "\u0007");
    // ASCII backspace
    ESCAPE_REPLACEMENTS.put('b', "\u0008");
    // ASCII horizontal tab
    ESCAPE_REPLACEMENTS.put('t', "\u0009");
    // ASCII newline (line feed; &#92;n maps to 0x0A)
    ESCAPE_REPLACEMENTS.put('n', "\n");
    // ASCII vertical tab
    ESCAPE_REPLACEMENTS.put('v', "\u000B");
    // ASCII form-feed
    ESCAPE_REPLACEMENTS.put('f', "\u000C");
    // carriage-return (&#92;r maps to 0x0D)
    ESCAPE_REPLACEMENTS.put('r', "\r");
    // ASCII escape character (Esc)
    ESCAPE_REPLACEMENTS.put('e', "\u001B");
    // ASCII space
    ESCAPE_REPLACEMENTS.put(' ', "\u0020");
    // ASCII double-quote
    ESCAPE_REPLACEMENTS.put('"', "\"");
    // ASCII backslash
    ESCAPE_REPLACEMENTS.put('\\', "\\");
    // Unicode next line
    ESCAPE_REPLACEMENTS.put('N', "\u0085");
    // Unicode non-breaking-space
    ESCAPE_REPLACEMENTS.put('_', "\u00A0");
    // Unicode line-separator
    ESCAPE_REPLACEMENTS.put('L', "\u2028");
    // Unicode paragraph separator
    ESCAPE_REPLACEMENTS.put('P', "\u2029");

    // 8-bit Unicode
    ESCAPE_CODES.put('x', 2);
    // 16-bit Unicode
    ESCAPE_CODES.put('u', 4);
    // 32-bit Unicode (Supplementary characters are supported)
    ESCAPE_CODES.put('U', 8);
  }

  private final LexerState state;
  private final Token.Builder tokenBuilder = Token.builder();

  ScalarChannel(LexerState state) {
    this.state = state;
  }

  @Override
  public boolean consume(CodeReader code, com.sonar.sslr.impl.Lexer output) {
    tokenBuilder.setURI(output.getURI());
    char c = code.charAt(0);
    if (state.flowLevel() == 0 && (c == '|' || c == '>')) {
      fetchBlockScalar(c, code, output);
      return true;
    } else if (c == '\'' || c == '"') {
      fetchFlowScalar(c, code, output);
      return true;
    } else if (checkPlain(code)) {
      fetchPlain(code, output);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Fetch a flow scalar (single- or double-quoted).
   *
   * @see http://www.yaml.org/spec/1.1/#id863975
   */
  private void fetchFlowScalar(char style, CodeReader code, com.sonar.sslr.impl.Lexer output) {
    // A flow scalar could be a simple key.
    state.savePossibleSimpleKey(code, output);

    // No simple keys after flow scalars.
    state.allowSimpleKey(false);

    // Scan and add SCALAR.
    boolean isDoubleQuote;
    // The style will be either single- or double-quoted; we determine this
    // by the first character in the entry (supplied)
    isDoubleQuote = style == '"';
    StringBuilder chunks = new StringBuilder();
    StringBuilder originalChunks = new StringBuilder();
    CodeBuffer.Cursor startMark = code.getCursor().clone();
    char quote = code.charAt(0);
    code.pop(originalChunks);
    chunks.append(scanFlowScalarNonSpaces(code, isDoubleQuote, startMark, originalChunks));
    while (code.charAt(0) != quote) {
      chunks.append(scanFlowScalarSpaces(code, startMark, originalChunks));
      chunks.append(scanFlowScalarNonSpaces(code, isDoubleQuote, startMark, originalChunks));
    }
    code.pop(originalChunks);
    Token token = tokenBuilder
      .setType(Tokens.STRING)
      .setValueAndOriginalValue(chunks.toString(), originalChunks.toString())
      .setLine(startMark.getLine())
      .setColumn(startMark.getColumn())
      .build();
    output.addToken(token);
  }

  /**
   * Scan some number of flow-scalar non-space characters.
   */
  private String scanFlowScalarNonSpaces(CodeReader reader, boolean doubleQuoted, CodeReader.Cursor startMark, StringBuilder originalChunks) {
    // See the specification for details.
    StringBuilder chunks = new StringBuilder();
    while (true) {
      // Scan through any number of characters which are not: NUL, blank,
      // tabs, line breaks, single-quotes, double-quotes, or backslashes.
      String forbidden = NULL_BL_T_LINEBR_S + "\'\"\\";
      StringBuilder chunk = new StringBuilder();
      while (forbidden.indexOf(reader.charAt(0)) == -1) {
        reader.pop(chunk);
      }
      chunks.append(chunk.toString());
      originalChunks.append(chunk.toString());
      // Depending on our quoting-type, the characters ', " and \ have
      // differing meanings.
      char ch = reader.charAt(0);
      if (isEscapedSingleQuote(reader, doubleQuoted, ch)) {
        chunks.append("'");
        reader.pop(originalChunks);
        reader.pop(originalChunks);
      } else if (isRegularQuote(doubleQuoted, ch)) {
        chunks.append(ch);
        reader.pop(originalChunks);
      } else if (isEscapeChar(doubleQuoted, ch)) {
        reader.pop(originalChunks);
        chunks.append(scanFlowScalarEscapeChar(reader, startMark, originalChunks));
      } else {
        return chunks.toString();
      }
    }
  }

  private static boolean isRegularQuote(boolean doubleQuoted, char ch) {
    return (doubleQuoted && ch == '\'') || (!doubleQuoted && "\"\\".indexOf(ch) != -1);
  }

  private static boolean isEscapeChar(boolean doubleQuoted, char ch) {
    return doubleQuoted && ch == '\\';
  }

  private static boolean isEscapedSingleQuote(CodeReader reader, boolean doubleQuoted, char ch) {
    return !doubleQuoted && ch == '\'' && reader.charAt(1) == '\'';
  }

  private String scanFlowScalarEscapeChar(CodeReader reader, CodeBuffer.Cursor startMark, StringBuilder originalChunks) {
    char ch = reader.charAt(0);
    int length;
    if (ESCAPE_REPLACEMENTS.containsKey(ch)) {
      // The character is one of the single-replacement
      // types; these are replaced with a literal character
      // from the mapping.
      reader.pop(originalChunks);
      return ESCAPE_REPLACEMENTS.get(ch);
    } else if (ESCAPE_CODES.containsKey(ch)) {
      // The character is a multi-digit escape sequence, with
      // length defined by the value in the ESCAPE_CODES map.
      length = ESCAPE_CODES.get(ch);
      reader.pop(originalChunks);
      String hex = new String(reader.peek(length));
      if (NOT_HEXA.matcher(hex).find()) {
        throw new YamlLexerException("while scanning a double-quoted scalar",
          startMark, "expected escape sequence of " + length
            + " hexadecimal numbers, but found: " + hex,
          reader.getCursor());
      }
      int decimal = Integer.parseInt(hex, 16);
      String unicode = new String(Character.toChars(decimal));
      originalChunks.append(forward(reader, length));
      return unicode;
    } else if (scanLineBreak(reader).length() != 0) {
      originalChunks.append('\n');
      return scanFlowScalarBreaks(reader, startMark, originalChunks);
    } else {
      throw new YamlLexerException("while scanning a double-quoted scalar", startMark,
        "found unknown escape character " + ch + "(" + ((int) ch) + ")",
        reader.getCursor());
    }
  }

  private String scanFlowScalarSpaces(CodeReader reader, CodeReader.Cursor startMark, StringBuilder originalChunks) {
    // See the specification for details.
    StringBuilder chunks = new StringBuilder();
    // Scan through any number of whitespace (space, tab) characters,
    // consuming them.
    StringBuilder b = new StringBuilder();
    while (" \t".indexOf(reader.charAt(0)) != -1) {
      reader.pop(b);
    }
    String whitespaces = b.toString();
    originalChunks.append(whitespaces);
    char ch = reader.charAt(0);
    if (ch == '\0') {
      // A flow scalar cannot end with an end-of-stream
      throw new YamlLexerException("while scanning a quoted scalar", startMark,
        "found unexpected end of stream", reader.getCursor());
    }
    // If we encounter a line break, scan it into our assembled string...
    String lineBreak = scanLineBreak(reader);
    originalChunks.append(lineBreak);
    if (lineBreak.length() != 0) {
      String breaks = scanFlowScalarBreaks(reader, startMark, originalChunks);
      if (!"\n".equals(lineBreak)) {
        chunks.append(lineBreak);
      } else if (breaks.length() == 0) {
        chunks.append(" ");
      }
      chunks.append(breaks);
    } else {
      chunks.append(whitespaces);
    }
    return chunks.toString();
  }

  private String scanFlowScalarBreaks(CodeReader reader, CodeReader.Cursor startMark, StringBuilder originalChunks) {
    // See the specification for details.
    StringBuilder chunks = new StringBuilder();
    while (true) {
      // Instead of checking indentation, we check for document
      // separators.
      String prefix = new String(reader.peek(3));
      if (("---".equals(prefix) || "...".equals(prefix))
        && NULL_BL_T_LINEBR_S.indexOf(reader.charAt(3)) != -1) {
        throw new YamlLexerException("while scanning a quoted scalar", startMark,
          "found unexpected document separator", reader.getCursor());
      }
      // Scan past any number of spaces and tabs, ignoring them
      while (" \t".indexOf(reader.charAt(0)) != -1) {
        reader.pop(originalChunks);
      }
      // If we stopped at a line break, add that; otherwise, return the
      // assembled set of scalar breaks.
      String lineBreak = scanLineBreak(reader);
      if (lineBreak.length() != 0) {
        chunks.append(lineBreak);
        originalChunks.append(lineBreak);
      } else {
        return chunks.toString();
      }
    }
  }

  private static String forward(CodeReader reader, int count) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < count; ++i) {
      reader.pop(b);
    }
    return b.toString();
  }

  /**
   * Fetch a plain scalar.
   */
  private void fetchPlain(CodeReader code, com.sonar.sslr.impl.Lexer output) {
    // A plain scalar could be a simple key.
    state.savePossibleSimpleKey(code, output);

    // No simple keys after plain scalars. But note that `scan_plain` will
    // change this flag if the scan is finished at the beginning of the
    // line.
    state.allowSimpleKey(false);

    // Scan and add SCALAR. May change `allow_simple_key`.
    StringBuilder chunks = new StringBuilder();
    StringBuilder originalChunks = new StringBuilder();
    CodeBuffer.Cursor startMark = code.getCursor().clone();
    int indent = state.indent() + 1;
    String[] spaces = {"", ""};
    while (true) {
      // A comment indicates the end of the scalar.
      if (code.charAt(0) == '#') {
        break;
      }
      int length = skipPlainChars(code);
      // It's not clear what we should do with ':' in the flow context.
      if (state.flowLevel() != 0 && hasUnexpectedColonAt(code, length)) {
        originalChunks.append(forward(code, length));
        throw new YamlLexerException("while scanning a plain scalar", startMark,
          "found unexpected ':'", code.getCursor());
      }
      if (length == 0) {
        break;
      }
      state.allowSimpleKey(false);
      chunks.append(spaces[0]);
      originalChunks.append(spaces[1]);
      String chunk = forward(code, length);
      chunks.append(chunk);
      originalChunks.append(chunk);
      spaces = scanPlainSpaces(code);
      if (spaces[0].length() == 0 || code.charAt(0) == '#'
        || (state.flowLevel() == 0 && code.getColumnPosition() < indent)) {
        break;
      }
    }
    String value = chunks.toString();
    TokenType type = decodePlainScalar(value);
    Token token = tokenBuilder
      .setType(type)
      .setValueAndOriginalValue(value, originalChunks.toString())
      .setLine(startMark.getLine())
      .setColumn(startMark.getColumn())
      .build();
    output.addToken(token);
  }

  private static boolean hasUnexpectedColonAt(CodeReader code, int length) {
    return code.charAt(length) == ':'
      && (",[]{}" + NULL_BL_T_LINEBR_S).indexOf(code.charAt(length + 1)) == -1;
  }

  private int skipPlainChars(CodeReader code) {
    int length = 0;
    while (true) {
      char ch2 = code.charAt(length);
      if (NULL_BL_T_LINEBR_S.indexOf(ch2) != -1
        || (state.flowLevel() == 0 && ch2 == ':' && NULL_BL_T_LINEBR_S
          .indexOf(code.charAt(length + 1)) != -1)
        || (state.flowLevel() != 0 && ",:?[]{}".indexOf(ch2) != -1)) {
        break;
      }
      length++;
    }
    return length;
  }

  /**
   * See the specification for details. SnakeYAML and libyaml allow tabs
   * inside plain scalar
   */
  private String[] scanPlainSpaces(CodeReader reader) {
    StringBuilder b = new StringBuilder();
    StringBuilder original = new StringBuilder();
    while (reader.charAt(0) == ' ' || reader.charAt(0) == '\t') {
      reader.pop(b);
    }
    String whitespaces = b.toString();
    original.append(whitespaces);
    String lineBreak = scanLineBreak(reader);
    original.append(lineBreak);
    if (lineBreak.length() != 0) {
      state.allowSimpleKey(true);
      String prefix = new String(reader.peek(3));
      if (isStreamDelimiter(reader, prefix)) {
        return new String[] {"", original.toString()};
      }
      StringBuilder breaks = new StringBuilder();
      while (true) {
        if (reader.charAt(0) == ' ') {
          reader.pop(original);
        } else {
          String lb = scanLineBreak(reader);
          if (lb.length() != 0) {
            breaks.append(lb);
            original.append(lb);
            prefix = new String(reader.peek(3));
            if (isStreamDelimiter(reader, prefix)) {
              return new String[] {"", original.toString()};
            }
          } else {
            break;
          }
        }
      }
      if (!"\n".equals(lineBreak)) {
        return new String[] {lineBreak + breaks, original.toString()};
      } else if (breaks.length() == 0) {
        return new String[] {" ", original.toString()};
      }
      return new String[] {breaks.toString(), original.toString()};
    }
    return new String[] {whitespaces, original.toString()};
  }

  private static boolean isStreamDelimiter(CodeReader reader, String prefix) {
    return "---".equals(prefix) || "...".equals(prefix)
      && NULL_BL_T_LINEBR_S.indexOf(reader.charAt(3)) != -1;
  }

  /**
   * Returns true if the next thing on the reader is a plain token.
   */
  private boolean checkPlain(CodeReader reader) {
    /*
     * <pre>
     * A plain scalar may start with any non-space character except:
     * '-', '?', ':', ',', '[', ']', '{', '}',
     * '#', '&amp;', '*', '!', '|', '&gt;', '\'', '\&quot;',
     * '%', '@', '`'.
     *
     * It may also start with
     * '-', '?', ':'
     * if it is followed by a non-space character.
     *
     * Note that we limit the last rule to the block context (except the
     * '-' character) because we want the flow context to be space
     * independent.
     * </pre>
     */
    char ch = reader.charAt(0);
    char next = reader.charAt(1);
    // If the next char is NOT one of the forbidden chars above or
    // whitespace, then this is the start of a plain scalar.
    return SPECIAL.indexOf(ch) == -1
      || (NULL_BL_T_LINEBR_S.indexOf(next) == -1 && (ch == '-' || (state.flowLevel() == 0 && "?:".indexOf(ch) != -1)));
  }


  @VisibleForTesting
  static TokenType decodePlainScalar(String value) {
    int len = value.length();
    if (len == 0) {
      return Tokens.STRING;
    }
    char c = value.charAt(0);
    if ("+-.0123456789".indexOf(c) != -1) {
      TokenType t = decodeNumberScalar(value, len);
      if (t != null) {
        return t;
      }
      return Tokens.STRING;
    }
    if ("null".equals(value)) {
      return Tokens.NULL;
    }
    TokenType tokenType = matchYAMLBoolean(value, len);
    if (tokenType != null) {
      return tokenType;
    }
    return Tokens.STRING;
  }

  private static TokenType matchYAMLBoolean(String value, int len) {
    switch (len) {
      case 1:
        switch (value.charAt(0)) {
          case 'N':
          case 'n':
            return Tokens.FALSE;
          case 'Y':
          case 'y':
            return Tokens.TRUE;
          default:
            return null;
        }
      case 2:
        if ("no".equalsIgnoreCase(value)) {
          return Tokens.FALSE;
        }

        if ("on".equalsIgnoreCase(value)) {
          return Tokens.TRUE;
        }
        break;
      case 3:
        if ("yes".equalsIgnoreCase(value)) {
          return Tokens.TRUE;
        }

        if ("off".equalsIgnoreCase(value)) {
          return Tokens.FALSE;
        }
        break;
      case 4:
        if ("true".equalsIgnoreCase(value)) {
          return Tokens.TRUE;
        }
        break;
      case 5:
        if ("false".equalsIgnoreCase(value)) {
          return Tokens.FALSE;
        }
        break;
      default:
        return null;
    }

    return null;
  }

  private static TokenType decodeNumberScalar(String value, int len) {
    if ("0".equals(value)) {
      return Tokens.INTEGER;
    } else {
      int i;
      if (value.charAt(0) == '-') {
        i = 1;
        if (len == 1) {
          return null;
        }
      } else {
        i = 0;
      }

      do {
        int c = value.charAt(i);
        if (c > '9' || c < '0') {
          if (PATTERN_FLOAT.matcher(value).matches()) {
            return Tokens.FLOAT;
          } else {
            return null;
          }
        }

        ++i;
      } while (i != len);

      return Tokens.INTEGER;
    }
  }

  /**
   * Fetch a block scalar (literal or folded).
   *
   * @see http://www.yaml.org/spec/1.1/#id863975
   */
  private void fetchBlockScalar(char style, CodeReader code, com.sonar.sslr.impl.Lexer output) {
    // A simple key may follow a block scalar.
    state.allowSimpleKey(true);

    // Reset possible simple key on the current level.
    state.removePossibleSimpleKey(code);

    // Scan and add SCALAR.
    // Depending on the given style, we determine whether the scalar is
    // folded ('>') or literal ('|')
    boolean folded = style == '>';
    StringBuilder chunks = new StringBuilder();
    StringBuilder originalChunks = new StringBuilder();
    CodeReader.Cursor startMark = code.getCursor().clone();
    // Scan the header
    code.pop(originalChunks);
    Chomping chompi = scanBlockScalarIndicators(code, startMark, originalChunks);
    int increment = chompi.getIncrement();
    scanBlockScalarIgnoredLine(code, output, startMark, originalChunks);

    // Determine the indentation level and go to the first non-empty line.
    int minIndent = state.indent() + 1;
    if (minIndent < 1) {
      minIndent = 1;
    }
    String breaks;
    int maxIndent;
    int indent;
    if (increment == -1) {
      Object[] brme = scanBlockScalarIndentation(code, originalChunks);
      breaks = (String) brme[0];
      maxIndent = (Integer) brme[1];
      indent = Math.max(minIndent, maxIndent);
    } else {
      indent = minIndent + increment - 1;
      Object[] brme = scanBlockScalarBreaks(code, indent, originalChunks);
      breaks = (String) brme[0];
    }

    String lineBreak = "";

    // Scan the inner part of the block scalar.
    while (code.getColumnPosition() == indent && code.charAt(0) != '\0') {
      chunks.append(breaks);
      boolean leadingNonSpace = " \t".indexOf(code.charAt(0)) == -1;
      StringBuilder innerChunk = new StringBuilder();
      while (Lexer.NULL_OR_LINEBR_S.indexOf(code.charAt(0)) == -1) {
        code.pop(innerChunk);
      }
      chunks.append(innerChunk.toString());
      originalChunks.append(innerChunk.toString());
      lineBreak = scanLineBreak(code);
      originalChunks.append(lineBreak);
      Object[] brme = scanBlockScalarBreaks(code, indent, originalChunks);
      breaks = (String) brme[0];
      if (code.getColumnPosition() == indent && code.charAt(0) != '\0') {

        // Unfortunately, folding rules are ambiguous.
        //
        // This is the folding according to the specification:
        if (folded && "\n".equals(lineBreak) && leadingNonSpace
          && " \t".indexOf(code.charAt(0)) == -1) {
          if (breaks.length() == 0) {
            chunks.append(" ");
          }
        } else {
          chunks.append(lineBreak);
        }
        // Clark Evans's interpretation (also in the spec examples) not
        // imported from PyYAML
      } else {
        break;
      }
    }
    // Chomp the tail.
    if (chompi.chompTailIsNotFalse()) {
      chunks.append(lineBreak);
    }
    if (chompi.chompTailIsTrue()) {
      chunks.append(breaks);
    }
    // We are done.
    Token token = tokenBuilder
      .setType(Tokens.STRING)
      .setValueAndOriginalValue(chunks.toString(), originalChunks.toString())
      .setLine(startMark.getLine())
      .setColumn(startMark.getColumn())
      .build();
    output.addToken(token);
  }

  /**
   * Scan a block scalar indicator. The block scalar indicator includes two
   * optional components, which may appear in either order.
   *
   * A block indentation indicator is a non-zero digit describing the
   * indentation level of the block scalar to follow. This indentation is an
   * additional number of spaces relative to the current indentation level.
   *
   * A block chomping indicator is a + or -, selecting the chomping mode away
   * from the default (clip) to either -(strip) or +(keep).
   *
   * @see http://www.yaml.org/spec/1.1/#id868988
   * @see http://www.yaml.org/spec/1.1/#id927035
   * @see http://www.yaml.org/spec/1.1/#id927557
   */
  private Chomping scanBlockScalarIndicators(CodeReader reader, CodeReader.Cursor startMark, StringBuilder originalChunks) {
    // See the specification for details.
    Boolean chomping = null;
    int increment = -1;
    char ch = reader.charAt(0);
    if (ch == '-' || ch == '+') {
      if (ch == '+') {
        chomping = Boolean.TRUE;
      } else {
        chomping = Boolean.FALSE;
      }
      reader.pop(originalChunks);
      ch = reader.charAt(0);
      if (Character.isDigit(ch)) {
        increment = Integer.parseInt(String.valueOf(ch));
        if (increment == 0) {
          throw makeScalarIndentException(reader, startMark);
        }
        reader.pop(originalChunks);
      }
    } else if (Character.isDigit(ch)) {
      increment = Integer.parseInt(String.valueOf(ch));
      if (increment == 0) {
        throw makeScalarIndentException(reader, startMark);
      }
      reader.pop(originalChunks);
      ch = reader.charAt(0);
      if (ch == '-' || ch == '+') {
        if (ch == '+') {
          chomping = Boolean.TRUE;
        } else {
          chomping = Boolean.FALSE;
        }
        reader.pop(originalChunks);
      }
    }
    ch = reader.charAt(0);
    if (Lexer.NULL_BL_LINEBR_S.indexOf(ch) == -1) {
      throw new YamlLexerException(WHILE_SCANNING_BLOCK_SCALAR, startMark, "expected chomping or indentation indicator but found " + ch,
        reader.getCursor());
    }
    return new Chomping(chomping, increment);
  }

  private static YamlLexerException makeScalarIndentException(CodeReader reader, CodeBuffer.Cursor startMark) {
    return new YamlLexerException(WHILE_SCANNING_BLOCK_SCALAR, startMark,
      "expected indentation indicator in the range 1-9, but found 0",
      reader.getCursor());
  }

  /**
   * Scan to the end of the line after a block scalar has been scanned; the
   * only things that are permitted at this time are comments and spaces.
   */
  private void scanBlockScalarIgnoredLine(CodeReader reader, com.sonar.sslr.impl.Lexer output, CodeReader.Cursor startMark, StringBuilder originalChunks) {
    // Forward past any number of trailing spaces
    while (reader.charAt(0) == ' ') {
      reader.pop(originalChunks);
    }
    new CommentChannel(true).consume(reader, output);
    // If the next character is not a null or line break, an error has
    // occurred.
    char ch = reader.charAt(0);
    String lineBreak = scanLineBreak(reader);
    originalChunks.append(lineBreak);
    if (lineBreak.length() == 0 && ch != '\0') {
      throw new YamlLexerException(WHILE_SCANNING_BLOCK_SCALAR, startMark,
        "expected a comment or a line break, but found " + ch, reader.getCursor());
    }
  }

  /**
   * Scans for the indentation of a block scalar implicitly. This mechanism is
   * used only if the block did not explicitly state an indentation to be
   * used.
   *
   * @see http://www.yaml.org/spec/1.1/#id927035
   */
  private Object[] scanBlockScalarIndentation(CodeReader reader, StringBuilder originalChunks) {
    // See the specification for details.
    StringBuilder chunks = new StringBuilder();
    int maxIndent = 0;
    // Look ahead some number of lines until the first non-blank character
    // occurs; the determined indentation will be the maximum number of
    // leading spaces on any of these lines.
    while ((Lexer.LINEBR_S + " \r").indexOf(reader.charAt(0)) != -1) {
      if (reader.charAt(0) != ' ') {
        // If the character isn't a space, it must be some kind of
        // line-break; scan the line break and track it.
        String breaks = scanLineBreak(reader);
        chunks.append(breaks);
        originalChunks.append(breaks);
      } else {
        // If the character is a space, move forward to the next
        // character; if we surpass our previous maximum for indent
        // level, update that too.
        reader.pop(originalChunks);
        if (reader.getColumnPosition() > maxIndent) {
          maxIndent = reader.getColumnPosition();
        }
      }
    }
    // Pass several results back together.
    return new Object[] {chunks.toString(), maxIndent};
  }

  private Object[] scanBlockScalarBreaks(CodeReader reader, int indent, StringBuilder originalChunks) {
    // See the specification for details.
    StringBuilder chunks = new StringBuilder();
    int col = reader.getColumnPosition();
    // Scan for up to the expected indentation-level of spaces, then move
    // forward past that amount.
    while (col < indent && reader.charAt(0) == ' ') {
      reader.pop(originalChunks);
      col++;
    }
    // Consume one or more line breaks followed by any amount of spaces,
    // until we find something that isn't a line-break.
    String lineBreak;
    while ((lineBreak = scanLineBreak(reader)).length() != 0) {
      chunks.append(lineBreak);
      originalChunks.append(lineBreak);
      // Scan past up to (indent) spaces on the next line, then forward
      // past them.
      col = reader.getColumnPosition();
      while (col < indent && reader.charAt(0) == ' ') {
        reader.pop(originalChunks);
        col++;
      }
    }
    // Return both the assembled intervening string and the end-mark.
    return new Object[] {chunks.toString()};
  }

  /**
   * Chomping the tail may have 3 values - yes, no, not defined.
   */
  private static class Chomping {
    private final Boolean value;
    private final int increment;

    Chomping(@Nullable Boolean value, int increment) {
      this.value = value;
      this.increment = increment;
    }

    boolean chompTailIsNotFalse() {
      return value == null || value;
    }

    boolean chompTailIsTrue() {
      return value != null && value;
    }

    int getIncrement() {
      return increment;
    }
  }
}
