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
package org.sonatype.nexus.blobstore.file;

import java.util.Map;

import org.sonatype.nexus.blobstore.api.BlobMetrics;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Metadata about a blob content, including headers, metrics and deleted status.
 *
 * @since 3.0
 */
public class FileBlobMetadata
{
  private FileBlobState blobState;

  private final Map<String, String> headers;

  private BlobMetrics metrics;

  public FileBlobMetadata(final FileBlobState blobState, final Map<String, String> headers) {
    this.blobState = checkNotNull(blobState);
    this.headers = checkNotNull(headers);
  }

  public FileBlobState getBlobState() {
    return blobState;
  }

  public void setBlobState(final FileBlobState blobState) {
    this.blobState = blobState;
  }

  public boolean isAlive() {
    return FileBlobState.ALIVE.equals(blobState);
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setMetrics(final BlobMetrics metrics) {
    this.metrics = metrics;
  }

  public BlobMetrics getMetrics() {
    return metrics;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "blobState=" + blobState +
        ", headers=" + headers +
        ", metrics=" + metrics +
        '}';
  }
}
