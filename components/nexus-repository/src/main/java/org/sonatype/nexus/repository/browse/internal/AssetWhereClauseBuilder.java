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

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

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

  @Nullable
  public static String whereClause(@Nullable final String content, final boolean includeFilter,
                                   final boolean includeLastId)
  {
    List<String> clauses = Lists.newArrayList();
    if (!Strings2.isBlank(content)) {
      clauses.add(content);
    }
    if (includeFilter) {
      clauses.add(P_NAME + " LIKE :nameFilter");
    }
    if (includeLastId) {
      clauses.add("@rid > :rid");
    }
    if (!clauses.isEmpty()) {
      return Joiner.on(" AND ").join(clauses);
    }
    else {
      return null;
    }
  }

  @Nullable
  public static String whereClause(final String content, final boolean includeFilter) {
    return whereClause(content, includeFilter, false);
  }
}
