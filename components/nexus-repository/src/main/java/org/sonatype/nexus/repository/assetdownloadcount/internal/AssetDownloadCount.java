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
package org.sonatype.nexus.repository.assetdownloadcount.internal;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.repository.assetdownloadcount.DateType;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Count of asset downloads at specified time frame.
 *
 * @since 3.3
 */
public class AssetDownloadCount
    extends AbstractEntity
{
  private String repositoryName;

  private String assetName = "";

  private String nodeId;

  private long count;

  private DateType dateType;

  private DateTime date;

  public AssetDownloadCount withRepositoryName(final String repositoryName) {
    checkNotNull(repositoryName);
    this.repositoryName = repositoryName;
    return this;
  }

  public AssetDownloadCount withAssetName(final String assetName) {
    checkNotNull(assetName);
    this.assetName = assetName;
    return this;
  }

  public AssetDownloadCount withNodeId(final String nodeId) {
    checkNotNull(nodeId);
    this.nodeId = nodeId;
    return this;
  }

  public AssetDownloadCount withCount(final long count) {
    this.count = count;
    return this;
  }

  public AssetDownloadCount withDateType(final DateType dateType) {
    checkNotNull(dateType);
    this.dateType = dateType;
    return this;
  }

  public AssetDownloadCount withDate(final DateTime date) {
    checkNotNull(date);
    this.date = date;
    return this;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getAssetName() {
    return assetName;
  }

  public String getNodeId() {
    return nodeId;
  }

  public long getCount() {
    return count;
  }

  public DateType getDateType() {
    return dateType;
  }

  public DateTime getDate() {
    return date;
  }
}
