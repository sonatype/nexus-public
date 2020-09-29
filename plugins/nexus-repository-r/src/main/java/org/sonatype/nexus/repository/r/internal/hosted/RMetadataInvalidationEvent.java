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
package org.sonatype.nexus.repository.r.internal.hosted;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event dispatched when R metadata (PACKAGES files) should be invalidated and rebuilt.
 *
 * @since 3.28
 */
public class RMetadataInvalidationEvent
{
  /**
   * The repository name for the metadata invalidation.
   */
  private final String repositoryName;

  /**
   * The base path (without filename) to invalidate the PACKAGES content for.
   */
  private final String basePath;

  /**
   * Constructor
   *
   * @param repositoryName The repository name for the metadata invalidation.
   * @param basePath       The base path (without filename) of the path to invalidate the PACKAGES content for.
   */
  public RMetadataInvalidationEvent(final String repositoryName, final String basePath) {
    this.repositoryName = checkNotNull(repositoryName);
    this.basePath = checkNotNull(basePath);
  }

  /**
   * Returns the repository name associated with the metadata invalidation request.
   *
   * @return the repository name for the invalidation
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /**
   * Returns the base path (without filename) to invalidate the PACKAGES content for.
   *
   * @return the base path for the invalidation
   */
  public String getBasePath() {
    return basePath;
  }
}
