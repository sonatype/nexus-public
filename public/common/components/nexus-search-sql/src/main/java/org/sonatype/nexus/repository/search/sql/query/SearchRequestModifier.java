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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.search.SearchRequest;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SqlSearchQueryContribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import org.springframework.stereotype.Component;

@Component
@Singleton
public class SearchRequestModifier
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private Map<String, SqlSearchQueryContribution> handlers;

  @Inject
  public SearchRequestModifier(final List<SqlSearchQueryContribution> handlersList) {
    this.handlers = QualifierUtil.buildQualifierBeanMap(checkNotNull(handlersList));
  }

  /**
   * Modifies the provided {@link SearchRequest} based on the search filters,
   * excluding the repository name filter. If the filter is not present, or it does not modify the request,
   * the original request is returned.
   */
  public SearchRequest modify(final SearchRequest request) {
    SearchRequest updatedSearchRequest = request;
    for (SearchFilter filter : request.getSearchFilters()) {
      if (filter != null && !REPOSITORY_NAME.equalsIgnoreCase(filter.getProperty())) {
        log.debug("Modifying request for {}", filter);
        updatedSearchRequest = modifyRequest(filter, updatedSearchRequest);
      }
    }
    return updatedSearchRequest;
  }

  /**
   * Modifies the provided {@link SearchRequest} based on the provided {@link SearchFilter}.
   */
  private SearchRequest modifyRequest(final SearchFilter filter, SearchRequest request) {
    SqlSearchQueryContribution queryContribution = Optional.ofNullable(filter.getProperty())
        .map(handlers::get)
        .orElse(handlers.get("default"));
    return queryContribution.modifyRequest(request);
  }
}
