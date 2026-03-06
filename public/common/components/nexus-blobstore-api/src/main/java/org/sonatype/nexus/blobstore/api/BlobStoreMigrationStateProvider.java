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
 * Provides information about active blob store migrations.
 *
 * This interface allows blob stores to determine if a migration is currently in progress,
 * enabling optimizations like conditional existence checking only during migration windows.
 *
 * @since 3.next
 */
public interface BlobStoreMigrationStateProvider
{
  /**
   * Check if any blob store migration is currently active.
   *
   * @return true if at least one blob store migration is in progress, false otherwise
   */
  boolean isMigrationActive();

  /**
   * Check if a specific blob store is currently being migrated from or to.
   *
   * @param blobStoreName the name of the blob store to check
   * @return true if the specified blob store is involved in an active migration, false otherwise
   */
  boolean isBlobStoreMigrating(String blobStoreName);
}
