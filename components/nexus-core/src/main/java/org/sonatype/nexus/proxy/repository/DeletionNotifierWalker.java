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

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDeleteItem;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Preconditions;

/**
 * WalkerProcessor performing a "notification" (by firing corresponding events) of items about to be deleted as part of
 * a delete operation invoked against collection item. It handles all item types except collections.
 *
 * @author cstamas
 */
public class DeletionNotifierWalker
    extends AbstractWalkerProcessor
{
  private final EventBus eventBus;

  private final ResourceStoreRequest request;

  public DeletionNotifierWalker(final EventBus eventBus,
                                final ResourceStoreRequest request)
  {
    this.eventBus = Preconditions.checkNotNull(eventBus);
    this.request = Preconditions.checkNotNull(request);
  }

  @Override
  public final void processItem(final WalkerContext context, final StorageItem item)
      throws Exception
  {
    if (!(item instanceof StorageCollectionItem)) {
      // cstamas: this should be not needed, as Walker should handle this!
      item.getItemContext().setParentContext(request.getRequestContext());

      // just fire it, and someone will eventually catch it
      eventBus.post(new RepositoryItemEventDeleteItem(context.getRepository(), item));
    }
  }
}
