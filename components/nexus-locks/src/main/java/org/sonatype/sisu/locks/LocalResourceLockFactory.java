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
package org.sonatype.sisu.locks;

import java.util.concurrent.Semaphore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Local semaphore-based {@link ResourceLockFactory} implementation.
 */
@Named("local")
@Singleton
public final class LocalResourceLockFactory
    extends AbstractResourceLockFactory
{
  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  @Inject
  public LocalResourceLockFactory() {
    this(true);
  }

  public LocalResourceLockFactory(final boolean jmxEnabled) {
    super(jmxEnabled);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  @Override
  protected String category() {
    return LocalResourceLock.class.getSimpleName() + 's';
  }

  @Override
  protected ResourceLock createResourceLock(final String name) {
    return new LocalResourceLock();
  }
}

/**
 * {@link ResourceLock} implemented on top of a JDK {@link Semaphore}.
 */
final class LocalResourceLock
    extends AbstractSemaphoreResourceLock
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final Semaphore sem;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  LocalResourceLock() {
    sem = new Semaphore(Integer.MAX_VALUE, true);
  }

  // ----------------------------------------------------------------------
  // Semaphore methods
  // ----------------------------------------------------------------------

  @Override
  protected void acquire(final int permits) {
    sem.acquireUninterruptibly(permits);
  }

  @Override
  protected void release(final int permits) {
    sem.release(permits);
  }

  @Override
  protected int availablePermits() {
    return sem.availablePermits();
  }
}
