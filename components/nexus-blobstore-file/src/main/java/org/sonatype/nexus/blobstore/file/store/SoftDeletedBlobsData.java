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
package org.sonatype.nexus.blobstore.file.store;

import java.time.OffsetDateTime;

import org.sonatype.nexus.common.entity.ContinuationAware;

/**
 * Data object for the soft deleted blobs table.
 */
public class SoftDeletedBlobsData
    implements ContinuationAware
{
  Integer recordId; // NOSONAR: internal id

  private String blobId;

  private String sourceBlobStoreName;

  private OffsetDateTime deletedDate;

  private OffsetDateTime datePathRef;

  public String getSourceBlobStoreName() {
    return sourceBlobStoreName;
  }

  public void setSourceBlobStoreName(final String sourceBlobStoreName) {
    this.sourceBlobStoreName = sourceBlobStoreName;
  }

  public OffsetDateTime getDeletedDate() {
    return deletedDate;
  }

  public void setDeletedDate(final OffsetDateTime deletedDate) {
    this.deletedDate = deletedDate;
  }

  public String getBlobId() {
    return blobId;
  }

  public OffsetDateTime getDatePathRef() {
    return datePathRef;
  }

  public void setDatePathRef(final OffsetDateTime datePathRef) {
    this.datePathRef = datePathRef;
  }

  @Override
  public String nextContinuationToken() {
    return Integer.toString(recordId);
  }
}
