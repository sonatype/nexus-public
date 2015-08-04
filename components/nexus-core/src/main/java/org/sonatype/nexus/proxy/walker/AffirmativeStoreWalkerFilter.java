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
 * The affirmative implementation of StoraWalkerFilter that is used when no filter is supplied. It will process all
 * items and will dive into all collections.
 *
 * @author cstamas
 */
public class AffirmativeStoreWalkerFilter
    implements WalkerFilter
{
  public boolean shouldProcess(WalkerContext ctx, StorageItem item) {
    return true;
  }

  public boolean shouldProcessRecursively(WalkerContext ctx, StorageCollectionItem coll) {
    return true;
  }
}
