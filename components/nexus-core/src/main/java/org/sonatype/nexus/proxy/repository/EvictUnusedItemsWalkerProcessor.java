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
package org.sonatype.nexus.proxy.repository;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerFilter;
import org.sonatype.nexus.proxy.wastebasket.DeleteOperation;

public class EvictUnusedItemsWalkerProcessor
    extends AbstractFileWalkerProcessor
{
  public static final String REQUIRED_FACET_KEY = "repository.facet";

  public static final Class<? extends Repository> DEFAULT_REQUIRED_FACET = ProxyRepository.class;

  private final long timestamp;

  private final ArrayList<String> files;

  public EvictUnusedItemsWalkerProcessor(long timestamp) {
    this.timestamp = timestamp;

    this.files = new ArrayList<String>();
  }

  protected Class<? extends Repository> getRequiredFacet(WalkerContext context) {
    if (context.getContext().containsKey(REQUIRED_FACET_KEY)) {
      return (Class<? extends Repository>) context.getContext().get(REQUIRED_FACET_KEY);
    }
    else {
      return DEFAULT_REQUIRED_FACET;
    }
  }

  protected Repository getRepository(WalkerContext ctx) {
    return ctx.getRepository();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public List<String> getFiles() {
    return files;
  }

  @Override
  public void beforeWalk(WalkerContext context)
      throws Exception
  {
    Class<? extends Repository> requiredFacet = getRequiredFacet(context);

    if (!getRepository(context).getRepositoryKind().isFacetAvailable(requiredFacet)) {
      context.stop(null);
    }
  }

  @Override
  public void processFileItem(WalkerContext ctx, StorageFileItem item)
      throws StorageException
  {
    // expiring found files
    try {
      if (item.getLastRequested() < getTimestamp()) {
        doDelete(ctx, item);

        getFiles().add(item.getPath());
      }
    }
    catch (IllegalOperationException e) {
      // simply stop if set during processing
      ctx.stop(e);
    }
    catch (UnsupportedStorageOperationException e) {
      // if op not supported (R/O repo?)
      ctx.stop(e);
    }
    catch (ItemNotFoundException e) {
      // will not happen
    }
  }

  protected void doDelete(WalkerContext ctx, StorageFileItem item)
      throws StorageException, UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException
  {
    final ResourceStoreRequest rsr = new ResourceStoreRequest(item);
    rsr.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY,DeleteOperation.DELETE_PERMANENTLY);
    getRepository(ctx).deleteItem(false, rsr);
  }

  @Override
  public void onCollectionExit(WalkerContext ctx, StorageCollectionItem coll)
      throws Exception
  {
    // expiring now empty directories
    try {
      if (getRepository(ctx).list(false, coll).size() == 0) {
        final ResourceStoreRequest rsr = new ResourceStoreRequest(coll);
        rsr.getRequestContext().put(DeleteOperation.DELETE_OPERATION_CTX_KEY,DeleteOperation.DELETE_PERMANENTLY);
        getRepository(ctx).deleteItem(false, rsr);
      }
    }
    catch (RepositoryNotAvailableException e) {
      // simply stop if set during processing
      ctx.stop(e);
    }
    catch (UnsupportedStorageOperationException e) {
      // if op not supported (R/O repo?)
      ctx.stop(e);
    }
    catch (ItemNotFoundException e) {
      // will not happen
    }
    catch (StorageException e) {
      ctx.stop(e);
    }
  }

  // ==

  public static class EvictUnusedItemsWalkerFilter
      implements WalkerFilter
  {
    public boolean shouldProcess(WalkerContext context, StorageItem item) {
      // skip "hidden" files
      return !item.getPath().startsWith("/.") && !item.getPath().startsWith(".");
    }

    public boolean shouldProcessRecursively(WalkerContext context, StorageCollectionItem coll) {
      // we are "cutting" the .index dir from processing
      return shouldProcess(context, coll);
    }
  }

}
