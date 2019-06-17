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

/**
 * @since 3.4
 */
public final class BlobAttributesConstants
{
  public static final String HEADER_PREFIX = "@";

  public static final String SHA1_HASH_ATTRIBUTE = "sha1";

  public static final String CONTENT_SIZE_ATTRIBUTE = "size";

  public static final String CREATION_TIME_ATTRIBUTE = "creationTime";

  public static final String DELETED_ATTRIBUTE = "deleted";

  public static final String DELETED_REASON_ATTRIBUTE = "deletedReason";

  public static final String DELETED_DATETIME_ATTRIBUTE = "deletedDateTime";

  private BlobAttributesConstants() {}
}
