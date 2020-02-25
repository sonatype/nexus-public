/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.view.matchers.token;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link PatternParser}.
 */
public class PatternParserTest
    extends TestSupport
{
  public static final String DEFAULT = PatternParser.DEFAULT_VARIABLE_REGEXP;

  @Test
  public void parseSimplePatterns() {
    checkTokenDetection("asdfasdf", lit("asdfasdf"));
    checkTokenDetection("/{a}", lit("/"), var("a", DEFAULT));
    checkTokenDetection("/{a}/{b}", lit("/"), var("a", DEFAULT), lit("/"), var("b", DEFAULT));
    checkTokenDetection("{chocolate}/{build}-{version}.{extension}", var("chocolate", DEFAULT), lit("/"),
        var("build", DEFAULT), lit("-"), var("version", DEFAULT), lit("."), var("extension", DEFAULT));
  }

  @Test
  public void parseRegexp() {
    // A simple regexp variable that matches everything
    checkTokenDetection("{name:.*}", var("name", ".*"));

    // A variable that matches only letters
    checkTokenDetection("{letters:[A-Za-z]*}", var("letters", "[A-Za-z]*"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void parsingUnfinishedVariableFails() {
    parse("{");
  }

  public void parseEscapedVariableStartAsLiteral() {
    checkTokenDetection("\\{hi}", lit("\\{hi}"));
  }

  private void checkTokenDetection(final String pattern, Token... expectedTokens) {
    final PatternParser parser = parse(pattern);
    final List<Token> tokens = parser.getTokens();

    assertThat(tokens, is(equalTo(asList(expectedTokens))));
  }

  private PatternParser parse(final String pattern) {return new PatternParser(pattern);}

  private static Token var(String name, String regexp) {
    return new VariableToken(name, regexp);
  }

  private static Token lit(String value) {
    return new LiteralToken(value);
  }
}
