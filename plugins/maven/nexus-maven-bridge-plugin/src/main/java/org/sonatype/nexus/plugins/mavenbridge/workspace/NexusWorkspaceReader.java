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
package org.sonatype.nexus.plugins.mavenbridge.workspace;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.WorkspaceReader;
import org.sonatype.aether.repository.WorkspaceRepository;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;

import org.apache.commons.lang.StringUtils;

public class NexusWorkspaceReader
    implements WorkspaceReader
{
  private final NexusWorkspace nexusWorkspace;

  private final WorkspaceRepository workspaceRepository;

  public NexusWorkspaceReader(NexusWorkspace nexusWorkspace) {
    this.nexusWorkspace = nexusWorkspace;

    this.workspaceRepository = new WorkspaceRepository("nexus", nexusWorkspace.getId());
  }

  public WorkspaceRepository getRepository() {
    return workspaceRepository;
  }

  /**
   * This method will in case of released artifact request just locate it, and return if found. In case of snapshot
   * repository, if it needs resolving, will resolve it 1st and than locate it. It will obey to the session (global
   * update policy, that correspondos to Maven CLI "-U" option.
   */
  public File findArtifact(Artifact artifact) {
    Gav gav = toGav(artifact);

    ArtifactStoreRequest gavRequest;

    for (MavenRepository mavenRepository : nexusWorkspace.getRepositories()) {
      gavRequest = new ArtifactStoreRequest(mavenRepository, gav, false, false);

      try {
        StorageFileItem artifactFile = mavenRepository.getArtifactStoreHelper().retrieveArtifact(gavRequest);

        // this will work with local FS storage only, since Aether wants java.io.File
        if (artifactFile.getRepositoryItemUid().getRepository()
            .getLocalStorage() instanceof DefaultFSLocalRepositoryStorage) {
          DefaultFSLocalRepositoryStorage ls =
              (DefaultFSLocalRepositoryStorage) artifactFile.getRepositoryItemUid().getRepository().getLocalStorage();

          return ls.getFileFromBase(artifactFile.getRepositoryItemUid().getRepository(), gavRequest);
        }
      }
      catch (Exception e) {
        // Something wrong happen for this repository, let's process the next one
      }
    }

    return null;
  }

  private Gav toGav(Artifact artifact) {
    // fix for bug in M2GavCalculator
    final String classifier = StringUtils.isEmpty(artifact.getClassifier()) ? null : artifact.getClassifier();

    final Gav gav =
        new Gav(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), classifier,
            artifact.getExtension(), null, null, null, false, null, false, null);

    return gav;
  }

  /**
   * Basically, this method will read the GA metadata, and return the "known versions".
   */
  public List<String> findVersions(Artifact artifact) {
    Gav gav = toGav(artifact);

    if (gav.isSnapshot()) {
      ArtifactStoreRequest gavRequest;

      for (MavenRepository mavenRepository : nexusWorkspace.getRepositories()) {
        gavRequest = new ArtifactStoreRequest(mavenRepository, gav, false, false);

        try {
          Gav snapshot = mavenRepository.getMetadataManager().resolveSnapshot(gavRequest, gav);
          return Collections.singletonList(snapshot.getVersion());
        }
        catch (Exception e) {
          // try next repo
          continue;
        }
      }
    }

    return Collections.emptyList();
  }

}
