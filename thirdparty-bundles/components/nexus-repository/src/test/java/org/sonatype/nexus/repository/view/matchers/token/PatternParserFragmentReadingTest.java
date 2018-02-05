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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.view.matchers.token.PatternParser.readFragment;

public class PatternParserFragmentReadingTest
    extends TestSupport
{
  public static final String WILD = ".+";

  public static final List<Character> NONE = Collections.<Character>emptyList();

  @Test
  public void readLiteralFragment() {
    final String s = readFragment(it("abcdefg"), asList('-'), asList('-'), NONE);

    assertThat(s, is(equalTo("abcdefg")));
  }

  @Test
  public void readTerminatedFragment() {
    final String s = readFragment(it("abc-defg"), asList('-'), asList('\\'), NONE);
    assertThat(s, is(equalTo("abc")));
  }

  @Test
  public void readEscapedFragment() {
    final String s = readFragment(it("abc\\-d-efg"), asList('-'), asList('\\'), NONE);
    assertThat(s, is(equalTo("abc-d")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void readDisallowedCharacter() {
    readFragment(it("s*fs"), NONE, NONE, asList('*'));
  }

  @Test(expected = IllegalArgumentException.class)
  public void escapingDoesNotProtectDisallowedCharacters() {

    readFragment(it("s\\*fs"), NONE, asList('\\'), asList('*'));
  }

  private CharacterIterator it(final String sdfs) {
    return new StringCharacterIterator(sdfs);
  }
}
