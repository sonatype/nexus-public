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
package org.sonatype.nexus.proxy.wastebasket;

import java.util.Collection;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.SilentWalker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerProcessor;

public class WastebasketWalker
    extends AbstractWalkerProcessor
    implements WalkerProcessor, SilentWalker
{

  private long age;

  public WastebasketWalker(long age) {
    this.age = age;
  }

  @Override
  public void processItem(WalkerContext ctx, StorageItem item) {
    long now = System.currentTimeMillis();
    long limitDate = now - age;

    if (item instanceof StorageFileItem && //
        (age == DefaultWastebasket.ALL || item.getModified() < limitDate)) {
      try {
        ctx.getRepository().getLocalStorage().shredItem(ctx.getRepository(), item.getResourceStoreRequest());
      }
      catch (ItemNotFoundException e) {
        // silent
      }
      catch (UnsupportedStorageOperationException e) {
        // silent?
      }
      catch (LocalStorageException e) {
        // silent?
      }
    }
  }

  @Override
  public void onCollectionExit(WalkerContext ctx, StorageCollectionItem item)
      throws Exception
  {
    if (ctx.getResourceStoreRequest().getRequestPath().equals(item.getPath())) {
      // NEXUS-4642 do not delete the trash
      return;
    }

    try {
      // item is now gone, let's check if this is empty and if so delete it as well
      Collection<StorageItem> items = item.list();
      if (items.isEmpty()) {
        ctx.getRepository().getLocalStorage().shredItem(ctx.getRepository(), item.getResourceStoreRequest());
      }
    }
    catch (final ItemNotFoundException ignore) {
      // someone else removed the item in the mean time. yet we anyhow wanted to remove it so... nevermind
    }
  }
}
