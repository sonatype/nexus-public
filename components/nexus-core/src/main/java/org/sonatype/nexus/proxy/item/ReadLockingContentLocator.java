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

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.util.WrappingInputStream;

import org.slf4j.LoggerFactory;

/**
 * This is a simple wrapper implementation of ContentLocator, that wraps any other ContentLocator, while doing proper
 * {@link Action} read locking against the UID the locator points to.
 * 
 * @author cstamas
 */
public class ReadLockingContentLocator
    extends AbstractWrappingContentLocator
{
  private final RepositoryItemUid wrappedUid;

  public ReadLockingContentLocator(final RepositoryItemUid wrappedUid, final ContentLocator wrappedLocator) {
    super(wrappedLocator);
    this.wrappedUid = wrappedUid;
  }

  @Override
  public InputStream getContent() throws IOException {
    final RepositoryItemUidLock lock = wrappedUid.getLock();

    lock.lock(Action.read);

    try {
      return new ReadLockingInputStream(wrappedUid, lock, getTarget().getContent());
    }
    catch (IOException e) {
      lock.unlock();

      throw e;
    }
    catch (Exception e) {
      lock.unlock();

      // wrap it
      IOException w = new IOException(e.getMessage());
      w.initCause(e);
      throw w;
    }
  }

  // ==

  private static class ReadLockingInputStream
      extends WrappingInputStream
  {
    private final RepositoryItemUid uid;

    private volatile RepositoryItemUidLock lock;

    public ReadLockingInputStream(final RepositoryItemUid uid, final RepositoryItemUidLock lock,
        final InputStream wrappedStream)
    {
      super(wrappedStream);
      this.uid = uid;
      this.lock = lock;
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
      }
      finally {
        if (lock != null) {
          lock.unlock();
          lock = null;
        }
      }
    }

    @Override
    public void finalize() throws Throwable {
      try {
        if (lock != null) {
          lock.unlock();
          lock = null;
          LoggerFactory.getLogger(ReadLockingContentLocator.class).warn("UID lock leak detected for UID {}", uid);
        }
      }
      finally {
        super.finalize();
      }
    }
  }
}
