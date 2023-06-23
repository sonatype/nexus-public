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
package org.sonatype.nexus.repository.search.sql.textsearch;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PostgresFullTextSearchQueryBuilderTest
    extends TestSupport
{
  private final PostgresFullTextSearchQueryBuilder underTest = new PostgresFullTextSearchQueryBuilder();

  @Test
  public void shouldBePipeSeparatedTsQuery() {

    assertThat(underTest.in("repository_name", asList("repo1", "repo2", "repo3")),
        is("repository_name @@ " +
            "(PLAINTO_TSQUERY('simple', repo1)||PLAINTO_TSQUERY('simple', repo2)||PLAINTO_TSQUERY('simple', repo3))"));

    assertThat(underTest.wildcards("repository_name", asList("repo1", "repo2", "repo3")),
        is("repository_name @@ (TO_TSQUERY('simple', PLAINTO_TSQUERY('simple', repo1)::text || ':*')" +
            "||TO_TSQUERY('simple', PLAINTO_TSQUERY('simple', repo2)::text || ':*')" +
            "||TO_TSQUERY('simple', PLAINTO_TSQUERY('simple', repo3)::text || ':*'))"));
  }

  @Test
  public void shouldBeATsQuery() {
    assertThat(underTest.equalTo("repository_name", "repo"),
        is("repository_name @@ PLAINTO_TSQUERY('simple', repo)"));

    assertThat(underTest.wildcard("repository_name", "repo"),
        is("repository_name @@ TO_TSQUERY('simple', PLAINTO_TSQUERY('simple', repo)::text || ':*')"));
  }

  @Test
  public void shouldReplaceWildcard() {
    assertThat(underTest.replaceWildcards("foo*"), is("foo:*"));

    assertThat(underTest.replaceWildcards("bar*"), is("bar:*"));
  }

  @Test
  public void shouldEscapeSymbolBeforeReplacingWildcards() {
    assertThat(underTest.sanitise("foo:*"), is("foo::*"));
  }

  @Test
  public void shouldRemoveLastToken() {
    assertThat(underTest.sanitise("foo.bar.test_1"), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar.1test"), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar.a"), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar."), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar.num.version"), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar.1.1.1"), is("foo.bar"));
    assertThat(underTest.sanitise("foo.bar.fu1.fu2"), is("foo.bar"));
  }

  @Test
  public void shouldNotRemoveLastToken() {
    assertThat(underTest.sanitise("foo.test"), is("foo.test"));
    assertThat(underTest.sanitise("foo"), is("foo"));
    assertThat(underTest.sanitise("foo$bar$"), is("foo$bar$"));
    assertThat(underTest.sanitise("foo%bar%a%txt"), is("foo%bar%a%txt"));
  }
}
