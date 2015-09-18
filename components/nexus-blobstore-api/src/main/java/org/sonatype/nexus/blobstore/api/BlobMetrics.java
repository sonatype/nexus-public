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
package org.sonatype.nexus.blobstore.api;

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * Provides basic metrics about a BLOB.
 *
 * @since 3.0
 */
public class BlobMetrics
  implements Serializable
{
  private final DateTime creationTime;

  private final String SHA1Hash;

  private final long contentSize;

  public BlobMetrics(final DateTime creationTime, final String SHA1Hash, final long contentSize) {
    this.creationTime = creationTime;
    this.SHA1Hash = SHA1Hash;
    this.contentSize = contentSize;
  }

  public DateTime getCreationTime() {
    return creationTime;
  }

  public String getSHA1Hash() {
    return SHA1Hash;
  }

  public long getContentSize() {
    return contentSize;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "creationTime=" + creationTime +
        ", SHA1Hash='" + SHA1Hash + '\'' +
        ", contentSize=" + contentSize +
        '}';
  }
}
