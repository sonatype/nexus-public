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
package org.sonatype.nexus.feeds;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.access.NexusItemAuthorizer;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class DefaultFeedArtifactEventFilter
    extends ComponentSupport
    implements FeedArtifactEventFilter
{
  private final NexusItemAuthorizer nexusItemAuthorizer;

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public DefaultFeedArtifactEventFilter(final NexusItemAuthorizer nexusItemAuthorizer,
                                        final RepositoryRegistry repositoryRegistry)
  {
    this.nexusItemAuthorizer = checkNotNull(nexusItemAuthorizer);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  public List<NexusArtifactEvent> filterArtifactEventList(List<NexusArtifactEvent> artifactEvents) {
    // make sure we have something to filter
    if (artifactEvents == null) {
      return null;
    }

    List<NexusArtifactEvent> filteredList = new ArrayList<NexusArtifactEvent>();

    for (NexusArtifactEvent nexusArtifactEvent : artifactEvents) {
      if (this.filterEvent(nexusArtifactEvent)) {
        filteredList.add(nexusArtifactEvent);
      }
    }

    return filteredList;
  }

  private boolean filterEvent(NexusArtifactEvent event) {
    try {
      Repository repo = this.repositoryRegistry.getRepository(event.getNexusItemInfo().getRepositoryId());

      ResourceStoreRequest req = new ResourceStoreRequest(event.getNexusItemInfo().getPath());

      if (!this.nexusItemAuthorizer.authorizePath(repo, req, Action.read)) {
        return false;
      }
    }
    catch (NoSuchRepositoryException e) {
      // Can't get repository for artifact, therefore we can't authorize access, therefore you don't see it
      log.debug(
          "Feed entry contained invalid repository id " + event.getNexusItemInfo().getRepositoryId(),
          e);

      return false;
    }

    return true;
  }

}
