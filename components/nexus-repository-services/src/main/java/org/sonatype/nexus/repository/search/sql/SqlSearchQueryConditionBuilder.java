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

import java.util.Collection;
import java.util.Set;

/**
 * Utility class for building exact and wildcard sql search query conditions.
 *
 * @see SqlSearchQueryCondition
 * @since 3.38
 */
public interface SqlSearchQueryConditionBuilder
{
  public static final String ESCAPE = "\\";

  public static final char ANY_CHARACTER = '?';

  public static final char ZERO_OR_MORE_CHARACTERS = '*';

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field &lt;operator&gt; #{field}",
   * {field=value})</code>
   */
  SqlSearchQueryCondition condition(String field, String value);

  /**
   * Creates a SqlSearchQueryCondition of the form <code>SqlSearchQueryCondition("field &lt;operator&gt; #{field}",
   * {field=value})</code>
   * The keys of the parameters Map contained in the created <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  SqlSearchQueryCondition condition(String field, String value, String parameterPrefix);

  /**
   * Builds an exact and/or wildcard conditions depending on the specified values for the specified field.
   *
   * If the specified values contains both exact and wildcard values then the conditions are ORed for the specified
   * field.
   */
  SqlSearchQueryCondition condition(String fieldName, Set<String> values);

  /**
   * Builds an exact and/or wildcard conditions depending on the specified values for the specified field.
   *
   * If the specified values contains both exact and wildcard values then the conditions are ORed for the specified
   * field.
   *
   * The keys of the parameters Map contained in the returned <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  SqlSearchQueryCondition condition(String fieldName, Set<String> values, String parameterPrefix);

  /**
   * Builds an exact conditions with empty value for the specified field.
   */
  SqlSearchQueryCondition conditionWithEmptyValue(String field);

  /**
   * Builds an exact conditions with empty value for the specified field.
   *
   * The keys of the parameters Map contained in the returned <code>SqlQueryCondition</code> are prefixed with the
   * specified <code>parameterPrefix</code>.
   */
  SqlSearchQueryCondition conditionWithEmptyValue(String field, String parameterPrefix);

  /**
   * Combines the specified collection of SqlSearchQueryCondition objects into a single SqlSearchQueryCondition where
   * the various {@link SqlSearchQueryCondition#getSqlConditionFormat()} are 'ORed' into a single conditionFormat String
   * and the {@link SqlSearchQueryCondition#getValues()} are combined into one <code>Map&lt;String, String&gt;</code>
   */
  SqlSearchQueryCondition combine(Collection<SqlSearchQueryCondition> conditions);

  String replaceWildcards(String value);

  String sanitise(String value);
}
