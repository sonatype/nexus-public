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

import org.sonatype.nexus.client.internal.util.Check;

public class SearchRequest
{

  private final Integer from;

  private final Integer count;

  private final String repositoryId;

  private final Boolean versionexpand;

  private final Boolean collapseresults;

  private final Query query;

  public SearchRequest(final Query query) {
    this(null, query);
  }

  public SearchRequest(final String repositoryId, final Query query) {
    this(null, null, repositoryId, query);
  }

  public SearchRequest(final Integer from, final Integer count, final String repositoryId, final Query query) {
    this(from, count, repositoryId, null, null, query);
  }

  public SearchRequest(final Integer from, final Integer count, final String repositoryId,
                       final Boolean versionexpand, final Boolean collapseresults, final Query query)
  {
    this.from = from;
    this.count = count;
    this.repositoryId = repositoryId;
    this.versionexpand = versionexpand;
    this.collapseresults = collapseresults;
    this.query = Check.notNull(query, Query.class);
  }

  public Integer getFrom() {
    return from;
  }

  public Integer getCount() {
    return count;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public Boolean getVersionexpand() {
    return versionexpand;
  }

  public Boolean getCollapseresults() {
    return collapseresults;
  }

  public Query getQuery() {
    return query;
  }
}
