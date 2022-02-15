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
import java.util.Objects;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.ComponentSearch;
import org.sonatype.nexus.repository.content.store.ContentStoreSupport;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryCondition;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

/**
 * @since 3.next
 */
@Named
public class SearchStore<T extends SearchDAO>
    extends ContentStoreSupport<T>
{
  @Inject
  public SearchStore(
      final DataSessionSupplier sessionSupplier,
      @Assisted final String contentStoreName,
      @Assisted final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
  }

  /**
   * Browse all components that match the given filters
   *
   * @param limit             maximum number of components to return
   * @param continuationToken optional token to continue from a previous request
   * @param filterQuery       optional filter to apply
   * @return collection of components and the next continuation token
   * @see Continuation#nextContinuationToken()
   * @since 3.next
   */
  @Transactional
  public Continuation<ComponentSearch> searchComponents(
      final int limit,
      @Nullable final String continuationToken,
      @Nullable final SqlSearchQueryCondition filterQuery,
      final boolean isReverseOrder,
      final SearchViewColumns sortColumnName)
  {
    String filterFormat = null;
    Map<String, String> formatValues = null;
    if (Objects.nonNull(filterQuery)) {
      filterFormat = filterQuery.getSqlConditionFormat();
      formatValues = filterQuery.getValues();
    }

    return dao().searchComponents(limit, continuationToken, filterFormat, formatValues, isReverseOrder, sortColumnName);
  }

  public int count()
  {
    return dao().count();
  }
}
