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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.search.table.TableSearchUtils;

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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.repository.search.sql.SqlSearchConditionType.EXACT;
import static org.sonatype.nexus.repository.search.sql.SqlSearchConditionType.WILDCARD;

/**
 * Utility class for building exact and wildcard sql search query conditions.
 *
 * @see SqlSearchQueryCondition
 * @since 3.38
 */
public abstract class SqlSearchQueryConditionBuilder
    extends ComponentSupport
{
  public static final String ESCAPE = "\\";

  public static final char ZERO_OR_MORE_CHARACTERS = '*';

  protected static final char ANY_CHARACTER = '?';

  private static final String VALUES_MUST_NOT_BE_EMPTY = "Values must not be empty.";

  private static final String FIELD_NAME_MUST_NOT_BE_EMPTY = "Field name must not be empty.";

  private static final String CONDITIONS_MUST_NOT_BE_EMPTY = "Conditions must not be empty.";

  private static final String FILTER_PARAMS = "filterParams";

  private static final String PLACEHOLDER_PARAMETER_PREFIX = "#{" + FILTER_PARAMS + ".";

  private static final String PLACEHOLDER_PARAMETER_SUFFIX = "}";

  private static final String LEFT_PARENTHESIS = "(";

  private static final String RIGHT_PARENTHESIS = ")";

  private static final String OR = " OR ";

  private static final char[] REGEX_IDENTIFIERS = {'*', '?'};

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field &lt;operator&gt; #{field}",
   * {field=value})</code>
   */
  public SqlSearchQueryCondition condition(final String field, final String value) {
    return createCondition(field, value, EMPTY);
  }

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field &lt;operator&gt; #{field}",
   * {field=value})</code>
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

  /**
   * Builds an exact conditions with empty value for the specified field.
   */
  public SqlSearchQueryCondition conditionWithEmptyValue(final String field) {
    return conditionWithEmptyValue(field, EMPTY);
  }

  /**
   * Builds an exact conditions with empty value for the specified field.
   *
   * The keys of the parameters Map contained in the returned <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  public SqlSearchQueryCondition conditionWithEmptyValue(
      final String field,
      final String parameterPrefix)
  {
    checkArgument(isNotBlank(field), FIELD_NAME_MUST_NOT_BE_EMPTY);
    return new SqlSearchQueryCondition(emptyOrNull(field, placeholder(parameterPrefix + field)),
        ImmutableMap.of(parameterPrefix + field, EMPTY));
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
      return new SqlSearchQueryCondition(equalTo(field, placeholder(parameterPrefix + field)),
          ImmutableMap.of(parameterPrefix + field, replaceEscapedWildcardSymbol(value)));
    }
    return wildcardCondition(field, value, parameterPrefix);
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

  public String replaceWildcards(final String value) {
    StringBuilder result = new StringBuilder();
    result.append(value.toCharArray());
    Map<Character, String> wildcardMapping = getWildcardMapping();

    for (char replaceChar : wildcardMapping.keySet()) {
      if (value.contains(String.valueOf(replaceChar))) {
        int index = value.indexOf(replaceChar);
        while (index >= 0) {
          int escapeIndex = value.indexOf(ESCAPE + replaceChar);
          if (escapeIndex == -1 || escapeIndex != index - 1) {
            String replacement = wildcardMapping.get(replaceChar);
            result.replace(index, index + 1, replacement);
          }
          index = value.indexOf(replaceChar, index + 1);
        }
      }
    }

    return String.valueOf(result);
  }

  protected abstract String equalTo(final String field, final String placeholder);

  protected abstract String emptyOrNull(final String field, final String placeholder);

  protected abstract String in(final String field, final List<String> placeholders);

  protected abstract String wildcard(final String fieldName, final String placeholder);

  protected abstract String wildcards(final String fieldName, final List<String> placeholders);

  protected abstract Map<Character, String> getWildcardMapping();

  protected abstract SqlSearchQueryCondition wildcardCondition(
      final String field,
      final String value,
      final String parameterPrefix);

  public String sanitise(final String value) {
    String result = trimStringAfterSecondDot(value);
    return replaceEscapedWildcardSymbol(replaceWildcards(result));
  }

  protected String placeholder(final String field) {
    return PLACEHOLDER_PARAMETER_PREFIX + field + PLACEHOLDER_PARAMETER_SUFFIX;
  }

  private static String replaceEscapedWildcardSymbol(final String value) {
    return value.replace(ESCAPE + ZERO_OR_MORE_CHARACTERS, String.valueOf(ZERO_OR_MORE_CHARACTERS))
        .replace(ESCAPE + ANY_CHARACTER, String.valueOf(ANY_CHARACTER));
  }

  /**
   * PostgreSQL insert & (AND) tsquery operator in case of a text contains 2 or more dots
   * and last word contains a digit(s) or one character.
   * For example:
   * <ul>
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.word1')     -> 'foo.bar' & 'word1':* </li> Not OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.a')         -> 'foo.bar' & 'a':* </li> Not OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.1.0')       -> 'foo.bar' & '1.0':* </> Not OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.word')      -> 'foo.bar.word':* </li> OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.word1.txt') -> 'foo.bar.word1.txt':* </li> OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar1')          -> 'foo.bar1':* </li> OK
   *  <li> PLAINTO_TSQUERY('simple', 'foo.bar.1.0.word')  -> 'foo.bar.1.0.word':* </> OK
   * </ul>
   * @param value search value
   * @return a sanitized value.
   */
  private static String trimStringAfterSecondDot(final String value) {
    if (TableSearchUtils.isRemoveLastWords(value)) {
      String[] split = value.split("\\.");
      return split[0] + '.' + split[1];
    }

    return value;
  }

  private static Map<SqlSearchConditionType, Set<String>> groupIntoExactOrWildcard(final Set<String> values) {
    return values.stream().collect(groupingBy(SqlSearchQueryConditionBuilder::exactOrWildcard, toSet()));
  }

  private static List<String> createValueNames(final String field, final int size) {
    return IntStream.range(0, size)
        .mapToObj(index -> field + index)
        .collect(toList());
  }

  private List<String> createPlaceholders(final List<String> values) {
    return values.stream()
        .map(this::placeholder)
        .collect(toList());
  }

  private Map<String, String> nameValues(final List<String> valueNames, final Set<String> values) {
    List<String> theValues = new ArrayList<>(values);
    return IntStream.range(0, Math.min(valueNames.size(), theValues.size()))
        .mapToObj(index -> new SimpleImmutableEntry<>(valueNames.get(index), theValues.get(index)))
        .collect(toMap(Entry::getKey, Entry::getValue));
  }

  private SqlSearchQueryCondition createWildcardCondition(
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

  private SqlSearchQueryCondition createExactCondition(
      final String fieldName,
      final Set<String> values,
      final List<String> valueNames,
      final List<String> placeholders)
  {
    final List<String> exactPlaceholders = placeholders.subList(0, values.size());
    final List<String> exactValueNames = valueNames.subList(0, values.size());
    final Set<String> escapedValues = values.stream()
            .map(SqlSearchQueryConditionBuilder::replaceEscapedWildcardSymbol)
            .collect(toSet());
    return new SqlSearchQueryCondition(in(fieldName, exactPlaceholders), nameValues(exactValueNames, escapedValues));
  }

  private static Map<String, String> flattenValues(final Stream<Map<String, String>> stream) {
    return stream.flatMap(value -> value.entrySet().stream()).collect(toMap(Entry::getKey, Entry::getValue));
  }

  protected static String join(final Stream<String> stream, final String delimiter) {
    return stream.collect(joining(delimiter, LEFT_PARENTHESIS, RIGHT_PARENTHESIS));
  }

  private Set<String> replaceWildcards(final Set<String> values) {
    return values.stream()
        .map(this::replaceWildcards)
        .map(SqlSearchQueryConditionBuilder::replaceEscapedWildcardSymbol)
        .collect(toSet());
  }
}
