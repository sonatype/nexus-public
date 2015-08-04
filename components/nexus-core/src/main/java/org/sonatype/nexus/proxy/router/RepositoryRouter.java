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
package org.sonatype.nexus.proxy.router;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Repository Router interface. This router offers a simple API to request items from Proximity. It calculates and
 * knows which repositories are registered within Proximity.
 *
 * @author cstamas
 * @see Repository
 * @see RepositoryRegistry
 */
public interface RepositoryRouter
    extends ResourceStore
{
  boolean isFollowLinks();

  void setFollowLinks(boolean follow);

  /**
   * Dereferences the link.
   */
  StorageItem dereferenceLink(StorageLinkItem link)
      throws AccessDeniedException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException;

  /**
   * Dereferences the link.
   */
  StorageItem dereferenceLink(StorageLinkItem link, boolean localOnly, boolean remoteOnly)
      throws AccessDeniedException,
             ItemNotFoundException,
             IllegalOperationException,
             StorageException;

  /**
   * Calculates the RequestRoute for the given request.
   */
  RequestRoute getRequestRouteForRequest(ResourceStoreRequest request)
      throws ItemNotFoundException;

  /**
   * Authorizes a TargetSet against an action. Used by authz filter to check the incoming request, that is obviously
   * addressed to content root.
   */
  boolean authorizePath(ResourceStoreRequest request, Action action);

}
