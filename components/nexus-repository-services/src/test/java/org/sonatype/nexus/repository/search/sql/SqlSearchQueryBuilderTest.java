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
package org.sonatype.nexus.repository.search.sql;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder.conjunctionBuilder;
import static org.sonatype.nexus.repository.search.sql.SqlSearchQueryBuilder.disjunctionBuilder;

public class SqlSearchQueryBuilderTest
    extends TestSupport
{
  @Test
  public void conjunctionBuilderShouldStartEmpty() {
    assertThat(conjunctionBuilder().buildQuery(), is(empty()));
  }

  @Test
  public void conjunctionBuilderShouldReturnASqlSearchQueryBuilder() {
    final SqlSearchQueryBuilder actual = conjunctionBuilder();

    assertThat(actual, is(notNullValue()));
    assertThat(actual.equals(conjunctionBuilder()), is(false));
  }

  @Test
  public void testConjunctionBuilderShouldAndConditions() {
    verifyQueryBuilderUsesOperatorForAllConditions(conjunctionBuilder(), " AND ");
  }

  @Test
  public void disjunctionBuilderShouldStartEmpty() {
    assertThat(disjunctionBuilder().buildQuery(), is(empty()));
  }

  @Test
  public void disjunctionBuilderShouldReturnASqlSearchQueryBuilder() {
    final SqlSearchQueryBuilder actual = disjunctionBuilder();

    assertThat(actual, is(notNullValue()));
    assertThat(actual.equals(disjunctionBuilder()), is(false));
  }

  @Test
  public void testDisjunctionBuilderShouldAndConditions() {
    verifyQueryBuilderUsesOperatorForAllConditions(disjunctionBuilder(), " OR ");
  }

  private void verifyQueryBuilderUsesOperatorForAllConditions(final SqlSearchQueryBuilder underTest, final String operator) {
    final String conditionFormat1 = "(namespace = ?)";
    final String conditionFormat2 = "(repository_name IN (?,?,?))";
    final String conditionFormat3 = "(name LIKE ? OR name LIKE ?)";

    final Map<String, String> values1 = of("namespace", "junit");
    final Map<String, String> values2 = of("repository_name0", "repo1", "repository_name1", "repo2",
        "repository_name2", "repo3");
    final Map<String, String> values3 = of("name0", "abc%", "name1", "_def");

    underTest.add(aSqlSearchCondition(conditionFormat1, values1));
    underTest.add(aSqlSearchCondition(conditionFormat2, values2));
    underTest.add(aSqlSearchCondition(conditionFormat3, values3));

    final Optional<SqlSearchQueryCondition> actual = underTest.buildQuery();

    assertThat(actual.isPresent(), is(true));
    assertThat(actual.get().getSqlConditionFormat(), is(
        String.join(operator, wrapInBrackets(conditionFormat1), wrapInBrackets(conditionFormat2),
            wrapInBrackets(conditionFormat3))));

    final Map<String, String> expectedValues = joinAllValues(values1, values2, values3);

    assertThat(actual.get().getValues().keySet(), containsInAnyOrder(expectedValues.keySet().toArray()));
    assertThat(actual.get().getValues().values(), containsInAnyOrder(expectedValues.values().toArray()));
  }

  private static String wrapInBrackets(final String input) {
    return "(" + input + ")";
  }

  private Map<String, String> joinAllValues(
      final Map<String, String> values1,
      final Map<String, String> values2,
      final Map<String, String> values3)
  {
    Map<String, String> allValues = new HashMap<>();
    allValues.putAll(values1);
    allValues.putAll(values2);
    allValues.putAll(values3);
    return allValues;
  }

  private SqlSearchQueryCondition aSqlSearchCondition(final String conditionFormat, final Map<String, String> values) {
    return new SqlSearchQueryCondition(conditionFormat, values);
  }
}
