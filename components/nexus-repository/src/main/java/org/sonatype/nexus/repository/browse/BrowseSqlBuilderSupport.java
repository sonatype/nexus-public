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
package org.sonatype.nexus.repository.browse;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.entity.EntityAdapter;
import org.sonatype.nexus.repository.query.QueryOptions;

import static org.sonatype.nexus.repository.browse.internal.SuffixSqlBuilder.buildSuffix;

/**
 * @since 3.7
 */
public abstract class BrowseSqlBuilderSupport
    extends ComponentSupport
{
  protected abstract EntityAdapter<?> getEntityAdapter();

  protected abstract String getBrowseIndex();

  protected StringBuilder buildBase(QueryOptions queryOptions) {
    StringBuilder queryBuilder = new StringBuilder("SELECT FROM ");

    if ("id".equals(queryOptions.getSortProperty())) {
      queryBuilder.append(getEntityAdapter().getTypeName());
    }
    else {
      queryBuilder.append("INDEXVALUES");
      if (queryOptions.getSortDirection() != null) {
        queryBuilder.append(queryOptions.getSortDirection());
      }
      queryBuilder.append(":").append(getBrowseIndex());
    }

    return queryBuilder;
  }

  protected String buildQuerySuffix(final QueryOptions queryOptions) {
    return buildSuffix(queryOptions);
  }
}
