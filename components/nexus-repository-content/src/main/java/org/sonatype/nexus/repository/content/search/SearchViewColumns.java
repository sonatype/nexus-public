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
package org.sonatype.nexus.repository.content.search;

import java.util.Objects;

/**
 * Enum representing column names for {@link SearchDAO}
 *
 * @since 3.38
 */
public enum SearchViewColumns
{
  COMPONENT_ID("componentId"),
  NAMESPACE("namespace"),
  SEARCH_COMPONENT_NAME("componentName"),
  VERSION("version"),
  NORMALISED_VERSION("normalised_version"),
  REPOSITORY_NAME("repositoryName"),
  FORMAT("format");

  private final String name;

  SearchViewColumns(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static SearchViewColumns fromSortFieldName(final String name) {
    if (Objects.isNull(name)) {
      return COMPONENT_ID;
    }
    switch (name) {
      case "group":
        return NAMESPACE;
      case "id":
        return COMPONENT_ID;
      case "name":
        return SEARCH_COMPONENT_NAME;
      case "version":
        return NORMALISED_VERSION;
      default:
        return COMPONENT_ID;
    }
  }
}
