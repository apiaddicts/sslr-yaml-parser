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

import com.sonar.sslr.impl.Lexer;
import com.sonar.sslr.impl.LexerException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sonar.sslr.channel.CodeReader;

public class LexerState {
  private int flowLevel = 0;
  private boolean allowSimpleKey = true;
  private int indent = -1;
  private Deque<Integer> indents;
  private Map<Integer, SimpleKey> possibleSimpleKeys;

  /**
   * The number of unclosed '{' and '['. `flow_level == 0` means block context.
   * @return the number
   */
  public int flowLevel() {
    return flowLevel;
  }

  public void increaseFlowLevel() {
    ++flowLevel;
  }

  public void decreaseFlowLevel() {
    --flowLevel;
  }

  /**
   * The current indentation level (a column index).
   * @return the current indentation level
   */
  public int indent() {
    return indent;
  }

  /**
   * Past indentation levels.
   * @return the new indentation level
   */
  public int popIndent() {
    indent = indents.pop();
    return indent;
  }

  /**
   * <pre>
   * A simple key is a key that is not denoted by the '?' indicator.
   * Example of simple keys:
   *   ---
   *   block simple key: value
   *   ? not a simple key:
   *   : { flow simple key: value }
   * We emit the KEY token before all keys, so when we find a potential
   * simple key, we try to locate the corresponding ':' indicator.
   * Simple keys should be limited to a single line and 1024 characters.
   *
   * Can a simple key start at the current position? A simple key may
   * start:
   * - at the beginning of the line, not counting indentation spaces
   *       (in block context),
   * - after '{', '[', ',' (in the flow context),
   * - after '?', ':', '-' (in the block context).
   * In the block context, this flag also signifies if a block collection
   * may start at the current position.
   * </pre>
   * @return {@code true} if the current context allows declaring simple keys
   */
  public boolean allowSimpleKey() {
    return allowSimpleKey;
  }

  public void allowSimpleKey(boolean allowSimpleKey) {
    this.allowSimpleKey = allowSimpleKey;
  }

  /*
   * Keep track of possible simple keys. This is a dictionary. The key is
   * `flow_level`; there can be no more that one possible simple key for each
   * level. The value is a SimpleKey record: (token_number, required, index,
   * line, column, mark) A simple key may start with ALIAS, ANCHOR, TAG,
   * SCALAR(flow), '[', or '{' tokens.
   */
  public Map<Integer, SimpleKey> possibleSimpleKeys() {
    return possibleSimpleKeys;
  }

  public LexerState() {
    this.indents = new ArrayDeque<>();
    // The order in possibleSimpleKeys is kept for nextPossibleSimpleKey()
    this.possibleSimpleKeys = new LinkedHashMap<>();
  }

  /**
   * Check if we need to increase indentation.
   */
  boolean addIndent(int column) {
    if (this.indent < column) {
      this.indents.push(this.indent);
      this.indent = column;
      return true;
    }
    return false;
  }

  /**
   * The next token may start a simple key. We check if it's possible and save
   * its position. This function is called for ALIAS, ANCHOR, TAG,
   * SCALAR(flow), '[', and '{'.
   */
  void savePossibleSimpleKey(CodeReader reader, Lexer output) {
    // The next token may start a simple key. We check if it's possible
    // and save its position. This function is called for
    // ALIAS, ANCHOR, TAG, SCALAR(flow), '[', and '{'.

    // Check if a simple key is required at the current position.
    // A simple key is required if this position is the root flowLevel, AND
    // the current indentation level is the same as the last indent-level.
    boolean required = (this.flowLevel == 0) && (this.indent == reader.getColumnPosition());

    if (allowSimpleKey || !required) {
      // A simple key is required only if it is the first token in the
      // current line. Therefore it is always allowed.
    } else {
      throw new LexerException(
        "A simple key is required only if it is the first token in the current line");
    }

    // The next token might be a simple key. Let's save it's number and
    // position.
    if (this.allowSimpleKey) {
      removePossibleSimpleKey(reader);
      int tokenNumber = output.getTokens().size();
      SimpleKey key = new SimpleKey(tokenNumber, required,
        reader.getLinePosition(), reader.getColumnPosition());
      this.possibleSimpleKeys.put(this.flowLevel, key);
    }
  }

  /**
   * Remove the saved possible key position at the current flow level.
   */
  void removePossibleSimpleKey(CodeReader code) {
    SimpleKey key = possibleSimpleKeys.remove(flowLevel);
    if (key != null && key.isRequired()) {
      throw new YamlLexerException("while scanning a simple key", null,
        "could not find expected ':'", code.getCursor());
    }
  }

  /**
   * <pre>
   * Remove entries that are no longer possible simple keys. According to
   * the YAML specification, simple keys
   * - should be limited to a single line,
   * - should be no longer than 1024 characters.
   * Disabling this procedure will allow simple keys of any length and
   * height (may cause problems if indentation is broken though).
   * </pre>
   */
  void stalePossibleSimpleKeys(CodeReader code) {
    if (!possibleSimpleKeys.isEmpty()) {
      for (Iterator<SimpleKey> iterator = possibleSimpleKeys.values().iterator(); iterator
        .hasNext();) {
        SimpleKey key = iterator.next();
        if (key.getLine() != code.getLinePosition()) {
          // TODO - we should check that we have not moved more than 1024 characters forward
          // If the key is not on the same line as the current
          // position OR the difference in column between the token
          // start and the current position is more than the maximum
          // simple key length, then this cannot be a simple key.
          if (key.isRequired()) {
            // If the key was required, this implies an error
            // condition.
            throw new YamlLexerException("While scanning a simple key", code.getCursor(),
              "could not find expected ':'", code.getCursor());
          }
          iterator.remove();
        }
      }
    }
  }
}
