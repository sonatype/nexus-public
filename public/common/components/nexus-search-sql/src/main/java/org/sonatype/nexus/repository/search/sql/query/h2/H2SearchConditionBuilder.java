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
package org.sonatype.nexus.repository.search.sql.query.h2;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.NullTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.RegexWildcardTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SingleValueTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.Term;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.replace;

/**
 * Base builder for converting {@link Expression} into H2 SQL syntax.
 * Uses LIKE queries instead of PostgreSQL's full-text search operators.
 */
abstract class H2SearchConditionBuilder
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected static final String BRACKET_CLOSE = ")";

  protected static final String BRACKET_OPEN = "(";

  protected final H2SearchDB db;

  protected final Map<String, Object> parameters = new HashMap<>();

  protected final ValidationErrorsException errors = new ValidationErrorsException();

  private int parameterIndex = 0;

  private final ConditionType conditionType;

  public enum ConditionType
  {
    COMPONENT_FILTER("filterParams"),
    ASSET_FILTER("assetFilterParams");

    private final String parameterName;

    ConditionType(final String filterParams) {
      this.parameterName = filterParams;
    }

    public String getParameterName() {
      return parameterName;
    }
  }

  H2SearchConditionBuilder(final H2SearchDB db) {
    this(db, ConditionType.COMPONENT_FILTER);
  }

  H2SearchConditionBuilder(final H2SearchDB db, final ConditionType conditionType) {
    this.db = checkNotNull(db);
    this.conditionType = conditionType;
  }

  public SqlSearchQueryCondition build(final Expression expression) {
    Optional<String> query = process(expression)
        .map(CharSequence::toString)
        .map(String::trim);

    if (!errors.getValidationErrors().isEmpty()) {
      throw errors;
    }

    return new SqlSearchQueryCondition(query.orElse(""), parameters);
  }

  private Optional<CharSequence> process(final Expression expression) {
    Optional<CharSequence> query;

    if (expression instanceof SqlClause clause) {
      query = process(clause);
    }
    else if (expression instanceof SqlPredicate predicate) {
      query = process(predicate);
    }
    else {
      errors.withError("Unknown expression " + expression);
      query = Optional.empty();
    }

    return query;
  }

  private Optional<CharSequence> process(final SqlClause clause) {
    if (clause.expressions().size() == 1) {
      return process(Iterables.getOnlyElement(clause.expressions()));
    }

    String operator = clause.operand() == Operand.AND ? " AND " : " OR ";

    String expression = clause.expressions()
        .stream()
        .map(this::process)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.joining(operator, BRACKET_OPEN, BRACKET_CLOSE));

    return Optional.of(expression);
  }

  private Optional<CharSequence> process(final SqlPredicate predicate) {
    Optional<H2SearchColumn> optColumn = db.getColumn(predicate.getSearchField());
    if (!optColumn.isPresent()) {
      log.error("Unknown column for search {}", predicate.getSearchField());
      return Optional.empty();
    }

    H2SearchColumn column = optColumn.get();
    Collection<SingleValueTerm<?>> terms = toList(predicate.getTerm());
    Operand op = predicate.operand();

    if (!op.supportsMultiple() && terms.size() != 1) {
      log.debug("Unsupported number of terms for operand {} for {} with terms {}", op, column, terms);
      errors.withError(
          String.format("Unsupported number of terms for operand %s for %s with terms %s", op, column, terms));
      return Optional.empty();
    }

    return createQuery(column, op, terms);
  }

  /**
   * Process a simple predicate, returns the query if created and terms will be included in the parameter map.
   *
   * @return the query text if made
   */
  protected Optional<CharSequence> createQuery(
      final H2SearchColumn column,
      final Operand op,
      final Collection<SingleValueTerm<?>> terms)
  {
    return createExactPredicate(column, op, terms);
  }

  /**
   * Creates a query for a predicate using simple SQL and an exact value
   */
  private Optional<CharSequence> createExactPredicate(
      final H2SearchColumn column,
      final Operand op,
      final Collection<SingleValueTerm<?>> terms)
  {
    switch (op) {
      case EQ:
        if (isNullTerm(terms)) {
          return Optional.of(column.columnName() + " IS NULL");
        }
        return createSimpleExpression(column, "=", Iterables.getOnlyElement(terms));
      case NOT_EQ:
        if (isNullTerm(terms)) {
          return Optional.of(column.columnName() + " IS NOT NULL");
        }
        return createSimpleExpression(column, "<>", Iterables.getOnlyElement(terms));
      case REGEX:
        return createRegexExpression(column, Iterables.getOnlyElement(terms));
      case IN:
        return createInExpression(column, terms);
      default:
        errors.withError("Unexpected operator " + op + " for column " + column);
        return Optional.empty();
    }
  }

  /**
   * Create a simple expression, i.e. {@code {column} {operator} {term}}
   */
  private Optional<CharSequence> createSimpleExpression(
      final H2SearchColumn column,
      final String operator,
      final SingleValueTerm<?> term)
  {
    String param = param(column.columnName());

    StringBuilder sb = new StringBuilder();
    sb.append(column.columnName());
    sb.append(' ').append(operator).append(' ');
    sb.append(placeholder(param));

    parameters.put(param, term.get());

    return Optional.of(sb);
  }

  /**
   * Creates a REGEXP_LIKE expression for H2 regex matching
   */
  private Optional<CharSequence> createRegexExpression(
      final H2SearchColumn column,
      final SingleValueTerm<?> term)
  {
    String param = param(column.columnName());

    StringBuilder sb = new StringBuilder();
    sb.append("REGEXP_LIKE(");
    sb.append(column.columnName());
    sb.append(", ");
    sb.append(placeholder(param));
    sb.append(")");

    Object value;
    if (term instanceof RegexWildcardTerm regexTerm) {
      value = regexTerm.toRegexPattern();
    }
    else {
      value = term.get();
    }
    parameters.put(param, value);

    return Optional.of(sb);
  }

  /**
   * Creates an SQL IN expression, e.g. {@code foo IN (?)}
   */
  private Optional<CharSequence> createInExpression(
      final H2SearchColumn column,
      final Collection<SingleValueTerm<?>> terms)
  {
    Map<String, Object> predicateParams =
        terms.stream().collect(Collectors.toMap(__ -> param(column.columnName()), SingleValueTerm::get));

    String predicate = predicateParams.keySet()
        .stream()
        .sorted()
        .map(this::placeholder)
        .collect(Collectors.joining(", ", column.columnName() + " IN (", BRACKET_CLOSE));

    parameters.putAll(predicateParams);

    return Optional.of(predicate);
  }

  private static String removeDots(final String value) {
    return replace(value, ".", "_");
  }

  /**
   * Creates a parameter name for the provided column. Guarantees uniqueness within this builder
   */
  protected String param(final String columnName) {
    return removeDots(columnName) + parameterIndex++;
  }

  /**
   * Creates a filter parameter syntax for mybatis
   */
  protected String placeholder(final String parameterName) {
    return "#{" + conditionType.getParameterName() + "." + parameterName + "}";
  }

  /**
   * Convert a {@link Term} which may be a {@link TermCollection} into Collection of {@link SingleValueTerm}
   */
  protected static Collection<SingleValueTerm<?>> toList(final Term term) {
    if (term instanceof TermCollection tc) {
      return tc.get();
    }
    return List.of((SingleValueTerm<?>) term);
  }

  /**
   * Check whether the provided terms are {@code null}
   */
  private static boolean isNullTerm(final Collection<SingleValueTerm<?>> terms) {
    return terms.stream().allMatch(NullTerm.class::isInstance);
  }
}
