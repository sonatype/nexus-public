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
package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.access.Action;

public class DefaultRepositoryItemUidLock
    implements RepositoryItemUidLock
{
  private final String key;

  private final LockResource contentLock;

  protected DefaultRepositoryItemUidLock(final String key, final LockResource contentLock) {
    super();

    this.key = key;

    this.contentLock = contentLock;
  }

  @Override
  public void lock(final Action action) {
    if (action.isReadAction()) {
      contentLock.lockShared();
    }
    else {
      contentLock.lockExclusively();
    }
  }

  @Override
  public void unlock() {
    contentLock.unlock();
  }

  public boolean hasLocksHeld() {
    return contentLock.hasLocksHeld();
  }

  // ==

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultRepositoryItemUidLock other = (DefaultRepositoryItemUidLock) obj;
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    }
    else if (!key.equals(other.key)) {
      return false;
    }
    return true;
  }

  // for Debug/tests vvv

  protected LockResource getContentLock() {
    return contentLock;
  }

  // for Debug/tests ^^^

}
