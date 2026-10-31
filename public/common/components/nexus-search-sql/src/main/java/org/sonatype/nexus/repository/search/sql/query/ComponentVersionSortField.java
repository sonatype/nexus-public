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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Arrays;
import java.util.Optional;

import org.sonatype.nexus.repository.search.ComponentVersionSortKeys;

/**
 * The sort keys the version-browsing API accepts, and the SQL each maps to.
 *
 * <p>
 * The keys are defined canonically in {@link ComponentVersionSortKeys}. This enum maps
 * from those canonical keys to the SQL expressions interpolated into SearchTableDAO.xml.
 * Caller-supplied text must never reach that interpolation.
 */
public enum ComponentVersionSortField
{
  VERSION(ComponentVersionSortKeys.VERSION, "cs.normalised_version"),

  LAST_UPDATED(ComponentVersionSortKeys.LAST_UPDATED, "MAX(cs.last_modified)"),

  REPOSITORIES(ComponentVersionSortKeys.REPOSITORIES, "COUNT(DISTINCT cs.search_repository_name)");

  private final String key;

  private final String sqlExpression;

  ComponentVersionSortField(final String key, final String sqlExpression) {
    this.key = key;
    this.sqlExpression = sqlExpression;
  }

  public String key() {
    return key;
  }

  public String sqlExpression() {
    return sqlExpression;
  }

  public static Optional<ComponentVersionSortField> fromKey(final String key) {
    return Arrays.stream(values()).filter(field -> field.key.equals(key)).findFirst();
  }
}
