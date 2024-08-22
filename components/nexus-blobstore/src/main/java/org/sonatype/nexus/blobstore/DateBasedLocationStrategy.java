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
package org.sonatype.nexus.blobstore;

import java.time.OffsetDateTime;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.time.UTC;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores blobs in a date-based directory tree.
 * <p>
 * For example, {@code 2024/08/23/hours/minutes/blobId}
 */
public class DateBasedLocationStrategy
    extends LocationStrategySupport
{
  @Override
  public String location(final BlobId blobId) {
    checkNotNull(blobId);

    OffsetDateTime blobCreationTime = blobId.getBlobCreationTime();
    if (blobCreationTime == null) {
      blobCreationTime = UTC.now();
    }
    return String.format("%04d/%02d/%02d/%02d/%02d/%s",
        blobCreationTime.getYear(),
        blobCreationTime.getMonthValue(),
        blobCreationTime.getDayOfMonth(),
        blobCreationTime.getHour(),
        blobCreationTime.getMinute(),
        escapeFilename(blobId.asUniqueString()));
  }
}
