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

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.EvictUnusedItemsWalkerProcessor;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;

public class EvictUnusedMavenItemsWalkerProcessor
    extends EvictUnusedItemsWalkerProcessor
{
  public EvictUnusedMavenItemsWalkerProcessor(long timestamp) {
    super(timestamp);
  }

  // added filter for maven reposes to exclude .index dirs
  // and all hash files, as they will be removed if main artifact
  // is removed
  public static class EvictUnusedMavenItemsWalkerFilter
      extends EvictUnusedItemsWalkerFilter
  {
    public boolean shouldProcess(WalkerContext context, StorageItem item) {
      return super.shouldProcess(context, item) && !item.getPath().startsWith("/.index")
          && !item.getPath().endsWith(".asc") && !item.getPath().endsWith(".sha1")
          && !item.getPath().endsWith(".md5");
    }
  }

  @Override
  public void doDelete(WalkerContext ctx, StorageFileItem item)
      throws StorageException, UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException
  {
    final MavenRepository repository = (MavenRepository) getRepository(ctx);
    final ResourceStoreRequest rsr = new ResourceStoreRequest(item);
    rsr.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY,DeleteOperation.DELETE_PERMANENTLY);
    repository.deleteItemWithChecksums(false, rsr);
  }

  // on maven repositories, we must use another delete method
  @Override
  public void onCollectionExit(WalkerContext ctx, StorageCollectionItem coll) {
    // expiring now empty directories
    try {
      if (getRepository(ctx).list(false, coll).size() == 0) {
        final ResourceStoreRequest rsr = new ResourceStoreRequest(coll);
        rsr.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY,DeleteOperation.DELETE_PERMANENTLY);
        ((MavenRepository) getRepository(ctx)).deleteItemWithChecksums(false, rsr);
      }
    }
    catch (UnsupportedStorageOperationException e) {
      // if op not supported (R/O repo?)
      ctx.stop(e);
    }
    catch (ItemNotFoundException e) {
      // will not happen
    }
    catch (IllegalOperationException e) {
      // simply stop if set during processing
      ctx.stop(e);
    }
    catch (StorageException e) {
      ctx.stop(e);
    }
  }
}
