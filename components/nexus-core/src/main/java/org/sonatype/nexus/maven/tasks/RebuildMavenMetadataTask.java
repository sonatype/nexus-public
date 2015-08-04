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

import java.util.List;

import javax.inject.Named;

import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesPathAwareTask;
import org.sonatype.scheduling.TaskUtil;

import org.codehaus.plexus.util.StringUtils;

/**
 * Rebuild Maven metadata task.
 */
@Named(RebuildMavenMetadataTaskDescriptor.ID)
public class RebuildMavenMetadataTask
    extends AbstractNexusRepositoriesPathAwareTask<Object>
{

  public static final String REBUILD_MAVEN_METADATA_ACTION = "REBUILD_MAVEN_METADATA";

  @Override
  protected String getRepositoryFieldId() {
    return RebuildMavenMetadataTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected String getRepositoryPathFieldId() {
    return RebuildMavenMetadataTaskDescriptor.RESOURCE_STORE_PATH_FIELD_ID;
  }

  @Override
  public Object doRun()
      throws Exception
  {
    ResourceStoreRequest req = new ResourceStoreRequest(getResourceStorePath());

    // no repo id, then do all repos
    if (StringUtils.isEmpty(getRepositoryId())) {
      List<MavenRepository> reposes = getRepositoryRegistry().getRepositoriesWithFacet(MavenRepository.class);

      TaskUtil.getCurrentProgressListener().beginTask("Recreating Maven Metadata on all Maven repositories",
          reposes.size());

      for (MavenRepository repo : reposes) {
        TaskUtil.getCurrentProgressListener().working(
            RepositoryStringUtils.getFormattedMessage("Recreating Maven Metadata on %s", repo), 1);
        repo.recreateMavenMetadata(req);
      }

      TaskUtil.getCurrentProgressListener().endTask("Done");
    }
    else {
      Repository repository = getRepositoryRegistry().getRepository(getRepositoryId());

      // is this a Maven repository at all?
      if (repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        MavenRepository mavenRepository = repository.adaptToFacet(MavenRepository.class);

        TaskUtil.getCurrentProgressListener().beginTask(
            RepositoryStringUtils.getFormattedMessage("Recreating Maven Metadata on %s", mavenRepository));

        mavenRepository.recreateMavenMetadata(req);

        TaskUtil.getCurrentProgressListener().endTask("Done");
      }
      else {
        getLogger().info(
            RepositoryStringUtils.getFormattedMessage(
                "Repository %s is not a Maven repository. Will not rebuild maven metadata, but the task seems wrongly configured!",
                repository));
      }
    }

    return null;
  }

  @Override
  protected String getAction() {
    return REBUILD_MAVEN_METADATA_ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Rebuilding maven metadata of repository " + getRepositoryName() + " from path "
          + getResourceStorePath() + " and below.";
    }
    else {
      return "Rebuilding maven metadata of all registered repositories";
    }
  }
}
