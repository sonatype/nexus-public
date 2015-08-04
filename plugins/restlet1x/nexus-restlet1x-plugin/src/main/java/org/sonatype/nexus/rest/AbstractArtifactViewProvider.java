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
package org.sonatype.nexus.rest;

import java.io.IOException;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.nexus.proxy.router.RequestRoute;
import org.sonatype.sisu.goodies.common.Loggers;

import org.restlet.data.Request;
import org.slf4j.Logger;

public abstract class AbstractArtifactViewProvider
    implements ArtifactViewProvider
{
  private final Logger logger = Loggers.getLogger(getClass());

  private RepositoryRouter repositoryRouter;

  @Inject
  public void setRepositoryRouter(final RepositoryRouter repositoryRouter) {
    this.repositoryRouter = repositoryRouter;
  }

  protected Logger getLogger() {
    return logger;
  }

  // ==

  public Object retrieveView(ResourceStore store, ResourceStoreRequest request, StorageItem item, Request req)
      throws IOException
  {
    RepositoryItemUid itemUid = null;

    if (item == null) {
      if (store instanceof RepositoryRouter) {
        RepositoryRouter repositoryRouter = (RepositoryRouter) store;
        // item is either not present or is not here yet (remote index)
        // the we can "simulate" what route would be used to get it, and just get info from the route
        RequestRoute route;

        try {
          route = repositoryRouter.getRequestRouteForRequest(request);
        }
        catch (ItemNotFoundException e) {
          // this is thrown while getting routes for any path "outside" of legal ones is given
          // like /content/foo/bar, since 2nd pathelem may be "repositories", "groups", "shadows", etc
          // (depends on
          // type of registered reposes)
          return null;
        }

        // request would be processed by targeted repository
        Repository itemRepository = route.getTargetedRepository();

        // create an UID against that repository
        itemUid = itemRepository.createUid(route.getRepositoryPath());
      }
      else if (store instanceof Repository) {
        itemUid = ((Repository) store).createUid(request.getRequestPath());
      }
      else {
        // this is highly unbelievable, unless Core gets extended by 3rd party
        return null;
      }
    }
    else {
      itemUid = item.getRepositoryItemUid();

      if ((item instanceof StorageLinkItem) && dereferenceLinks()) {
        // TODO: we may have "deeper" links too! Implement this properly!
        try {
          item =
              repositoryRouter.dereferenceLink((StorageLinkItem) item, request.isRequestLocalOnly(),
                  request.isRequestRemoteOnly());
        }
        catch (Exception e) {
          getLogger().warn("Failed to dereference the storagelink " + item.getRepositoryItemUid(), e);

          // leave item unchanged
        }
      }
    }

    // so, we ended with:
    // itemUid is always populated, hence we have Repository and repository path
    // so, item may be null or non-null, if non-null, it may be link

    // check for item not found finally. Those may be allowed in proxy repositories only.
    if (item == null && !processNotFoundItems(itemUid.getRepository())) {
      // return not-applicable. This is not a proxy repository, and the item is not found. Since it is not
      // proxy repo, it will be never cached from remote too, simply, it is not here.
      return null;
    }

    return retrieveView(request, itemUid, item, req);
  }

  // ==

  /**
   * Defines a "default": we do not want to dereference links. For example, when asking for a view over M1 shadow
   * repository, we do want to "describe" the M1 artifact, even if it is actually a link to M2 artifact. We still
   * need
   * M1 GAV for it.
   */
  protected boolean dereferenceLinks() {
    return false;
  }

  /**
   * Checks for a "default" behavior for not-found items. For those, we expect that repository is proxy repository,
   * and we allow further processing. For all other non-proxy repositories, we just return "not available", since
   * they
   * have no remote peer to download from, hence, since it is not found, it will be never there. Override if needed.
   */
  protected boolean processNotFoundItems(Repository repo) {
    if (repo.getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
      return false;
    }
    return true;
  }

  /**
   * Do actual work.
   */
  protected abstract Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item,
                                         Request req)
      throws IOException;
}
