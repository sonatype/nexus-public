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
package org.sonatype.nexus.repository.content.store;

import java.time.OffsetDateTime;
import java.util.Map;

import org.sonatype.nexus.common.entity.ContinuationAware;
import org.sonatype.nexus.repository.content.AssetInfo;

/**
 * {@link AssetInfo} data backed by the content data store.
 *
 * @since 3.41
 */
public class AssetInfoData
    extends AbstractRepositoryContent
    implements AssetInfo, ContinuationAware
{
  private Integer assetId; // NOSONAR: internal id

  private Integer componentId;

  private String path;

  private String contentType;

  private String createdBy;

  private String createdByIp;

  private Map<String, String> checksums;

  private OffsetDateTime blobCreated;

  private OffsetDateTime lastDownloaded;

  private Long blobSize;

  private OffsetDateTime addedToRepository;

  @Override
  public String nextContinuationToken() {
    return Integer.toString(assetId);
  }

  @Override
  public Integer assetId() {
    return assetId;
  }

  @Override
  public Integer componentId() {
    return componentId;
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public String contentType() {
    return contentType;
  }

  @Override
  public String createdBy() {
    return createdBy;
  }

  @Override
  public String createdByIp() {
    return createdByIp;
  }

  @Override
  public OffsetDateTime lastDownloaded() {
    return lastDownloaded;
  }

  @Override
  public Long blobSize() {
    return blobSize;
  }

  @Override
  public Map<String, String> checksums() {
    return checksums;
  }

  @Override
  public OffsetDateTime blobCreated() {
    return blobCreated;
  }

  @Override
  public OffsetDateTime addedToRepository() {
    return addedToRepository;
  }

  @Override
  public String toString() {
    return "AssetInfoData{" +
        "assetId=" + assetId +
        ", componentId=" + componentId +
        ", path='" + path + '\'' +
        ", contentType='" + contentType + '\'' +
        ", createdBy='" + createdBy + '\'' +
        ", createdByIp='" + createdByIp + '\'' +
        ", checksums=" + checksums +
        ", blobCreated=" + blobCreated +
        ", lastDownloaded=" + lastDownloaded +
        ", blobSize=" + blobSize +
        ", addedToRepository=" + addedToRepository +
        '}';
  }
}
