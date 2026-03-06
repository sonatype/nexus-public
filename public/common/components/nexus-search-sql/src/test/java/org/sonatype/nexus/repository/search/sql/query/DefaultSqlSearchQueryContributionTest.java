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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.*;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultSqlSearchQueryContributionTest
    extends TestSupport
{
  private static final String GROUP_RAW = "group.raw";

  private DefaultSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    underTest = new DefaultSqlSearchQueryContribution();
    underTest.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())));
  }

  @Test
  public void shouldIgnoreNull() {
    assertFalse(underTest.createPredicate(null).isPresent());
  }

  @Test
  public void shouldIgnoreWhitespace() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter(GROUP_RAW, "    "));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new ExactTerm(""))));
  }

  @Test
  public void shouldIgnoreUnknownFields() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("an_unknown_field", "mockito"));

    assertFalse(result.isPresent());
  }

  @Test
  public void shouldAddConditionToQueryBuilder() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter(GROUP_RAW, "unit? mockito*"));

    assertTrue(result.isPresent());
    assertThat(result.get(),
        is(SqlClause.create(Operand.OR, new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new WildcardTerm("unit")),
            new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new WildcardTerm("mockito")))));
  }

  @Test
  public void testSplit() {
    assertThat(split("foo*"), contains(new WildcardTerm("foo")));
    assertThat(split("foo?*"), contains(new WildcardTerm("foo")));
    assertThat(split("foo?"), contains(new WildcardTerm("foo")));
    assertThat(split("foo*bar*ddd"),
        contains(new WildcardTerm("foo"), new WildcardTerm("bar"), new ExactTerm("ddd")));
    assertThat(split("foo?bar"), contains(new WildcardTerm("foo"), new ExactTerm("bar")));

    // escaped wildcards - these should be ExactTerm since group field now uses exact matching
    assertThat(split("foo\\*bar"), contains(new ExactTerm("foo*bar")));
    assertThat(split("foo\\?bar"), contains(new ExactTerm("foo?bar")));
    assertThat(split("foo\\*"), contains(new ExactTerm("foo*")));
    assertThat(split("foo\\?"), contains(new ExactTerm("foo?")));
    assertThat(split("\\*bar"), contains(new ExactTerm("*bar")));
    assertThat(split("\\?bar"), contains(new ExactTerm("?bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testLeadingAsterick() {
    assertThat(split("*bar"), contains(new LenientTerm("bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testTooFewWildcard() {
    split("ba*");
  }

  @Test(expected = ValidationErrorsException.class)
  public void testLeadingQuestion() {
    assertThat(split("?bar"), contains(new LenientTerm("bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testSpecialCharacterAndLeadingWildcard() {
    assertThat(split("/*"), contains(new LenientTerm("bar")));
  }

  @Test
  public void testCaseInsensitiveGroupSearch() {
    // Test uppercase letters are handled as ExactTerm (exact matching with original case)
    assertThat(split("Support"), contains(new ExactTerm("Support")));
    assertThat(split("TEAM"), contains(new ExactTerm("TEAM")));
    assertThat(split("Maven"), contains(new ExactTerm("Maven")));
  }

  @Test
  public void testHierarchicalGroupNames() {
    // Test hierarchical group names with slashes and mixed case (exact matching)
    assertThat(split("/Support/Team"),
        contains(new ExactTerm("/Support/Team")));
    assertThat(split("org/apache/maven"),
        contains(new ExactTerm("org/apache/maven")));
  }

  @Test
  public void testExactMatchField() {
    // Test that name.raw field (which has exactMatch=true) creates ExactTerm
    Collection<SingleValueTerm<?>> result = splitForField("name.raw", "hello");
    assertThat(result, contains(new ExactTerm("hello")));
  }

  @Test
  public void testExactMatchFieldWithMultipleTerms() {
    // Test that name.raw field with multiple space-separated terms creates multiple ExactTerms
    Collection<SingleValueTerm<?>> result = splitForField("name.raw", "hello world");
    assertThat(result, contains(new ExactTerm("hello"), new ExactTerm("world")));
  }

  @Test
  public void testLenientMatchField() {
    // Test that group.raw field now creates ExactTerm (changed to exactMatch=true for NEXUS-49265)
    Collection<SingleValueTerm<?>> result = splitForField(GROUP_RAW, "support");
    assertThat(result, contains(new ExactTerm("support")));
  }

  private Collection<SingleValueTerm<?>> split(final String value) {
    return splitForField(GROUP_RAW, value);
  }

  private Collection<SingleValueTerm<?>> splitForField(final String field, final String value) {
    Optional<Expression> expression = underTest.createPredicate(new SearchFilter(field, value));

    assertTrue(expression.isPresent());

    Expression expr = expression.get();

    // Handle SqlClause (multiple terms) or SqlPredicate (single term)
    if (expr instanceof SqlClause) {
      SqlClause clause = (SqlClause) expr;
      return clause.expressions()
          .stream()
          .map(e -> ((SqlPredicate) e).getTerm())
          .flatMap(term -> {
            if (term instanceof StringTerm st) {
              return Stream.of(st);
            }
            else if (term instanceof TermCollection tc) {
              return tc.get().stream();
            }
            return Stream.empty();
          })
          .toList();
    }
    else if (expr instanceof SqlPredicate) {
      Term term = ((SqlPredicate) expr).getTerm();

      if (term instanceof StringTerm st) {
        return List.of(st);
      }
      else if (term instanceof TermCollection tc) {
        return tc.get();
      }
    }

    fail("Unknown expression: " + expr);
    return null;
  }
}
