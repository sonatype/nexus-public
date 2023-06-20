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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.sql.TextualQueryType;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryConditionBuilder;

import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.removeEnd;

/**
 * Query building utility for full text search columns
 *
 * @see TextualQueryType#FULL_TEXT_SEARCH_QUERY
 * @since 3.43
 */
@Singleton
@Named(PostgresFullTextSearchQueryBuilder.NAME)
public class PostgresFullTextSearchQueryBuilder
    extends SqlSearchQueryConditionBuilder
{
  public static final String CONDITION_FORMAT = "%s @@ %s";

  public static final String NAME = "full_text_search_query_builder";

  private static final String PIPE = "||";

  private static final String TS_QUERY_FORMAT = "PLAINTO_TSQUERY('simple', %s)";

  private static final String EMPTY_OR_NULL_CONDITION_FORMAT = "%s = %s OR %s IS NULL";

  private static final String WILDCARD_TS_QUERY_FORMAT =
      "TO_TSQUERY('simple', PLAINTO_TSQUERY('simple', %s)::text || ':*')";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  public static final String PREFIX_MATCHER = ":*";

  @Override
  public String replaceWildcards(final String value) {
    String wildcardReplacedValue = super.replaceWildcards(value);
    return convertInfixWildcardToSuffix(wildcardReplacedValue);
  }

  private String convertInfixWildcardToSuffix(final String wildcardReplacedValue) {
    if (wildcardReplacedValue.contains(PREFIX_MATCHER) && !wildcardReplacedValue.trim().endsWith(PREFIX_MATCHER)) {
      int prefixMatcherPosition = wildcardReplacedValue.indexOf(PREFIX_MATCHER);
      if (prefixMatcherPosition > 0) {
        return wildcardReplacedValue.substring(0, prefixMatcherPosition + PREFIX_MATCHER.length());
      }
    }
    return wildcardReplacedValue;
  }

  @Override
  protected SqlSearchQueryCondition wildcardCondition(
      final String field,
      final String value,
      final String parameterPrefix)
  {
    return new SqlSearchQueryCondition(wildcard(field, placeholder(parameterPrefix + field)),
        ImmutableMap.of(parameterPrefix + field, removeEnd(sanitise(value), ":*")));
  }

  @Override
  protected String equalTo(final String field, final String placeholder) {
    return getCondition(field, tsQuery(placeholder, TS_QUERY_FORMAT));
  }

  @Override
  protected String emptyOrNull(final String field, final String placeholder) {
    return getEmptyOrNullCondition(field, placeholder);
  }

  @Override
  protected String in(final String field, final List<String> placeholders) {
    return getCondition(field, tsQuery(placeholders, TS_QUERY_FORMAT));
  }

  @Override
  protected String wildcard(final String fieldName, final String placeholder) {
    return getCondition(fieldName, tsQuery(placeholder, WILDCARD_TS_QUERY_FORMAT));
  }

  @Override
  protected String wildcards(final String fieldName, final List<String> placeholders) {
    return getCondition(fieldName, tsQuery(placeholders, WILDCARD_TS_QUERY_FORMAT));
  }

  @Override
  protected Map<Character, String> getWildcardMapping() {
    return ImmutableMap.of(ZERO_OR_MORE_CHARACTERS, PREFIX_MATCHER, ANY_CHARACTER, PREFIX_MATCHER);
  }

  /**
   * Forms postgres full text search lexemes/tokens for each search term in <code>searchTerms</code> so that they can be
   * used for searching a TSVECTOR column for matches.
   *
   * @param searchTerms The texts to search for
   * @param queryFormat Preformatted string contains pattern for query creation
   * @return Returns <code>TS_TSQUERY('simple', searchTerm1) || TS_TSQUERY('simple', searchTerm2) ||
   * TS_TSQUERY('simple', searchTermN)</code>
   */
  private static String tsQuery(final List<String> searchTerms, final String queryFormat) {
    return searchTerms.stream()
        .map(searchTerm -> tsQuery(searchTerm, queryFormat))
        .collect(joining(PIPE, LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
  }

  private String getCondition(final String fieldName, final String placeholder) {
    return String.format(CONDITION_FORMAT, fieldName, placeholder);
  }

  private String getEmptyOrNullCondition(final String fieldName, final String placeholder) {
    return String.format(EMPTY_OR_NULL_CONDITION_FORMAT, fieldName, placeholder, fieldName);
  }

  /**
   * Forms postgres full text search lexemes/tokens for the specified <code>searchTerm</code> so that they can be used
   * for searching a TSVECTOR column for matches.
   *
   * @param searchTerm The text to search for
   * @param queryFormat Preformatted string contains pattern for query creation
   * @return Returns <code>TS_TSQUERY('simple', text)</code>
   */
  private static String tsQuery(final String searchTerm, final String queryFormat) {
    return String.format(queryFormat, searchTerm);
  }
}
