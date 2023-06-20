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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.rest.sql.TextualQueryType;

import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.joining;

/**
 * Query building utility for none full text search columns
 *
 * @see TextualQueryType#DEFAULT_TEXT_QUERY
 * @since 3.43
 */
@Named(DefaultSqlSearchQueryConditionBuilder.NAME)
@Singleton
public class DefaultSqlSearchQueryConditionBuilder
    extends SqlSearchQueryConditionBuilder
{
  public static final char ZERO_OR_MORE_CHARACTERS = '*';

  public static final char SQL_ZERO_OR_MORE_CHARACTERS = '%';

  public static final char SQL_ANY_CHARACTER = '_';

  public static final char ANY_CHARACTER = '?';

  private static final String IN = " IN ";

  private static final String LIKE = " LIKE ";

  private static final String COMMA = ",";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  private static final String OR = " OR ";

  public static final String NAME = "default_text_query_builder";

  @Override
  protected SqlSearchQueryCondition wildcardCondition(
      final String field,
      final String value,
      final String parameterPrefix)
  {
    return new SqlSearchQueryCondition(wildcard(field, placeholder(parameterPrefix + field)),
        ImmutableMap.of(parameterPrefix + field, sanitise(value)));
  }

  protected String equalTo(final String field, final String placeholder) {
    return String.format("%s = %s", field, placeholder);
  }

  protected String emptyOrNull(final String field, final String placeholder) {
    return String.format("%s = %s OR %s IS NULL", field, placeholder, field);
  }

  protected String in(final String field, final List<String> placeholders) {
    return field + IN + createInClausePlaceholders(placeholders);
  }

  protected String wildcard(final String fieldName, final String placeholder) {
    return fieldName + LIKE + placeholder;
  }

  protected String wildcards(final String fieldName, final List<String> placeholders) {
    return placeholders.stream()
        .map(value -> wildcard(fieldName, value))
        .collect(joining(OR, LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
  }

  @Override
  protected Map<Character, String> getWildcardMapping() {
    return ImmutableMap.of(
        ANY_CHARACTER, String.valueOf(SQL_ANY_CHARACTER),
        ZERO_OR_MORE_CHARACTERS, String.valueOf(SQL_ZERO_OR_MORE_CHARACTERS)
    );
  }

  private static String createInClausePlaceholders(final List<String> values) {
    return join(values.stream(), COMMA);
  }
}
