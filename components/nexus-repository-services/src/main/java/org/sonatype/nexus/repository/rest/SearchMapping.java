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

import org.sonatype.nexus.repository.rest.sql.UnsupportedSearchField;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes the mapping of an Elasticsearch attribute to an alias and also provides a description of the
 * attribute.
 *
 * @since 3.7
 */
public class SearchMapping
{
  private final String attribute;

  private final String alias;

  private final String description;

  private final SearchFieldSupport field;

  public SearchMapping(
      final String alias,
      final String attribute,
      final String description,
      final SearchFieldSupport field)
  {
    this.alias = checkNotNull(alias);
    this.attribute = checkNotNull(attribute);
    this.description = checkNotNull(description);
    this.field = checkNotNull(field);
  }

  public SearchMapping(final String alias, final String attribute, final String description) {
    this(alias, attribute, description, UnsupportedSearchField.INSTANCE);
  }

  public String getAttribute() {
    return attribute;
  }

  public String getAlias() {
    return alias;
  }

  public String getDescription() {
    return description;
  }

  public SearchFieldSupport getField() {
    return field;
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
        Objects.equals(field, that.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, alias, description, field);
  }

  @Override
  public String toString() {
    return "SearchMapping{" +
        "attribute='" + attribute + '\'' +
        ", alias='" + alias + '\'' +
        ", description='" + description + '\'' +
        ", field='" + field + '\'' +
        '}';
  }
}
