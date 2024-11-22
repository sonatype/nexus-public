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
package org.sonatype.nexus.blobstore.group;

import org.sonatype.nexus.blobstore.api.BlobStore;

/**
 * Blob store group service.
 *
 * @since 3.15
 */
public interface BlobStoreGroupService
{
  /**
   * Are blob store groups enabled, i.e. can they be created and modified?
   */
  boolean isEnabled();

  /**
   * Takes a {@link BlobStore} and creates a {@link BlobStoreGroup} that contains the original blob store
   *
   * @param from a {@link BlobStore} to be converted into a group blob store
   * @param newNameForOriginal a new name for the original blob store
   * @return {@link BlobStoreGroup} that contains the original blob store
   */
  BlobStoreGroup convert(final BlobStore from, final String newNameForOriginal);
}
