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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One page of a component's distinct versions.
 */
@Schema(description = "One page of a component's distinct versions")
public class ComponentVersionsPageXO
{
  @Schema(description = "The versions on this page, ordered as requested")
  private List<ComponentVersionXO> items;

  @Schema(description = "Total number of distinct versions matching the query")
  private long total;

  @Schema(description = "Zero-based index of this page")
  private int page;

  @Schema(description = "Requested page size")
  private int size;

  public List<ComponentVersionXO> getItems() {
    return items;
  }

  public void setItems(final List<ComponentVersionXO> items) {
    this.items = items;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(final long total) {
    this.total = total;
  }

  public int getPage() {
    return page;
  }

  public void setPage(final int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(final int size) {
    this.size = size;
  }
}
