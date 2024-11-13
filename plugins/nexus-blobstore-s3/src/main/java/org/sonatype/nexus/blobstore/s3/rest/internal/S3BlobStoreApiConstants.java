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

package org.sonatype.nexus.blobstore.s3.rest.internal;

/**
 * Constants for S3 blobstore REST APIs.
 */
public class S3BlobStoreApiConstants
{
  private S3BlobStoreApiConstants() { }

  public static final String NOT_AN_S3_BLOB_STORE_MSG_FORMAT = "\"%s is not an S3 blob store.\"";

  public static final String BLOB_STORE_NAME_UPDATE_ERROR_MESSAGE = "Renaming an S3 blob store name is not supported";

  public static final String NON_EXISTENT_BLOB_STORE_ERROR_MESSAGE_FORMAT = "No S3 blob store called '%s'";

  public static final String BLOB_STORE_TYPE_MISMATCH_ERROR_FORMAT = "Blob store %s is not an S3 blob store";

  static final String DUPLICATE_REGIONS_ERROR_MESSAGE = "More than one failover bucket defined for a single region.";

  static final String FAILOVER_DEFAULT_ERROR_MESSAGE = "Failovers may not contain 'default' as a region";

  static final String MATCHES_PRIMARY_ERROR_MESSAGE = "Failover region duplicates primary region";

}
