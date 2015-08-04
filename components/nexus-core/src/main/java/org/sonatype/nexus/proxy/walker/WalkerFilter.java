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

/**
 * A simple filter a la FileFilter, for filtering which items should be processed. Note: the StoreWalker does RECURSIVE
 * processing, hence unlike FileFilter, if the current collection is filtered out, but the filter says diveIn, it will
 * dive in deeper.
 *
 * @author cstamas
 */
public interface WalkerFilter
{
  /**
   * This method is called for every item implementation. If returns fals, it will not initate any call against to
   * for
   * processing (ie. collEnter, processItem, callExit). But the recursive processing of this item (in case it is
   * Collection) is not affected by this method!
   */
  boolean shouldProcess(WalkerContext context, StorageItem item);

  /**
   * In case of Collections, StoreWalker will ask should it process those recursively. This is a place to "cut" the
   * tree walking if needed.
   */
  boolean shouldProcessRecursively(WalkerContext context, StorageCollectionItem coll);
}
