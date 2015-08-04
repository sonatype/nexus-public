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
package org.sonatype.nexus.maven.tasks;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encapsulate parameters to {@link ReleaseRemover}
 *
 * @since 2.5
 */
public class ReleaseRemovalRequest
{
  private final String repositoryId;

  private final int numberOfVersionsToKeep;

  private final boolean useIndex;

  private final String targetId;

  /**
   * @param repositoryId           the repository to target
   * @param numberOfVersionsToKeep the number of released versions to keep for Group/Artifact in the repository
   * @param useIndex               use index instead of walker
   * @param targetId               (optional) Repository Target id to be applied
   */
  public ReleaseRemovalRequest(final String repositoryId,
                               final int numberOfVersionsToKeep,
                               final boolean useIndex,
                               final @Nullable String targetId)
  {
    this.repositoryId = checkNotNull(repositoryId);
    this.numberOfVersionsToKeep = checkNotNull(numberOfVersionsToKeep);
    this.useIndex = useIndex;
    this.targetId = targetId;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public int getNumberOfVersionsToKeep() {
    return numberOfVersionsToKeep;
  }

  public boolean isUseIndex() {
    return useIndex;
  }

  @Nullable
  public String getTargetId() {
    return targetId;
  }
}
