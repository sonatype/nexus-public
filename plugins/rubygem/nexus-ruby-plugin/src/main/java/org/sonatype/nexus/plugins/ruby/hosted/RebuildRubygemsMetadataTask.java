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
package org.sonatype.nexus.plugins.ruby.hosted;

import java.util.List;

import javax.inject.Named;

import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

/**
 * Rubygems hosted repository rebuild metadata task.
 *
 * @since 2.11
 */
@Named(RebuildRubygemsMetadataTaskDescriptor.ID)
public class RebuildRubygemsMetadataTask
    extends AbstractNexusRepositoriesTask<Object>
{
  public static final String ACTION = "REBUILDRUBYGEMSMETADATA";

  @Override
  protected String getRepositoryFieldId() {
    return RebuildRubygemsMetadataTaskDescriptor.REPO_FIELD_ID;
  }

  @Override
  public Object doRun() throws Exception {
    if (getRepositoryId() != null) {
      Repository repository = getRepositoryRegistry().getRepository(getRepositoryId());

      // is this a hosted rubygems repository at all?
      if (repository.getRepositoryKind().isFacetAvailable(HostedRubyRepository.class)) {
        HostedRubyRepository rubyRepository = repository.adaptToFacet(HostedRubyRepository.class);

        rubyRepository.recreateMetadata();
      }
      else {
        getLogger().info(
                "Repository {} is not a hosted Rubygems repository. Will not rebuild metadata, but the task seems wrongly configured!",
                repository);
      }
    }
    else {
      List<HostedRubyRepository> reposes = getRepositoryRegistry().getRepositoriesWithFacet(HostedRubyRepository.class);

      for (HostedRubyRepository repo : reposes) {
        repo.recreateMetadata();
      }
    }

    return null;
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() != null) {
      return "Rebuilding gemspecs and specs-index of repository " + getRepositoryName();
    }
    else {
      return "Rebuilding gemspecs and specs-index of all registered repositories";
    }
  }
}