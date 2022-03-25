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

import com.sonar.sslr.api.Token;
import java.net.URI;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.JsonNode;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.grammar.YamlGrammar;
import org.apiaddicts.apitools.dosonarapi.sslr.yaml.snakeyaml.parser.Tokens;


public class MissingNode extends JsonNode {
  public static final JsonNode MISSING = new MissingNode();

  public MissingNode() {
    super(YamlGrammar.MISSING, "MISSING", Token.builder().setLine(1).setColumn(1).setURI(URI.create("file://unknown")).setType(Tokens.NULL).setValueAndOriginalValue("null").build());
  }

  @Override
  public boolean isMissing() {
    return true;
  }
}
