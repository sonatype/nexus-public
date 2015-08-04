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

public abstract class AbstractWalkerProcessor
    implements WalkerProcessor
{
  private boolean active = true;

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void beforeWalk(WalkerContext context)
      throws Exception
  {
  }

  public void onCollectionEnter(WalkerContext context, StorageCollectionItem coll)
      throws Exception
  {
  }

  public abstract void processItem(WalkerContext context, StorageItem item)
      throws Exception;

  public void onCollectionExit(WalkerContext context, StorageCollectionItem coll)
      throws Exception
  {
  }

  public void afterWalk(WalkerContext context)
      throws Exception
  {
  }
}
