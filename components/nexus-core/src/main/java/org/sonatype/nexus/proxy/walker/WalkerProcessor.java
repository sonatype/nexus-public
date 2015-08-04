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
package org.sonatype.nexus.proxy.walker;

import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.WalkerContext.TraversalType;

/**
 * A walker processors are units that are "attachable" to a single storage walk, hence the result will be combined but
 * having only one walk. If in any method and exception is thrown, the walker will stop.
 *
 * @author cstamas
 */
public interface WalkerProcessor
{
  /**
   * No calls will be made to the processor if this method returns {@code false}. In case of "sandwiching" multiple
   * processors, one might declare itself "inactive" if some specific goal was achieven, and it will not use
   * any CPU cycles from that point on. Warning: once processor declares itself "inactive", walker will not
   * invoke any method on this processor, so there is no way to "activate" itself using processor methods.
   */
  boolean isActive();

  /**
   * Invoked before walk begins.
   */
  void beforeWalk(WalkerContext context)
      throws Exception;

  /**
   * Invoked when entering a {@link StorageCollectionItem}. Based on {@link TraversalType} value, the "boxing"
   * of this method's call and {@link #onCollectionExit(WalkerContext, StorageCollectionItem)} call may be
   * nested ({@link TraversalType#DEPTH_FIRST}) or sequential ({@link TraversalType#BREADTH_FIRST}).
   */
  void onCollectionEnter(WalkerContext context, StorageCollectionItem coll)
      throws Exception;

  /**
   * Invoked for each visited {@link StorageItem}. Depending on the {@link WalkerContext#isProcessCollections()} value,
   * this method will be invoked with any item type except collections ({@code false}), or collections will be
   * passed on this method too ({@code true}).
   */
  void processItem(WalkerContext context, StorageItem item)
      throws Exception;

  /**
   * Invoked when exiting a {@link StorageCollectionItem}, if walk is not being stopped. See {@link
   * #onCollectionEnter(WalkerContext, StorageCollectionItem)} for more details about boxing of these calls.
   */
  void onCollectionExit(WalkerContext context, StorageCollectionItem coll)
      throws Exception;

  /**
   * Invoked after walk finishes if it ended without being stopped.
   */
  void afterWalk(WalkerContext context)
      throws Exception;
}
