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
package org.sonatype.nexus.blobstore.rest;

import org.sonatype.nexus.blobstore.quota.internal.SpaceRemainingQuota;
import org.sonatype.nexus.blobstore.quota.internal.SpaceUsedQuota;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

/**
 * @since 3.19
 */
public class BlobStoreApiSoftQuota
{
  @NotBlank
  @ApiModelProperty("The type to use such as " + SpaceRemainingQuota.ID + ", or " + SpaceUsedQuota.ID)
  private String type;

  @Range
  @ApiModelProperty("The limit in MB.")
  private Long limit;

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public Long getLimit() {
    return limit;
  }

  public void setLimit(final Long limit) {
    this.limit = limit;
  }
}
