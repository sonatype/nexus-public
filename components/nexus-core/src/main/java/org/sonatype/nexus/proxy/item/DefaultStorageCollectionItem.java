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
package org.sonatype.nexus.proxy.item;

import java.util.Collection;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * Default implementation of {@link StorageCollectionItem}.
 */
public class DefaultStorageCollectionItem
    extends AbstractStorageItem
    implements StorageCollectionItem
{

  public DefaultStorageCollectionItem(Repository repository, ResourceStoreRequest request, boolean canRead,
      boolean canWrite)
  {
    super(repository, request, canRead, canWrite);
  }

  public DefaultStorageCollectionItem(RepositoryRouter router, ResourceStoreRequest request, boolean canRead,
      boolean canWrite)
  {
    super(router, request, canRead, canWrite);
  }

  @Override
  public Collection<StorageItem> list() throws AccessDeniedException, NoSuchResourceStoreException,
      IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (isVirtual()) {
      return getStore().list(getResourceStoreRequest());
    }
    else {
      Repository repo = getRepositoryItemUid().getRepository();
      Collection<StorageItem> result = repo.list(false, this);
      correctPaths(result);
      return result;
    }
  }

  // ==

  /**
   * This method "normalizes" the paths back to the "level" from where the original item was requested.
   */
  protected void correctPaths(Collection<StorageItem> list) {
    for (StorageItem item : list) {
      if (getPath().endsWith(RepositoryItemUid.PATH_SEPARATOR)) {
        ((AbstractStorageItem) item).setPath(getPath() + item.getName());
      }
      else {
        ((AbstractStorageItem) item).setPath(getPath() + RepositoryItemUid.PATH_SEPARATOR + item.getName());
      }
    }
  }

  // --

  @Override
  public String toString() {
    return String.format("%s (coll)", super.toString());
  }
}
