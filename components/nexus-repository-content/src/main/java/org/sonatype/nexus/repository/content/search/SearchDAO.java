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

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.content.ComponentSearch;
import org.sonatype.nexus.repository.content.store.AssetBlobDAO;
import org.sonatype.nexus.repository.content.store.AssetDAO;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;

import org.apache.ibatis.annotations.Param;

/**
 * @since 3.38
 */
@Expects({ContentRepositoryDAO.class, ComponentDAO.class, AssetDAO.class, AssetBlobDAO.class, ConfigurationDAO.class})
@SchemaTemplate("format")
public interface SearchDAO
    extends ContentDataAccess
{
  String FILTER_PARAMS = "filterParams";

  /**
   *
   * @param limit amount of rows in response
   * @param continuationToken optional token to continue from a previous request
   * @param filter optional filter to apply
   * @param values optional values map for filter (required if filter is not null)
   * @param isReverseOrder boolean flag to reverse soring order
   * @param sortColumnName column name to be used for search
   * @return collection of {@link SearchData} representing search results for given format and the next continuation token
   */
  Continuation<ComponentSearch> searchComponents(
      @Param("limit") int limit,
      @Nullable @Param("continuationToken") String continuationToken,
      @Nullable @Param("filter") String filter,
      @Nullable @Param(FILTER_PARAMS) final Map<String, String> values,
      @Param("isReverseOrder") boolean isReverseOrder,
      @Nullable @Param("sortColumnName") SearchViewColumns sortColumnName); //TODO to be revisited in the scope of NEXUS-29476

  /**
   * Count all {@link SearchData} in the given format.
   *
   * @return count of all {@link SearchData} in the given format
   */
  int count();
}
