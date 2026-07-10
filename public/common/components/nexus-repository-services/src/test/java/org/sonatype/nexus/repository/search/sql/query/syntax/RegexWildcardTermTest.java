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
package org.sonatype.nexus.repository.search.sql.query.syntax;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RegexWildcardTermTest
{
  @Test
  public void testSingleQuestionMark() {
    RegexWildcardTerm term = new RegexWildcardTerm("com?ns-lang3");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^com.ns-lang3$"));
  }

  @Test
  public void testTrailingQuestionMark() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons?lang3?");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons.lang3.$"));
  }

  @Test
  public void testMultipleAsteriskWithTrailingQuestionMark() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons*lang?");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons.*lang.$"));
  }

  @Test
  public void testLeadingQuestionMarkWithAsterisk() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons?lang*");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons.lang.*$"));
  }

  @Test
  public void testAsteriskInMiddle() {
    RegexWildcardTerm term = new RegexWildcardTerm("com*ns-lang3");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^com.*ns-lang3$"));
  }

  @Test
  public void testMultipleAsterisks() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons*lang3*");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons.*lang3.*$"));
  }

  @Test
  public void testNoWildcards() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons-lang3");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons-lang3$"));
  }

  @Test
  public void testEscapeSequences() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons\\*lang");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons\\*lang$"));
  }

  @Test
  public void testEscapedQuestionMark() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons\\?lang");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons\\?lang$"));
  }

  @Test
  public void testRegexSpecialCharacters() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons.lang");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons\\.lang$"));
  }

  @Test
  public void testRegexSpecialCharacterBracket() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons[lang]");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^commons\\[lang\\]$"));
  }

  @Test
  public void testGetReturnsOriginalTerm() {
    RegexWildcardTerm term = new RegexWildcardTerm("commons*lang");
    assertThat(term.get(), is("commons*lang"));
  }

  @Test
  public void testNullTermThrowsNullPointerException() {
    org.junit.Assert.assertThrows(NullPointerException.class, () -> new RegexWildcardTerm(null));
  }

  @Test
  public void testEmptyPattern() {
    RegexWildcardTerm term = new RegexWildcardTerm("");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^$"));
  }

  @Test
  public void testOnlyWildcards() {
    RegexWildcardTerm term = new RegexWildcardTerm("?");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^.$"));
  }

  @Test
  public void testOnlyAsterisk() {
    RegexWildcardTerm term = new RegexWildcardTerm("*");
    String regex = term.toRegexPattern();
    assertThat(regex, is("^.*$"));
  }
}
