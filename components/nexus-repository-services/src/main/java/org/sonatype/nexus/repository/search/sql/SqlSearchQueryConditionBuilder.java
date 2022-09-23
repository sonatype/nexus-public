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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.search.sql.SqlSearchConditionType.EXACT;
import static org.sonatype.nexus.repository.search.sql.SqlSearchConditionType.WILDCARD;

/**
 * Utility class for building exact and wildcard sql search query conditions.
 *
 * @see SqlSearchQueryCondition
 * @since 3.38
 */
@Named
@Singleton
public class SqlSearchQueryConditionBuilder
    extends ComponentSupport
{
  public static final String ESCAPE = "\\";

  public static final char ZERO_OR_MORE_CHARACTERS = '*';

  private static final char SQL_ANY_CHARACTER = '_';

  private static final char SQL_ZERO_OR_MORE_CHARACTERS = '%';

  private static final String LIKE = " LIKE ";

  private static final char ANY_CHARACTER = '?';

  private static final String VALUES_MUST_NOT_BE_EMPTY = "Values must not be empty.";

  private static final String FIELD_NAME_MUST_NOT_BE_EMPTY = "Field name must not be empty.";

  private static final String CONDITIONS_MUST_NOT_BE_EMPTY = "Conditions must not be empty.";

  private static final String FILTER_PARAMS = "filterParams";

  private static final String PLACEHOLDER_PARAMETER_PREFIX = "#{" + FILTER_PARAMS + ".";

  private static final String PLACEHOLDER_PARAMETER_SUFFIX = "}";

  private static final String COMMA = ",";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  private static final String IN = " IN ";

  private static final String OR = " OR ";

  private static final char[] REGEX_IDENTIFIERS = {'*', '?'};

  private static final Map<Character, Character> wildcardMapping = ImmutableMap.of(
      ANY_CHARACTER, SQL_ANY_CHARACTER,
      ZERO_OR_MORE_CHARACTERS, SQL_ZERO_OR_MORE_CHARACTERS
  );

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field = #{field}",
   * {field=value})</code> if the specified value is an exact value or a SqlSearchQueryCondition of the form
   * <code>SqlSearchQueryCondition("field LIKE #{field}", {field=value})</code> if the specified value is a wildcard.
   */
  public SqlSearchQueryCondition condition(final String field, final String value) {
    return createCondition(field, value, EMPTY);
  }

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field = #{field}",
   * {field=value})</code> if the specified value is an exact value or a SqlSearchQueryCondition of the form
   * <code>SqlSearchQueryCondition("field LIKE #{field}", {field=value})</code> if the specified value is a wildcard.
   *
   * The keys of the parameters Map contained in the created <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  public SqlSearchQueryCondition condition(final String field, final String value, final String parameterPrefix) {
    return createCondition(field, value, parameterPrefix);
  }

  /**
   * Builds an exact and/or wildcard conditions depending on the specified values for the specified field.
   *
   * If the specified values contains both exact and wildcard values then the conditions are ORed for the specified
   * field.
   */
  public SqlSearchQueryCondition condition(final String fieldName, final Set<String> values) {
    checkArgument(isNotBlank(fieldName), FIELD_NAME_MUST_NOT_BE_EMPTY);
    checkArgument(!values.isEmpty(), VALUES_MUST_NOT_BE_EMPTY);

    return createCondition(fieldName, values, EMPTY);
  }

  /**
   * Builds an exact and/or wildcard conditions depending on the specified values for the specified field.
   *
   * If the specified values contains both exact and wildcard values then the conditions are ORed for the specified
   * field.
   *
   * The keys of the parameters Map contained in the returned <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  public SqlSearchQueryCondition condition(
      final String fieldName,
      final Set<String> values,
      final String parameterPrefix)
  {
    return createCondition(fieldName, values, parameterPrefix);
  }

  private SqlSearchQueryCondition createCondition(
      final String fieldName,
      final Set<String> values,
      final String parameterPrefix)
  {
    if (values.size() == 1) {
      return condition(fieldName, getOnlyElement(values), parameterPrefix);
    }

    final Map<SqlSearchConditionType, Set<String>> valueGroups = groupIntoExactOrWildcard(values);
    final List<String> valueNames = createValueNames(parameterPrefix + fieldName, values.size());
    final List<String> placeholders = createPlaceholders(valueNames);
    final List<SqlSearchQueryCondition> conditions = new ArrayList<>();

    if (valueGroups.containsKey(EXACT)) {
      conditions.add(createExactCondition(fieldName, valueGroups.get(EXACT), valueNames, placeholders));
    }

    if (valueGroups.containsKey(WILDCARD)) {
      conditions.add(createWildcardCondition(fieldName, valueGroups, valueNames, placeholders));
    }
    return combine(conditions);
  }

  private SqlSearchQueryCondition createCondition(
      final String field,
      final String value,
      final String parameterPrefix)
  {
    checkArgument(isNotBlank(field), FIELD_NAME_MUST_NOT_BE_EMPTY);
    checkArgument(isNotBlank(value), "Value must not be empty.");

    if (exactOrWildcard(value) == EXACT) {
      return new SqlSearchQueryCondition(equalTo(field, parameterPrefix),
          ImmutableMap.of(parameterPrefix + field, replaceEscapedWildcardSymbol(value)));
    }
    return new SqlSearchQueryCondition(wildcard(field, placeholder(parameterPrefix + field)),
        ImmutableMap.of(parameterPrefix + field, replaceEscapedWildcardSymbol(replaceWildcards(value))));
  }

  /**
   * Combines the specified collection of SqlSearchQueryCondition objects into a single SqlSearchQueryCondition where
   * the various {@link SqlSearchQueryCondition#getSqlConditionFormat()} are 'ORed' into a single conditionFormat String
   * and the {@link SqlSearchQueryCondition#getValues()} are combined into one <code>Map&lt;String, String&gt;</code>
   */
  public SqlSearchQueryCondition combine(Collection<SqlSearchQueryCondition> conditions) {
    checkArgument(!conditions.isEmpty(), CONDITIONS_MUST_NOT_BE_EMPTY);

    if (conditions.size() == 1) {
      return getOnlyElement(conditions);
    }

    return new SqlSearchQueryCondition(
        join(conditions.stream().map(SqlSearchQueryCondition::getSqlConditionFormat), OR),
        flattenValues(conditions.stream().map(SqlSearchQueryCondition::getValues)));
  }

  private static SqlSearchConditionType exactOrWildcard(final String value) {
    if (isWildcard(value)) {
      return SqlSearchConditionType.WILDCARD;
    }
    return EXACT;
  }

  public static boolean isWildcard(final String value) {
    for (char regexChar : REGEX_IDENTIFIERS) {
      if (value.contains(String.valueOf(regexChar))) {
        int index = value.indexOf(regexChar);
        while (index >= 0) {
          int escapeIndex = value.indexOf(ESCAPE + regexChar);
          if (escapeIndex == -1 || escapeIndex != index - 1) {
            return true;
          }
          index = value.indexOf(regexChar, index + 1);
        }
      }
    }
    return false;
  }

  private static String equalTo(final String field, final String parameterPrefix) {
    return String.format("%s = %s", field, placeholder(parameterPrefix + field));
  }

  private static String wildcard(final String fieldName, final String placeholder) {
    return fieldName + LIKE + placeholder;
  }

  private static String placeholder(final String field) {
    return PLACEHOLDER_PARAMETER_PREFIX + field + PLACEHOLDER_PARAMETER_SUFFIX;
  }

  private static String replaceWildcards(final String value) {
    String escapedValue = escapeWildcardSymbolIfExists(value);
    char[] result = escapedValue.toCharArray();

    for (char replaceChar : wildcardMapping.keySet()) {
      if (escapedValue.contains(String.valueOf(replaceChar))) {
        int index = escapedValue.indexOf(replaceChar);
        while (index >= 0) {
          int escapeIndex = escapedValue.indexOf(ESCAPE + replaceChar);
          if (escapeIndex == -1 || escapeIndex != index - 1) {
            result[index] = wildcardMapping.get(replaceChar);
          }
          index = escapedValue.indexOf(replaceChar, index + 1);
        }
      }
    }

    return String.valueOf(result);
  }

  private static String escapeWildcardSymbolIfExists(final String value) {
    String result = value;
    if (value.endsWith(ESCAPE) && !value.endsWith(ESCAPE + ESCAPE)){
      result += ESCAPE;
    }
    return result.replace(String.valueOf(SQL_ZERO_OR_MORE_CHARACTERS), ESCAPE + SQL_ZERO_OR_MORE_CHARACTERS)
        .replace(String.valueOf(SQL_ANY_CHARACTER), ESCAPE + SQL_ANY_CHARACTER);
  }

  private static String replaceEscapedWildcardSymbol(final String value) {
    return value.replace(ESCAPE + ZERO_OR_MORE_CHARACTERS, String.valueOf(ZERO_OR_MORE_CHARACTERS))
        .replace(ESCAPE + ANY_CHARACTER, String.valueOf(ANY_CHARACTER));
  }

  private static Map<SqlSearchConditionType, Set<String>> groupIntoExactOrWildcard(final Set<String> values) {
    return values.stream().collect(groupingBy(SqlSearchQueryConditionBuilder::exactOrWildcard, toSet()));
  }

  private static List<String> createValueNames(final String field, final int size) {
    return IntStream.range(0, size)
        .mapToObj(index -> field + index)
        .collect(toList());
  }

  private static List<String> createPlaceholders(final List<String> values) {
    return values.stream()
        .map(SqlSearchQueryConditionBuilder::placeholder)
        .collect(toList());
  }

  private static Map<String, String> nameValues(final List<String> valueNames, final Set<String> values) {
    List<String> theValues = new ArrayList<>(values);
    return IntStream.range(0, Math.min(valueNames.size(), theValues.size()))
        .mapToObj(index -> new SimpleImmutableEntry<>(valueNames.get(index), replaceEscapedWildcardSymbol(theValues.get(index))))
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  private static SqlSearchQueryCondition createWildcardCondition(
      final String fieldName,
      final Map<SqlSearchConditionType, Set<String>> valueGroups,
      final List<String> valueNames,
      final List<String> placeholders)
  {
    final Set<String> wildcardValues = valueGroups.get(WILDCARD);
    final Set<String> exactValues = valueGroups.getOrDefault(EXACT, emptySet());
    final List<String> wildcardPlaceholders = placeholders.subList(exactValues.size(), placeholders.size());
    final List<String> wildcardValueNames = valueNames.subList(exactValues.size(), placeholders.size());

    return new SqlSearchQueryCondition(wildcards(fieldName, wildcardPlaceholders),
        nameValues(wildcardValueNames, replaceWildcards(wildcardValues)));
  }

  private static String wildcards(final String fieldName, final List<String> placeholders) {
    return placeholders.stream()
        .map(value -> wildcard(fieldName, value))
        .collect(joining(OR, LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
  }

  private static SqlSearchQueryCondition createExactCondition(
      final String fieldName,
      final Set<String> values,
      final List<String> valueNames,
      final List<String> placeholders)
  {
    final List<String> exactPlaceholders = placeholders.subList(0, values.size());
    final List<String> exactValueNames = valueNames.subList(0, values.size());
    return new SqlSearchQueryCondition(in(fieldName, exactPlaceholders), nameValues(exactValueNames, values));
  }

  private static Map<String, String> flattenValues(final Stream<Map<String, String>> stream) {
    return stream.flatMap(value -> value.entrySet().stream()).collect(toMap(Entry::getKey, Entry::getValue));
  }

  private static String join(final Stream<String> stream, final String delimiter) {
    return stream.collect(joining(delimiter, LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
  }

  private static String in(final String field, final List<String> placeholders) {
    return field + IN + createInClausePlaceholders(placeholders);
  }

  private static String createInClausePlaceholders(final List<String> values) {
    return join(values.stream(), COMMA);
  }

  public static Set<String> replaceWildcards(final Set<String> values) {
    return values.stream()
        .map(SqlSearchQueryConditionBuilder::replaceWildcards)
        .map(SqlSearchQueryConditionBuilder::replaceEscapedWildcardSymbol)
        .collect(toSet());
  }
}
