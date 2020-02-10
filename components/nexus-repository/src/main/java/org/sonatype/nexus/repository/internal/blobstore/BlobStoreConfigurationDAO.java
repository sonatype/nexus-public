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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Collection;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.datastore.api.IterableDataAccess;

/**
 * {@link BlobStoreConfigurationData} access.
 *
 * @since 3.21
 */
public interface BlobStoreConfigurationDAO
    extends IterableDataAccess.WithName<BlobStoreConfigurationData>
{
  /**
   * Find candidate configurations that might have the given name as a member.
   *
   * These candidates will then be filtered in the config store to find the exact match.
   * This way we can use a simple filter in the DB when JSON filtering is not available
   * without needing to have additional tables or denormalized attributes.
   */
  Collection<BlobStoreConfiguration> findCandidateParents(String name);
}
