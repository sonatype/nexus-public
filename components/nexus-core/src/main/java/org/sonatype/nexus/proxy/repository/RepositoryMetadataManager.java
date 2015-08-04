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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

/**
 * Repository meta data manager generalization. Encapsulates some "generic" meta data related operations. Not all
 * methods might "make sense" for given implementation, what is actual semantics of them, depends on the underlying
 * implementation.
 *
 * @author cstamas
 * @since 2.1
 */
public interface RepositoryMetadataManager
{
  /**
   * Forces recreation of repository meta data, if any, from given path and below (if applicable).
   *
   * @return {@code true} if recreation did run and did successfully finished.
   */
  boolean recreateMetadata(ResourceStoreRequest request);

  /**
   * Expires all the caches used by this repository's meta data, if any, from given path and below (if applicable).
   * Similar to {@link Repository#expireCaches(ResourceStoreRequest)} method, but limited to repository meta data
   * items only, with same behavior. For details, see referenced method. Also, on group repositories, this call is
   * propagated to it's member repositories!
   *
   * @param path a path from to start descending. If null, it is taken as "root".
   * @return {@code true} if cache was altered (expired) with this invocation.
   */
  boolean expireMetadataCaches(ResourceStoreRequest request);

  /**
   * Purges the NFC caches used by this repository's meta data, if any, from path and below (if applicable).
   *
   * @return {@code true} if cache was altered (expired) with this invocation.
   */
  boolean expireNotFoundMetadataCaches(ResourceStoreRequest request);
}
