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
package org.sonatype.nexus.repository.rest;

import java.util.Objects;

import org.sonatype.nexus.repository.rest.sql.SearchField;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes an asset/component property which is exposed as a query term for search APIs and internally maps these
 * properties to their storage in ElasticSearch and SQL.
 */
public class SearchMapping
{
  private final String attribute;

  private final String alias;

  private final String description;

  private final SearchField field;

  private final boolean exactMatch;

  private final FilterType filterType;

  public enum FilterType {
    COMPONENT,
    ASSET;
  }

  /**
   * Creates a SearchMapping where the exact match of the mapping is true
   */
  public SearchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchField field)
  {
    this(alias, attribute, description, field, true);
  }

  public SearchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchField field,
      final boolean exactMatch) {
    this(alias, attribute, description, field, exactMatch, FilterType.COMPONENT);
  }

  public SearchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchField field,
      final FilterType filterType) {
    this(alias, attribute, description, field, true, filterType);
  }

  public SearchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchField field,
      final boolean exactMatch,
      final FilterType filterType) {
    this.alias = checkNotNull(alias);
    this.attribute = checkNotNull(attribute);
    this.description = checkNotNull(description);
    this.field = checkNotNull(field);
    this.exactMatch = exactMatch;
    this.filterType = checkNotNull(filterType);
  }

  /**
   * The property in which ElasticSearh stores the value for this term.
   */
  public String getAttribute() {
    return attribute;
  }

  /**
   * An identifier that external resources should use as an alias when executing search queries.
   *
   *  i.e. maven.extension=jar
   */
  public String getAlias() {
    return alias;
  }

  /**
   * A human readable string describing what this property is
   */
  public String getDescription() {
    return description;
  }

  /**
   * The column in which SQL search stores the value for this term.
   */
  public SearchField getField() {
    return field;
  }

  /**
   * Indicates for the REST API whether this field should be treated as an exact match, or whether we should use
   * partial matches of the field.
   */
  public boolean isExactMatch() {
    return exactMatch;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SearchMapping that = (SearchMapping) o;

    return Objects.equals(attribute, that.attribute) &&
        Objects.equals(alias, that.alias) &&
        Objects.equals(description, that.description) &&
        Objects.equals(field, that.field) &&
        Objects.equals(exactMatch, that.exactMatch) && filterType == that.filterType;
  }

  public FilterType getFilterType() {
    return filterType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, alias, description, field, exactMatch, filterType);
  }

  @Override
  public String toString() {
    return "SearchMapping{" +
        "attribute='" + attribute + '\'' +
        ", alias='" + alias + '\'' +
        ", description='" + description + '\'' +
        ", field='" + field + '\'' +
        ", exactMatch='" + exactMatch + '\'' +
        ", filterType=" + filterType +
        '}';
  }
}
