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
package org.sonatype.nexus.repository.storage;

import org.sonatype.nexus.common.entity.Entity;

/**
 * A logical container of components and assets.
 *
 * @since 3.0
 */
public class Bucket
    extends Entity
{
  /**
   * An identifying name for disaster recovery purposes (which isn't required to be strictly unique)
   */
  public static final String REPO_NAME_HEADER = "Bucket.repo-name";

  private String repositoryName;

  /**
   * Gets the repository name.
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /**
   * Sets the repository name.
   */
  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }
}
