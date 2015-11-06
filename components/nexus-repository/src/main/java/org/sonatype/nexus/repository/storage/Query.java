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
package org.sonatype.nexus.repository.storage;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper for constructing orientDB queries.
 *
 * @since 3.0
 */
public class Query
{
  /**
   * Helper for creating query builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private final StringBuilder where = new StringBuilder();

    private final Map<String, Object> parameters = Maps.newHashMap();

    private final StringBuilder suffix = new StringBuilder();

    private int parameterNumber;

    private Builder() {
      // nop
    }

    /**
     * Appends to the 'where' clause.
     */
    public Builder where(String where) {
      this.where.append(where);
      return this;
    }

    /**
     * Appends an equals expression for a parameterized value.
     */
    public Builder eq(Object value) {
      checkNotNull(value);
      checkState(hasWhere(), "Missing where statement");
      return this.where(" = ").param(value);
    }

    public Builder and(String value) {
      checkNotNull(value);
      checkState(hasWhere(), "Missing where statement");
      return where(" AND ").where(value);
    }

    public Builder or(String value) {
      checkNotNull(value);
      checkState(hasWhere(), "Missing where statement");
      return where(" OR ").where(value);
    }

    public Builder isNull() {
      checkState(hasWhere(), "Missing where statement");
      return where(" IS NULL ");
    }

    public Builder isNotNull() {
      checkState(hasWhere(), "Missing where statement");
      return where(" IS NOT NULL ");
    }

    public boolean hasWhere() {
      return clean(where) != null;
    }

    /**
     * Appends a parameterized value to the where clause.
     */
    public Builder param(Object value) {
      return param("p", value);
    }

    private Builder param(String parameterName, Object value) {
      final String mangledName = checkNotNull(parameterName) + parameterNumber++;
      parameters.put(mangledName, value);
      where(":" + mangledName);
      return this;
    }

    public Builder suffix(String suffix) {
      this.suffix.append(suffix);
      return this;
    }

    public Query build() {
      final StringBuilder str = where;
      return new Query(clean(str), clean(suffix), Collections.unmodifiableMap(parameters)
      );
    }

    private String clean(final StringBuilder str) {
      return Strings.emptyToNull(str.toString().trim());
    }
  }

  private final String where, suffix;

  private final Map<String, Object> parameters;

  private Query(final String where, final String suffix, final Map<String, Object> parameters) {
    this.where = where;
    this.suffix = suffix;
    this.parameters = parameters;
  }

  public String getWhere() {
    return where;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public String getQuerySuffix() {
    return suffix;
  }
}
