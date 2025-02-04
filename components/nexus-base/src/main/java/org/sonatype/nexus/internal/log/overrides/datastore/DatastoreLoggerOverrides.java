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
package org.sonatype.nexus.internal.log.overrides.datastore;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.common.log.LoggerOverridesReloadEvent;
import org.sonatype.nexus.internal.log.overrides.LogbackLoggerOverridesSupport;
import org.sonatype.nexus.internal.log.LoggerOverrides;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * {@link LoggerOverrides} datastore implementation.
 */
@Named
@Singleton
@ManagedLifecycle(phase = STORAGE)
public class DatastoreLoggerOverrides
    extends LogbackLoggerOverridesSupport
    implements LoggerOverrides
{
  private final LoggingOverridesStore loggingLevelsStore;

  private final EventManager eventManager;

  private final Map<String, LoggerLevel> loggerLevels = new ConcurrentHashMap<>();

  private final ReentrantReadWriteLock loggerLevelsLock = new ReentrantReadWriteLock();

  @Inject
  public DatastoreLoggerOverrides(
      final ApplicationDirectories appDirectories,
      final LoggingOverridesStore loggingLevelsStore,
      final EventManager eventManager)
  {
    super(appDirectories);
    this.loggingLevelsStore = checkNotNull(loggingLevelsStore);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public void load() {
    loggerLevels.clear();
    if (logbackFileExists()) {
      try {
        loggerLevels.putAll(readFromFile());
      }
      catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected void doStart() throws Exception {
    log.debug("Load overrides from datastore");
    syncWithDBAndGet();

    // if override level is loaded from xml file but not presented in db - migrate it

    List<LoggingOverridesData> levelsMigrateToDb = loggerLevels.entrySet()
        .stream()
        .filter(entry -> !loggingLevelsStore.exists(entry.getKey()))
        .map(entry -> new LoggingOverridesData(entry.getKey(), entry.getValue().toString()))
        .collect(Collectors.toList());

    // notify overrides were reloaded
    eventManager.post(new LoggerOverridesReloadEvent());

    levelsMigrateToDb.forEach(loggingLevelsStore::create);
  }

  @Override
  public Map<String, LoggerLevel> syncWithDBAndGet() {
    loggerLevelsLock.writeLock().lock();
    Continuation<LoggingOverridesData> levels = loggingLevelsStore.readRecords();
    levels.forEach(data -> loggerLevels.put(data.getName(), LoggerLevel.valueOf(data.getLevel())));
    loggerLevelsLock.writeLock().unlock();
    return loggerLevels;
  }

  @Override
  public void reset() {
    loggerLevels.clear();
    loggingLevelsStore.deleteAllRecords();
  }

  @Override
  public void set(final String name, final LoggerLevel level) {
    loggerLevels.put(name, level);

    LoggingOverridesData data = new LoggingOverridesData(name, level.toString());
    if (loggingLevelsStore.exists(name)) {
      loggingLevelsStore.update(data);
    }
    else {
      loggingLevelsStore.create(data);
    }
  }

  @Nullable
  @Override
  public LoggerLevel get(final String name) {
    return loggerLevels.get(name);
  }

  @Nullable
  @Override
  public LoggerLevel remove(final String name) {
    LoggerLevel removed = loggerLevels.remove(name);
    loggingLevelsStore.deleteByName(name);
    return removed;
  }

  @Override
  public boolean contains(final String name) {
    return loggerLevels.containsKey(name);
  }

  @Override
  public Iterator<Entry<String, LoggerLevel>> iterator() {
    return ImmutableMap.copyOf(loggerLevels).entrySet().iterator();
  }

  @Override
  public void save() {
    // empty. method 'set' writes data to db directly
  }
}
