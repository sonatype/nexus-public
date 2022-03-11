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

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.Expects;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.content.SearchResult;
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
   * Search components in the scope of one format.
   *
   * @param request DTO containing all required params for search
   * @return collection of {@link SearchResultData} representing search results for a given format and the next
   * continuation token
   */
  Collection<SearchResult> searchComponents(SqlSearchRequest request);

  /**
   * Count all {@link SearchResultData} in the given format.
   *
   * @return count of all {@link SearchResultData} in the given format
   */
  int count();
}
