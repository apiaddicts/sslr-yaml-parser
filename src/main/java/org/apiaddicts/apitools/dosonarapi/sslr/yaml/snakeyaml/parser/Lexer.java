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

import com.sonar.sslr.impl.channel.BlackHoleChannel;
import com.sonar.sslr.impl.channel.UnknownCharacterChannel;

import java.nio.charset.Charset;

public abstract class Lexer {
    protected static final String LINEBR_S = "\n\u0085\u2028\u2029\uFFFF";
    private static final String FULL_LINEBR_S = "\r" + LINEBR_S;
    protected static final String NULL_OR_LINEBR_S = "\0" + FULL_LINEBR_S;
    protected static final String NULL_BL_LINEBR_S = " " + NULL_OR_LINEBR_S;
    protected static final String NULL_BL_T_LINEBR_S = "\t" + NULL_BL_LINEBR_S;

    private Lexer() {
        // hidden utility class constructor
    }

    public static com.sonar.sslr.impl.Lexer create(Charset charset) {
        LexerState state = new LexerState();
        return com.sonar.sslr.impl.Lexer.builder()
                .withCharset(charset)
                .withFailIfNoChannelToConsumeOneCharacter(true)
                .withPreprocessor(new FinalIndentUnwinder(state))
                .withChannel(new BlackHoleChannel(" ++"))
                .withChannel(new CommentChannel(false))
                .withChannel(new UnwindIndentChannel(state))
                .withChannel(new LineBreakChannel(state))
                .withChannel(new BlockEntryChannel(state))
                .withChannel(new FlowCollectionChannel(state))
                .withChannel(new KeyChannel(state))
                .withChannel(new ValueChannel(state))
                .withChannel(new ScalarChannel(state))
                .withChannel(new UnknownCharacterChannel())
                .build();
    }
}
