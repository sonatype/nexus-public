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
import java.time.format.DateTimeFormatter;

import org.sonatype.nexus.blobstore.api.BlobId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores blobs in a date-based directory tree.
 * <p>
 * For example, {@code yyyy/mm/dd/hours/minutes/blobId}
 */
public class DateBasedLocationStrategy
    extends LocationStrategySupport
{
  private static final Logger log = LoggerFactory.getLogger(DateBasedLocationStrategy.class);

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss");

  @Override
  public String location(final BlobId blobId) {
    checkNotNull(blobId);

    OffsetDateTime blobCreationTime = blobId.getBlobCreated();
    if (blobCreationTime == null) {
      log.error("BlobId {} has no creation time", blobId);
      throw new RuntimeException("Blob can't be found: " + blobId);
    }
    String datePath = blobCreationTime.format(DATE_TIME_FORMATTER);
    return datePath + "/" + escapeFilename(blobId.asUniqueString());
  }
}
