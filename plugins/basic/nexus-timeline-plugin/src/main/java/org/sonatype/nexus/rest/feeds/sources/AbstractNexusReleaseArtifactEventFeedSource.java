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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.Repository;

public abstract class AbstractNexusReleaseArtifactEventFeedSource
    extends AbstractNexusItemEventFeedSource
{
  private SyndEntryBuilder<NexusArtifactEvent> entryBuilder;

  @Inject
  public void setEntryBuilder(final @Named("artifact") SyndEntryBuilder<NexusArtifactEvent> entryBuilder) {
    this.entryBuilder = entryBuilder;
  }

  @Override
  public SyndEntryBuilder<NexusArtifactEvent> getSyndEntryBuilder(NexusArtifactEvent event) {
    return entryBuilder;
  }

  protected Set<String> getRepoIdsFromParams(Map<String, String> params) {
    Set<String> result = new HashSet<String>();

    Collection<Repository> repos = getRepositoryRegistry().getRepositories();

    for (Repository repo : repos) {
      // huh? release as policy exists for MavenRepository only?
      if (repo.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
        if (RepositoryPolicy.RELEASE.equals(repo.adaptToFacet(MavenRepository.class).getRepositoryPolicy())) {
          result.add(repo.getId());
        }
      }
    }

    return result;
  }
}
