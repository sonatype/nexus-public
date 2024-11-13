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
package org.sonatype.nexus.internal.log.overrides.file;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.internal.log.overrides.LogbackLoggerOverridesSupport;
import org.sonatype.nexus.internal.log.LoggerOverrides;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

/**
 * Logback {@link LoggerOverrides} implementation.
 *
 * Special handling for {@code ROOT} logger, which is persisted as {@code root.level} property.
 *
 * @since 2.7
 */
@Named
@Singleton
public class LogbackLoggerOverrides
    extends LogbackLoggerOverridesSupport
    implements LoggerOverrides
{
  private final Map<String, LoggerLevel> loggerLevels = new HashMap<>();

  @Inject
  public LogbackLoggerOverrides(final ApplicationDirectories applicationDirectories) {
    super(applicationDirectories);
  }

  @VisibleForTesting
  LogbackLoggerOverrides(final File file) {
    super(file);
  }

  @Override
  public synchronized void load() {
    log.debug("Load");

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
  public synchronized void save() {
    log.debug("Save");

    try {
      writeToFile(loggerLevels);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, LoggerLevel> syncWithDBAndGet() {
    // Not applicable to Orient
    return loggerLevels;
  }

  @Override
  public synchronized void reset() {
    log.debug("Reset");

    loggerLevels.clear();
    try {
      writeToFile(loggerLevels);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void set(final String name, final LoggerLevel level) {
    log.debug("Set: {}={}", name, level);

    loggerLevels.put(name, level);
  }

  @Override
  @Nullable
  public synchronized LoggerLevel get(final String name) {
    return loggerLevels.get(name);
  }

  @Override
  @Nullable
  public synchronized LoggerLevel remove(final String name) {
    log.debug("Remove: {}", name);

    return loggerLevels.remove(name);
  }

  @Override
  public synchronized boolean contains(final String name) {
    return loggerLevels.containsKey(name);
  }

  @Override
  public synchronized Iterator<Entry<String, LoggerLevel>> iterator() {
    return ImmutableMap.copyOf(loggerLevels).entrySet().iterator();
  }
}
