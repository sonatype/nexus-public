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
package org.sonatype.nexus.repository.content.browse.internal;

import java.util.List;
import java.util.Locale;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;

import static java.util.stream.Collectors.joining;

public class BrowseNodeDAOQueryBuilder
{
  public static final String WHERE_PARAMS = "whereParams";

  public String findChildrenQuery(
      @Param("path") final String path,
      @Param("maxNodes") final int maxNodes,
      @Param("contentSelectors") final List<String> contentSelectors,
      final ProviderContext providerContext)
  {
    final String tableName = getFormatName(providerContext) + "_browse_node B";

    SQL sql = new SQL()
        .SELECT("B.*, L.parent_id IS NULL as leaf")
        .FROM(tableName)
        .LEFT_OUTER_JOIN("(SELECT DISTINCT parent_id FROM " + tableName + ") L ON B.browse_node_id = L.parent_id");

    if (path != null) {
      String innerTable = new SQL()
          .SELECT_DISTINCT("browse_node_id")
          .FROM(tableName)
          .WHERE("repository_id = #{repository.repositoryId}")
          .WHERE("path = #{path}")
          .toString();

      sql.WHERE("B.parent_id = (" + innerTable + ")");
    }
    else {
      sql.WHERE(" B.parent_id IS NULL")
         .WHERE(" B.repository_id = #{repository.repositoryId}");
    }

    if (contentSelectors != null && !contentSelectors.isEmpty()) {
      sql.AND();
      sql.WHERE(contentSelectors.stream().collect(joining(") or (", "(", ")")));
    }

    sql.LIMIT(maxNodes);

    return sql.toString();
  }

  private String getFormatName(final ProviderContext providerContext) {
    return providerContext.getMapperType().getSimpleName()
        .replaceAll(BrowseNodeDAO.class.getSimpleName(), "")
        .toLowerCase(Locale.ENGLISH);
  }
}
