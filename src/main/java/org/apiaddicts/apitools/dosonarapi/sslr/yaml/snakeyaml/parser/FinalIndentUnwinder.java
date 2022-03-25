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

import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Preprocessor;
import com.sonar.sslr.api.PreprocessorAction;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class FinalIndentUnwinder extends Preprocessor {

  private final IndentUnwinder unwinder;

  private static boolean isGenerated(Token token) {
    return token.getOriginalValue().equals("");
  }

  public FinalIndentUnwinder(LexerState state) {
    this.unwinder = new IndentUnwinder(state);
  }

  @Override
  public PreprocessorAction process(List<Token> tokens) {
    Token token = tokens.get(0);
    if (token.getType().equals(GenericTokenType.EOF)) {
      List<Token> toInject = unwinder.unwindIndent(-1, token.getLine(), token.getColumn(), token.getURI());
      if (!toInject.isEmpty()) {
        return new PreprocessorAction(0, Collections.emptyList(), toInject);
      }
    } else if (isScalar(token) && tokens.size() > 3) {
      // look ahead to see if there's a key/block-mapping to invert
      // see ValueChannel
      List<Token> toInject = new ArrayList<>();
      if (tokens.get(1).getType() == Tokens.KEY && isGenerated(tokens.get(1))) {
        toInject.add(tokens.get(1));
        toInject.add(tokens.get(0));
        if (tokens.get(2).getType() == Tokens.BLOCK_MAPPING_START && isGenerated(tokens.get(2))) {
          toInject.add(0, tokens.get(2));
        }
      }
      if (!toInject.isEmpty()) {
        return new PreprocessorAction(toInject.size(), Collections.emptyList(), toInject);
      }
    }
    return PreprocessorAction.NO_OPERATION;
  }

  private static final EnumSet<Tokens> SCALARS = EnumSet.of(Tokens.STRING, Tokens.FLOAT, Tokens.INTEGER, Tokens.TRUE, Tokens.FALSE, Tokens.NULL);

  private static boolean isScalar(Token token) {
    return SCALARS.contains(token.getType());
  }
}
