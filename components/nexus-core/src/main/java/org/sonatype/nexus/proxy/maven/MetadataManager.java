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
package org.sonatype.nexus.proxy.maven;

import java.io.IOException;

import org.sonatype.nexus.proxy.maven.gav.Gav;

public interface MetadataManager
{
  /**
   * Calling this method updates the GAV, GA and G metadatas accordingly. It senses whether it is a snapshot or not.
   */
  void deployArtifact(ArtifactStoreRequest request)
      throws IOException;

  /**
   * Resolves the artifact, honoring LATEST and RELEASE as version. In case of snapshots, it will try to resolve the
   * timestamped version too, if needed.
   */
  Gav resolveArtifact(ArtifactStoreRequest gavRequest)
      throws IOException;

  /**
   * Resolves the snapshot base version to a timestamped version if possible. Only when a repo is snapshot.
   */
  Gav resolveSnapshot(ArtifactStoreRequest gavRequest, Gav gav)
      throws IOException;
}
