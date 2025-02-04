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
package org.sonatype.nexus.repository.content;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.ContinuationAware;

public class AssetReconcileData
    implements ContinuationAware
{
  private BlobRef blobRef;

  private String repository;

  private String path;

  public Integer getAssetBlobId() {
    return assetBlobId;
  }

  private Integer assetBlobId;

  public BlobRef getBlobRef() {
    return blobRef;
  }

  public void setBlobRef(final BlobRef blobRef) {
    this.blobRef = blobRef;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(final String repository) {
    this.repository = repository;
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  @Override
  public String nextContinuationToken() {
    return Integer.toString(assetBlobId);
  }

  public void setAssetBlobId(final Integer assetBlobId) {
    this.assetBlobId = assetBlobId;
  }
}
