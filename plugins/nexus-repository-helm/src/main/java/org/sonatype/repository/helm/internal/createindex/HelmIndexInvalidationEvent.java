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
package org.sonatype.repository.helm.internal.createindex;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event class for use when invalidating Helm index.yaml file
 *
 * @since 3.next
 */
public class HelmIndexInvalidationEvent
{
  private final String repositoryName;

  private final boolean waitBeforeRebuild;

  public HelmIndexInvalidationEvent(final String repositoryName,
                                    final boolean waitBeforeRebuild) {
    this.repositoryName = checkNotNull(repositoryName);
    this.waitBeforeRebuild = waitBeforeRebuild;
  }

  /**
   * The helm repository that requires its index invalidated and rebuilt.
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /**
   * If set then force createIndex to wait before generating to prevent multiple re-writes being scheduled one after
   * another
   */
  public boolean isWaitBeforeRebuild() {
    return waitBeforeRebuild;
  }
}
