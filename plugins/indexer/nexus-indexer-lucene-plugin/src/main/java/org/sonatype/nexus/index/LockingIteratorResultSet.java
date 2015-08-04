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
package org.sonatype.nexus.index;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorResultSet;

/**
 * IteratorResultSet wrapper that unlocks provided lock when closed.
 */
class LockingIteratorResultSet
    implements IteratorResultSet
{
  private final IteratorResultSet result;

  private final Lock lock;

  private boolean closed;

  public LockingIteratorResultSet(IteratorResultSet result, Lock lock) {
    this.result = result;
    this.lock = lock;
  }

  @Override
  public boolean hasNext() {
    return result.hasNext();
  }

  @Override
  public ArtifactInfo next() {
    return result.next();
  }

  @Override
  public void remove() {
    result.remove();
  }

  @Override
  public Iterator<ArtifactInfo> iterator() {
    return result.iterator();
  }

  @Override
  public void close()
      throws IOException
  {
    if (!closed) {
      try {
        result.close();
      }
      finally {
        lock.unlock();
        closed = true;
      }
    }
  }

  @Override
  public int getTotalProcessedArtifactInfoCount() {
    return result.getTotalProcessedArtifactInfoCount();
  }

}
