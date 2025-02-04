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
package org.sonatype.nexus.blobstore.quota;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Holds result for the evaluation of {@link BlobStoreQuota}.
 *
 * @since 3.14
 */
public class BlobStoreQuotaResult
{
  private final boolean isViolation;

  private final String humanReadableMessage;

  private final String blobStoreName;

  public BlobStoreQuotaResult(
      final boolean isViolation,
      final String blobStoreName,
      final String humanReadableMessage)
  {
    this.isViolation = isViolation;
    this.blobStoreName = checkNotNull(blobStoreName);
    this.humanReadableMessage = checkNotNull(humanReadableMessage);
  }

  public boolean isViolation() {
    return this.isViolation;
  }

  public String getMessage() {
    return this.humanReadableMessage;
  }

  public String getBlobStoreName() {
    return this.blobStoreName;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "isViolation='" + isViolation + '\'' +
        ", message='" + humanReadableMessage + '\'' +
        ", blobStoreName='" + blobStoreName + '\'' +
        '}';
  }
}
