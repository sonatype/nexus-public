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
package org.sonatype.nexus.repository.rest.internal.api;

import java.util.List;

/**
 * Paginated response for repository details with server-side filtering support.
 * Enables enterprise-scale browse functionality with efficient data transfer.
 */
public class RepositoryDetailPageXO
{
  private final List<RepositoryDetailXO> data;

  private final int totalCount;

  private final int page;

  private final int pageSize;

  public RepositoryDetailPageXO(
      final List<RepositoryDetailXO> data,
      final int totalCount,
      final int page,
      final int pageSize)
  {
    this.data = data;
    this.totalCount = totalCount;
    this.page = page;
    this.pageSize = pageSize;
  }

  public List<RepositoryDetailXO> getData() {
    return data;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalPages() {
    return (int) Math.ceil((double) totalCount / pageSize);
  }

  public boolean hasNextPage() {
    return page < getTotalPages();
  }

  public boolean hasPreviousPage() {
    return page > 1;
  }
}
