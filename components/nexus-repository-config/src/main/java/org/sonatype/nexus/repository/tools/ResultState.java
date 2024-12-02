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
package org.sonatype.nexus.repository.tools;

/**
 * Possible error states that can be detected by {@link DeadBlobFinder}.
 * 
 * @since 3.3
 */
public enum ResultState
{
  ASSET_DELETED, // DB record was deleted during inspection
  DELETED, // DB record references blobRef which has since been deleted
  MISSING_BLOB_REF, // DB record has no blobRef
  SHA1_DISAGREEMENT, // DB record and blob have different SHA1
  UNAVAILABLE_BLOB, // blob has an inputstream that reports 0 when calling isAvailable()
  UNKNOWN,
  UNREADABLE_BLOB, // Unable to read blob from disk
}
