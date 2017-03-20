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
package org.sonatype.nexus.repository.browse.internal;

import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Build where clause for asset select queries.
 *
 * @since 3.3
 */
public class AssetWhereClauseBuilder
{
  private AssetWhereClauseBuilder() {
  }

  public static String whereClause(final String content, final boolean includeFilter, final boolean includeLastId) {
    String whereClause = content;
    if (includeFilter) {
      whereClause += " AND " + P_NAME + " LIKE :nameFilter";
    }
    if (includeLastId) {
      whereClause += " AND @RID > :rid";
    }
    return whereClause;
  }

  public static String whereClause(final String content, final boolean includeFilter) {
    return whereClause(content, includeFilter, false);
  }
}
