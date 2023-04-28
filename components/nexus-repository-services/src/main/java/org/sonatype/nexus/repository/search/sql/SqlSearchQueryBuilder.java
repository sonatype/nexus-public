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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.search.SqlSearchQueryContribution;
import org.sonatype.nexus.repository.search.query.SearchFilter;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Accumulates conditions created by the {@link SqlSearchQueryContribution} implementations for each {@link
 * SearchFilter} passed into them. It holds the conditions built so far.
 *
 * @since 3.38
 */
public class SqlSearchQueryBuilder
{
  private static final String AND = " AND ";

  private static final String OR = " OR ";

  private final List<SqlSearchQueryCondition> conditions = new ArrayList<>();

  private final String operator;

  private SqlSearchQueryBuilder(final String operator) {
    this.operator = operator;
  }

  public SqlSearchQueryBuilder add(final SqlSearchQueryCondition condition) {
    checkArgument(nonNull(condition), "Condition must not be blank.");
    conditions.add(condition);
    return this;
  }

  /**
   * Create a query builder with the AND operator
   */
  public static SqlSearchQueryBuilder conjunctionBuilder() {
    return new SqlSearchQueryBuilder(AND);
  }

  /**
   * Create a query builder with the OR operator
   */
  public static SqlSearchQueryBuilder disjunctionBuilder() {
    return new SqlSearchQueryBuilder(OR);
  }

  /**
   * Concatenates all the SqlSearchQueryCondition conditions into a single one.
   * All the {@link SqlSearchQueryCondition#getSqlConditionFormat()} are 'ANDed' into one and all the  {@link
   * SqlSearchQueryCondition#getValues()} are combined into one.
   *
   * The intention is for this to be used in building a SQL query through a {@link java.sql.PreparedStatement}.
   * The {@link SqlSearchQueryCondition#getValues()} contains named values which corresponds to the {@link
   * SqlSearchQueryCondition#getSqlConditionFormat()}.
   *
   * @return A {@link SqlSearchQueryCondition} which is a consolidation of all the conditionFormats and named values in
   * this object.
   */
  public Optional<SqlSearchQueryCondition> buildQuery() {
    if (conditions.isEmpty()) {
      return empty();
    }
    return of(new SqlSearchQueryCondition(joinQueryFormats(), getValues()));
  }

  private String joinQueryFormats() {
    Stream<String> conditionsStream = conditions.stream()
        .map(SqlSearchQueryCondition::getSqlConditionFormat);

    if (conditions.size() > 1) {
      conditionsStream = conditionsStream.map(SqlSearchQueryBuilder::wrapInBrackets);
    }

    return conditionsStream.collect(joining(operator));
  }

  private static String wrapInBrackets(final String input) {
    return "(" + input + ")";
  }

  private Map<String, String> getValues() {
    return conditions.stream()
        .map(SqlSearchQueryCondition::getValues)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(toMap(Entry::getKey, Entry::getValue));
  }
}
