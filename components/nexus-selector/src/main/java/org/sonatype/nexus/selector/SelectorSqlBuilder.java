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
package org.sonatype.nexus.selector;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.isAlphanumeric;

/**
 * Builder of SQL 'where' clauses for content selectors.
 *
 * @since 3.16
 */
public class SelectorSqlBuilder
{
  private final StringBuilder queryBuilder = new StringBuilder();

  private final Map<String, String> queryParameters = new HashMap<>();

  private final Map<String, String> propertyAliases = new HashMap<>();

  private String propertyPrefix = "";

  private String parameterPrefix = "";

  /**
   * Aliases the given property name to a specific record field.
   */
  public SelectorSqlBuilder propertyAlias(final String name, final String alias) {
    propertyAliases.put(checkNotNull(name), checkNotNull(alias));
    return this;
  }

  /**
   * Sets the record field prefix to use for non-aliased property names.
   */
  public SelectorSqlBuilder propertyPrefix(final String prefix) {
    propertyPrefix = checkNotNull(prefix);
    return this;
  }

  /**
   * Sets the unique prefix to use for generated parameter names.
   */
  public SelectorSqlBuilder parameterPrefix(final String prefix) {
    parameterPrefix = checkNotNull(prefix);
    return this;
  }

  /**
   * Appends the given property to the query, aliasing/prefixing it as necessary.
   */
  public void appendProperty(final String property) {
    queryBuilder.append(propertyAliases.computeIfAbsent(property, p -> {
      checkArgument(isAlphanumeric(p));
      return propertyPrefix + p;
    }));
  }

  /**
   * Appends the given literal to the query; storing it as a parameter under a generated name.
   */
  public void appendLiteral(final String literal) {
    String parameter = parameterPrefix + queryParameters.size();
    queryBuilder.append(':').append(parameter);
    queryParameters.put(parameter, literal);
  }

  /**
   * Appends the given operator to the query.
   */
  public void appendOperator(final String operator) {
    queryBuilder.append(' ').append(operator).append(' ');
  }

  /**
   * Appends the given expression to the query, nested inside brackets to preserve precedence.
   */
  public void appendExpression(final Runnable expression) {
    queryBuilder.append('(');
    expression.run();
    queryBuilder.append(')');
  }

  /**
   * Returns the query string built so far.
   */
  public String getQueryString() {
    return queryBuilder.toString();
  }

  /**
   * Returns the parameters stored so far.
   */
  public Map<String, String> getQueryParameters() {
    return queryParameters;
  }

  /**
   * Clears the query string and its parameters so this builder can be re-used to build a new query.
   *
   * Any configured aliases or prefixes are left in place.
   */
  public void clearQueryString() {
    queryBuilder.setLength(0);
    queryParameters.clear();
  }
}
