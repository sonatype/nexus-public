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

import java.util.Map;

import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.hash.HashCode;

/**
 * Blob store utilities.
 *
 * @since 3.15
 */
public interface BlobStoreUtil
{
  /**
   * Get a count of the repositories using a blob store.
   *
   * @param blobStoreId blob store id
   * @return number of repositories using the blob store
   */
  int usageCount(String blobStoreId);

  /**
   * Returns true if the file path is valid. The path is valid if all the folder names in the path are less than the
   * given maximum.
   * 
   * @since 3.20
   * @param filePath A file path
   * @param maxLength The max length
   * @return True if valid, false otherwise
   */
  boolean validateFilePath(String filePath, int maxLength);

  Map<HashAlgorithm, HashCode> toHashObjects(final Map<String, String> checksums);
}
