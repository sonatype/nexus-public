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
package org.sonatype.nexus.obr.util;

import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

/**
 * {@link WalkerFilter} that only accepts files that are potential OBR bundle resource candidates.
 */
public class ObrWalkerFilter
    implements WalkerFilter
{
  public boolean shouldProcess(final WalkerContext context, final StorageItem item) {
    return ObrUtils.acceptItem(item);
  }

  public boolean shouldProcessRecursively(final WalkerContext context, final StorageCollectionItem coll) {
    return !coll.getName().startsWith(".");
  }
}
