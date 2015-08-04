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

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;

/**
 * Common utility methods for WalkerProcessor implementations that perform
 * file delete operations as part of their processing.
 *
 * @since 2.5
 */
public abstract class AbstractFileDeletingWalkerProcessor
    extends AbstractWalkerProcessor
{

  /**
   * Inspect the given collection and delete it from the repository if it no longer has any files.
   */
  protected void removeDirectoryIfEmpty(MavenRepository repository, StorageCollectionItem coll)
      throws StorageException, IllegalOperationException, UnsupportedStorageOperationException
  {
    try {
      if (repository.list(false, coll).size() > 0) {
        return;
      }
      // directory is empty, never move to trash
      repository.deleteItem(false, createResourceStoreRequest(coll, DeleteOperation.DELETE_PERMANENTLY));
    }
    catch (ItemNotFoundException e) {
      // silent, this happens if whole GAV is removed and the dir is removed too
    }
  }

  /**
   * Create a request to delete an item.
   */
  protected ResourceStoreRequest createResourceStoreRequest(final StorageItem item, final WalkerContext ctx) {
    ResourceStoreRequest request = new ResourceStoreRequest(item);

    if (ctx.getContext().containsKey(DeleteOperation.DELETE_OPERATION_CTX_KEY)) {
      request.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY,
          ctx.getContext().get(DeleteOperation.DELETE_OPERATION_CTX_KEY));
    }

    return request;
  }

  /**
   * Create a request to delete an item with the specified DeleteOperation.
   */
  protected ResourceStoreRequest createResourceStoreRequest(final StorageCollectionItem item,
                                                            final DeleteOperation operation)
  {
    ResourceStoreRequest request = new ResourceStoreRequest(item);
    request.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY, operation);
    return request;
  }

}
