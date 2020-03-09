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

  public SearchMapping(final String alias, final String attribute, final String description) {
    this.alias = checkNotNull(alias);
    this.attribute = checkNotNull(attribute);
    this.description = checkNotNull(description);
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SearchMapping that = (SearchMapping) o;

    if (!attribute.equals(that.attribute)) {
      return false;
    }
    if (!alias.equals(that.alias)) {
      return false;
    }
    return description.equals(that.description);
  }

  @Override
  public int hashCode() {
    int result = attribute.hashCode();
    result = 31 * result + alias.hashCode();
    result = 31 * result + description.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "SearchMapping{" +
        "attribute='" + attribute + '\'' +
        ", alias='" + alias + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
