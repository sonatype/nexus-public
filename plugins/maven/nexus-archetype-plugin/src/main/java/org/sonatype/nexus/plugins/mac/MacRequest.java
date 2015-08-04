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
package org.sonatype.nexus.plugins.mac;

import org.apache.maven.index.ArtifactInfoFilter;

/**
 * A request carrying needed information to construct response Archetype Catalog properly.
 *
 * @author cstamas
 */
public class MacRequest
{
  private final String repositoryId;

  private final String repositoryUrl;

  private final ArtifactInfoFilter artifactInfoFilter;

  public MacRequest(String repositoryId) {
    this(repositoryId, null, null);
  }

  public MacRequest(final String repositoryId, final String repositoryUrl,
                    final ArtifactInfoFilter artifactInfoFilter)
  {
    this.repositoryId = repositoryId;
    this.repositoryUrl = repositoryUrl;
    this.artifactInfoFilter = artifactInfoFilter;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public ArtifactInfoFilter getArtifactInfoFilter() {
    return artifactInfoFilter;
  }
}
