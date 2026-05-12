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
package org.sonatype.nexus.audit.internal.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response wrapper for audit log REST API containing items, pagination metadata, and summary statistics.
 */
public class AuditLogResponseXO
{
  private final List<AuditEventXO> items;

  private final PaginationXO pagination;

  @JsonCreator
  public AuditLogResponseXO(
      @JsonProperty("items") final List<AuditEventXO> items,
      @JsonProperty("pagination") final PaginationXO pagination)
  {
    this.items = items;
    this.pagination = pagination;
  }

  public List<AuditEventXO> getItems() {
    return items;
  }

  public PaginationXO getPagination() {
    return pagination;
  }

  public static class PaginationXO
  {
    private final int totalItems;

    private final int totalPages;

    private final int currentPage;

    private final int itemsPerPage;

    @JsonCreator
    public PaginationXO(
        @JsonProperty("totalItems") final int totalItems,
        @JsonProperty("totalPages") final int totalPages,
        @JsonProperty("currentPage") final int currentPage,
        @JsonProperty("itemsPerPage") final int itemsPerPage)
    {
      this.totalItems = totalItems;
      this.totalPages = totalPages;
      this.currentPage = currentPage;
      this.itemsPerPage = itemsPerPage;
    }

    public int getTotalItems() {
      return totalItems;
    }

    public int getTotalPages() {
      return totalPages;
    }

    public int getCurrentPage() {
      return currentPage;
    }

    public int getItemsPerPage() {
      return itemsPerPage;
    }
  }
}
