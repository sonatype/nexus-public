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
package org.sonatype.nexus.repository.search;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.15
 */
public class SearchFilter
{
  private final String property;

  private final String value;

  public SearchFilter(final String property, final String value) {
    this.property = checkNotNull(property);
    this.value = checkNotNull(value);
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchFilter that = (SearchFilter) o;
    return Objects.equals(property, that.property) &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(property, value);
  }

  @Override
  public String toString() {
    return "SearchFilter{" +
        "property='" + property + '\'' +
        ", value='" + value + '\'' +
        '}';
  }
}
