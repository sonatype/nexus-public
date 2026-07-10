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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.30
 */
public class RepositoryDetailXO
{
  private final String name;

  private final String type;

  private final String format;

  private final String url;

  private final RepositoryStatusXO status;

  private Long size;

  private Long componentCount;

  private Long assetCount;

  private String blobStoreName;

  public RepositoryDetailXO(
      final String name,
      final String type,
      final String format,
      final String url,
      final RepositoryStatusXO status)
  {
    this.name = checkNotNull(name);
    this.type = checkNotNull(type);
    this.format = checkNotNull(format);
    this.url = checkNotNull(url);
    this.status = checkNotNull(status);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getFormat() {
    return format;
  }

  public String getUrl() {
    return url;
  }

  public RepositoryStatusXO getStatus() {
    return status;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(final Long size) {
    this.size = size;
  }

  public Long getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(final Long componentCount) {
    this.componentCount = componentCount;
  }

  public Long getAssetCount() {
    return assetCount;
  }

  public void setAssetCount(final Long assetCount) {
    this.assetCount = assetCount;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }

  public void setBlobStoreName(final String blobStoreName) {
    this.blobStoreName = blobStoreName;
  }
}
