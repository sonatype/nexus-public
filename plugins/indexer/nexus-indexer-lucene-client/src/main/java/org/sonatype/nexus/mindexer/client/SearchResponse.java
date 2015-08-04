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
package org.sonatype.nexus.mindexer.client;

import java.util.List;

import org.sonatype.nexus.client.internal.util.Check;

public class SearchResponse
{

  private final SearchRequest searchRequest;

  private final int totalCount;

  private final Integer from;

  private final Integer count;

  private final boolean tooManyResults;

  private final boolean collapsed;

  private final List<SearchResponseArtifact> hits;

  public SearchResponse(final SearchRequest searchRequest, final int totalCount, final Integer from,
                        final Integer count, final boolean tooManyResults, final boolean collapsed,
                        List<SearchResponseArtifact> hits)
  {
    this.searchRequest = Check.notNull(searchRequest, SearchRequest.class);
    this.totalCount = totalCount;
    this.from = from;
    this.count = count;
    this.tooManyResults = tooManyResults;
    this.collapsed = collapsed;
    this.hits = Check.notNull(hits, SearchResponseArtifact.class);
  }

  public SearchRequest getSearchRequest() {
    return searchRequest;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public Integer getFrom() {
    return from;
  }

  public Integer getCount() {
    return count;
  }

  public boolean isTooManyResults() {
    return tooManyResults;
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  public List<SearchResponseArtifact> getHits() {
    return hits;
  }

  /**
   * Prepares a SearchRequest that would retrieve a next page (in case of "paged" requests).
   */
  public SearchRequest getRequestForNextPage() {
    final int from = getFrom() != null ? getFrom() : 0;
    final int count = getCount() != null ? getCount() : getHits().size();
    return new SearchRequest(from + count, count, getSearchRequest().getRepositoryId(),
        getSearchRequest().getVersionexpand(), getSearchRequest().getCollapseresults(),
        getSearchRequest().getQuery());
  }
}
