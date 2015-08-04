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
package org.sonatype.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.sisu.goodies.common.ComponentSupport;

public abstract class AbstractApplicationStatusSource
    extends ComponentSupport
    implements ApplicationStatusSource
{
  /**
   * System status.
   */
  private final SystemStatus systemStatus;

  /**
   * Read/Write lock guarding systemStatus updates.
   */
  private final ReadWriteLock lock;

  /**
   * Timestamp of last update.
   */
  private long lastUpdate = -1;

  /**
   * Public constructor.
   */
  public AbstractApplicationStatusSource() {
    this.systemStatus = new SystemStatus();

    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Internal method for getting SystemStatus. Does not perform any locking.
   */
  protected SystemStatus getSystemStatusInternal() {
    return systemStatus;
  }

  /**
   * Returns the RW lock.
   */
  protected ReadWriteLock getLock() {
    return lock;
  }

  // ==

  /**
   * Returns the SystemStatus, guaranteeing its consistent state.
   */
  public SystemStatus getSystemStatus() {
    updateSystemStatusIfNeeded(false);

    Lock lock = getLock().readLock();

    lock.lock();

    try {
      return getSystemStatusInternal();
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Force an update of SystemStatus.
   */
  public void updateSystemStatus() {
    updateSystemStatusIfNeeded(true);
  }

  public boolean setState(SystemState state) {
    Lock lock = getLock().writeLock();

    lock.lock();

    try {
      getSystemStatusInternal().setState(state);

      return true;
    }
    finally {
      lock.unlock();
    }
  }

  // ==

  /**
   * Reads the version from a properties file (the one embedded by Maven into Jar).
   */
  protected String readVersion(String path) {
    String version = "Unknown";

    try {
      Properties props = new Properties();

      InputStream is = getClass().getResourceAsStream(path);

      if (is != null) {
        props.load(is);

        version = props.getProperty("version");
      }

    }
    catch (IOException e) {
      log.error("Could not load/read version from: {}", path, e);
    }

    return version;
  }

  /**
   * Will check is needed a SystemStatus update (using retain time) and will perform it.
   *
   * @param forced if update is forced (performs update forcefully)
   */
  protected void updateSystemStatusIfNeeded(boolean forced) {
    long currentTime = System.currentTimeMillis();

    if (forced || (currentTime - lastUpdate > 30000)) {
      Lock lock = getLock().writeLock();

      lock.lock();

      try {
        // maybe someone did the job, while we were blocked
        if (forced || (currentTime - lastUpdate > 30000)) {
          renewSystemStatus(getSystemStatusInternal());

          lastUpdate = currentTime;
        }
      }
      finally {
        lock.unlock();
      }
    }
  }

  /**
   * Discovers (probably in "edition specific" way) the version of the application.
   */
  protected abstract String discoverApplicationVersion();

  /**
   * Implement here any updates to SystemStatus needed. No need to bother with locking, it happens in the caller of
   * this method. The method body contains exclusive lock to SystemStatus.
   */
  protected abstract void renewSystemStatus(SystemStatus systemStatus);
}
