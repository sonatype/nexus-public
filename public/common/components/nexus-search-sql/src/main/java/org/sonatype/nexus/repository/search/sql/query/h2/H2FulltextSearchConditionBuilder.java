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
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.search.sql.query.syntax.BooleanTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SingleValueTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import com.google.common.collect.Iterables;

/**
 * H2 implementation of fulltext search using LIKE queries instead of PostgreSQL's TSVECTOR.
 *
 * This builder converts search terms into LIKE expressions that work with H2's VARCHAR columns.
 */
public class H2FulltextSearchConditionBuilder
    extends H2SearchConditionBuilder
{
  H2FulltextSearchConditionBuilder(final H2SearchDB db) {
    super(db);
  }

  H2FulltextSearchConditionBuilder(final H2SearchDB db, final ConditionType conditionType) {
    super(db, conditionType);
  }

  @Override
  protected Optional<CharSequence> createQuery(
      final H2SearchColumn column,
      final Operand op,
      final Collection<SingleValueTerm<?>> terms)
  {
    if (requiresTextSearch(column, terms)) {
      return createTextSearch(column, op, terms);
    }
    return super.createQuery(column, op, terms);
  }

  /**
   * Text search is used for columns that support it.
   * For tokenized columns, we always use LIKE queries even for single exact terms because tokenized data
   * is stored as space-separated values (e.g., "0.1 0 1"), not in PostgreSQL TSVECTOR format.
   * For non-tokenized columns, we use exact matching for single exact terms.
   */
  private static boolean requiresTextSearch(final H2SearchColumn column, final Collection<SingleValueTerm<?>> terms) {
    if (!column.supportsTextSearch()) {
      return false;
    }

    // Tokenized columns always need LIKE matching
    if (column.tokenized()) {
      return true;
    }

    // Non-tokenized columns: use exact matching when there is a single exact term
    if (terms.size() > 1) {
      return true;
    }
    SingleValueTerm<?> term = Iterables.getOnlyElement(terms);

    return !(term instanceof ExactTerm || term instanceof BooleanTerm);
  }

  private Optional<CharSequence> createTextSearch(
      final H2SearchColumn column,
      final Operand operand,
      final Collection<SingleValueTerm<?>> terms)
  {
    if (terms.stream().allMatch(term -> term instanceof StringTerm st && Strings2.isBlank(st.get()))) {
      return Optional.empty();
    }

    String operator;
    switch (operand) {
      case EQ:
        operator = " AND ";
        break;
      case ANY:
      case IN:
        operator = " OR ";
        break;
      case REGEX:
        return super.createQuery(column, operand, terms);
      default:
        errors.withError("Unsupported operand " + operand + " for terms " + terms);
        return Optional.empty();
    }

    String query = terms.stream()
        .map(term -> createLikeExpression(column, term))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(CharSequence::toString)
        .collect(Collectors.joining(operator, BRACKET_OPEN, BRACKET_CLOSE));

    return Optional.of(query);
  }

  /**
   * Creates a LIKE expression for the term.
   * H2 doesn't support full-text search, so we use case-insensitive LIKE queries.
   */
  private Optional<CharSequence> createLikeExpression(final H2SearchColumn column, final SingleValueTerm<?> term) {
    String value = term.get().toString();

    if (Strings2.isBlank(value)) {
      return Optional.empty();
    }

    String param = param(column.columnName());
    String likePattern;

    if (term instanceof ExactTerm) {
      // Exact match - wrap in % for contains search
      likePattern = "%" + maybeHandleJsonExactTerm(column, escapeLike(value)) + "%";
    }
    else if (term instanceof WildcardTerm) {
      // Wildcard - convert * to % for SQL LIKE
      likePattern = "%" + maybeHandleJsonWildCardTerm(column, escapeLike(value).replace("*", "%")) + "%";
    }
    else if (term instanceof LenientTerm) {
      // Lenient - treat as contains
      likePattern = "%" + escapeLike(value) + "%";
    }
    else {
      // Default - treat as contains
      likePattern = "%" + escapeLike(value) + "%";
    }

    parameters.put(param, likePattern);

    // Use LOWER() for case-insensitive search
    return Optional.of("LOWER(" + column.columnName() + ") LIKE LOWER(" + placeholder(param) + ")");
  }

  /**
   * Escape special characters in LIKE patterns
   */
  private String escapeLike(final String value) {
    return value
        .replace("\\", "\\\\") // Escape backslash first
        .replace("%", "\\%") // Escape percent
        .replace("_", "\\_"); // Escape underscore
  }

  private String maybeHandleJsonExactTerm(final H2SearchColumn column, final String value) {
    return addJsonDelimitersIfNeeded(column, value, JsonQuotingMode.EXACT);
  }

  private String maybeHandleJsonWildCardTerm(final H2SearchColumn column, final String value) {
    return addJsonDelimitersIfNeeded(column, value, JsonQuotingMode.WILDCARD);
  }

  /**
   * Quoting mode for JSON column matching.
   */
  private enum JsonQuotingMode
  {
    /**
     * Match exact JSON array element
     */
    EXACT,

    /**
     * Match JSON array element by prefix
     */
    WILDCARD
  }

  /**
   * Adds JSON string delimiters to prevent incorrect substring matching in JSON columns.
   *
   * @param column the search column
   * @param value the search value
   * @param mode the quoting mode to apply
   * @return the value with JSON delimiters if column is JSON, otherwise unchanged
   */
  private String addJsonDelimitersIfNeeded(
      final H2SearchColumn column,
      final String value,
      final JsonQuotingMode mode)
  {
    if (!column.isJsonColumn()) {
      return value;
    }

    return switch (mode) {
      case EXACT -> "\"" + value + "\"";
      case WILDCARD -> "\"" + value;
    };
  }
}
