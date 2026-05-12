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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Arrays;
import java.util.Optional;

import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_2;
import static org.sonatype.nexus.repository.rest.sql.SearchField.FORMAT_FIELD_3;
import static org.sonatype.nexus.repository.rest.sql.SearchField.KEYWORDS;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAME;
import static org.sonatype.nexus.repository.rest.sql.SearchField.NAMESPACE;
import static org.sonatype.nexus.repository.rest.sql.SearchField.VERSION;

public class KeywordSqlSearchQueryContributionTest
{
  public static final String GAVEC_CONDITION_FORMAT = "GAVEC_CONDITION";

  private KeywordSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    underTest = new KeywordSqlSearchQueryContribution();
    underTest.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())));
  }

  @Test
  public void shouldIgnoreNull() {
    Optional<Expression> result = underTest.createPredicate(null);

    assertTrue(!result.isPresent());
  }

  @Test
  public void shouldBeMavenGavecSearchCondition() {
    Optional<Expression> result =
        underTest.createPredicate(new SearchFilter("keyword", "org.mockito:mockito-core:3.24:jar:tests"));

    assertTrue(result.isPresent());

    assertThat(result.get(), is(SqlClause.create(Operand.AND, predicate(Operand.EQ, NAMESPACE, exact("org.mockito")),
        predicate(Operand.EQ, NAME, exact("mockito-core")), predicate(Operand.EQ, VERSION, exact("3.24")),
        predicate(Operand.EQ, FORMAT_FIELD_2, exact("jar")), predicate(Operand.EQ, FORMAT_FIELD_3, exact("tests")))));
  }

  @Test
  public void splitByAndSearch() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("keyword", "mockito junit"));

    assertThat(result.get(),
        is(SqlClause.create(Operand.OR, new SqlPredicate(Operand.EQ, KEYWORDS, new WildcardTerm("mockito")),
            new SqlPredicate(Operand.EQ, KEYWORDS, new WildcardTerm("junit")))));
  }

  @Test
  public void shouldCreateExactTermWithDoubleQuotes() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("keyword", "\"aether-util\""));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, KEYWORDS, new ExactTerm("aether-util"))));
  }

  @Test
  public void shouldCreateWildcardTermWithoutQuotes() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("keyword", "aether-util"));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, KEYWORDS, new WildcardTerm("aether-util"))));
  }

  @Test
  public void shouldHandleMultipleQuotedTerms() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("keyword", "\"aether\" \"util\""));

    assertTrue(result.isPresent());
    // Note: Empty string between quoted terms is also captured as ExactTerm
    assertThat(result.get(),
        is(SqlClause.create(Operand.OR, new SqlPredicate(Operand.EQ, KEYWORDS, new ExactTerm("aether")),
            new SqlPredicate(Operand.EQ, KEYWORDS, new ExactTerm("")),
            new SqlPredicate(Operand.EQ, KEYWORDS, new ExactTerm("util")))));
  }

  @Test
  public void shouldHandleMixedQuotedAndUnquoted() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("keyword", "aether \"util\""));

    assertTrue(result.isPresent());
    assertThat(result.get(),
        is(SqlClause.create(Operand.OR, new SqlPredicate(Operand.EQ, KEYWORDS, new WildcardTerm("aether")),
            new SqlPredicate(Operand.EQ, KEYWORDS, new ExactTerm("util")))));
  }

  private static ExactTerm exact(final String value) {
    return new ExactTerm(value);
  }

  private static SqlPredicate predicate(final Operand operand, final SearchField searchField, final StringTerm term) {
    return new SqlPredicate(operand, searchField, term);
  }
}
