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
package org.sonatype.nexus.repository.content.search.table;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.repository.content.SearchResult;
import org.sonatype.nexus.repository.content.search.SearchResultData;
import org.sonatype.nexus.repository.content.search.SqlSearchRequest;

/**
 * DAO for access search table entries
 */
public interface SearchTableDAO
    extends ContentDataAccess
{
  /**
   * Count components in the given format.
   *
   * @param filter optional filter to apply
   * @param values optional values map for filter (required if filter is not null)
   * @return number of found components.
   */
  int count(@Nullable String filter, @Nullable Map<String, String> values);

  /**
   * Search components in the scope of one format.
   *
   * @param request DTO containing all required params for search
   * @return collection of {@link SearchResultData} representing search results for a given format.
   */
  Collection<SearchResult> searchComponents(SqlSearchRequest request);
}
