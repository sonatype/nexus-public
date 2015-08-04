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
package org.sonatype.nexus.logging.internal;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.nexus.logging.LoggingConfigurator;
import org.sonatype.nexus.logging.model.LevelXO;
import org.sonatype.nexus.logging.model.LoggerXO;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link LoggingConfigurator} implementation.
 *
 * @since 2.7
 */
@Named
@Singleton
public class LoggingConfiguratorImpl
    implements LoggingConfigurator
{

  public static final String ROOT = "ROOT";

  private final LogManager logManager;

  private final ReadWriteLock lock;

  @Inject
  public LoggingConfiguratorImpl(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);

    lock = new ReentrantReadWriteLock();
  }

  @Override
  public Collection<LoggerXO> getLoggers() {
    Collection<LoggerXO> loggers = Lists.newArrayList();
    for (Entry<String, LoggerLevel> entry : logManager.getLoggers().entrySet()) {
      loggers.add(new LoggerXO().withName(entry.getKey()).withLevel(LevelXO.valueOf(entry.getValue().name())));
    }
    return loggers;
  }

  @Override
  public LevelXO setLevel(final String name, final LevelXO level) {
    checkNotNull(name, "name");
    checkNotNull(level, "level");

    try {
      lock.writeLock().lock();
      logManager.setLoggerLevel(name, LoggerLevel.valueOf(level.name()));
      return LevelXO.valueOf(logManager.getLoggerEffectiveLevel(name).name());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void remove(final String name) {
    checkNotNull(name, "name");
    checkArgument(!ROOT.equals(name), ROOT + " logger cannot be removed");

    try {
      lock.writeLock().lock();
      logManager.unsetLoggerLevel(name);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void reset() {
    try {
      lock.writeLock().lock();
      logManager.resetLoggers();
    }
    finally {
      lock.writeLock().unlock();
    }
  }

}
