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
package org.sonatype.nexus.repository.search.sql.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class SearchTokenizerTest
{
  @Test
  void testTsEscape() {
    assertThat(SearchTokenizer.tsEscape("asdf"), is("'asdf'"));
    assertThat(SearchTokenizer.tsEscape("as'df"), is("'as\\'df'"));
    assertThat(SearchTokenizer.tsEscape("AS'DF"), is("'as\\'df'"));
    assertThat(SearchTokenizer.tsEscape("as\\df"), is("'as\\\\df'"));
  }

  @Test
  void testStripLeadingSeparators() {
    assertThat(SearchTokenizer.stripLeadingSeparators(""), is(""));
    assertThat(SearchTokenizer.stripLeadingSeparators("-"), is(""));
    assertThat(SearchTokenizer.stripLeadingSeparators("-asdf"), is("asdf"));
    assertThat(SearchTokenizer.stripLeadingSeparators("/-\\._ asdf"), is("asdf"));
  }

  @Test
  void testStripTrailingSeparators() {
    assertThat(SearchTokenizer.stripTrailingSeparators(""), is(""));
    assertThat(SearchTokenizer.stripTrailingSeparators("-"), is(""));
    assertThat(SearchTokenizer.stripTrailingSeparators("asdf-"), is("asdf"));
    assertThat(SearchTokenizer.stripTrailingSeparators("EUCLID/"), is("EUCLID"));
    assertThat(SearchTokenizer.stripTrailingSeparators("EUCLID"), is("EUCLID"));
    assertThat(SearchTokenizer.stripTrailingSeparators("/EUCLID/"), is("/EUCLID"));
    assertThat(SearchTokenizer.stripTrailingSeparators("term."), is("term"));
    assertThat(SearchTokenizer.stripTrailingSeparators("asdf /-\\._"), is("asdf"));
  }

  @ParameterizedTest
  @CsvSource({
      "package_id, true", "package.id, true", "package-id, true",
      "package\\id, true", "package/id, true", "package id, true",
      "packageid, false", "package*id, false", "package#id, false"
  })
  void testTokenizerRegex(String expression, boolean expectedValue) {
    assertThat(SearchTokenizer.TOKENIZER.matcher(expression).find(), is(expectedValue));
  }
}
