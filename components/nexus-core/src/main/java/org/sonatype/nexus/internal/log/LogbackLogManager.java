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
package org.sonatype.nexus.internal.log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.NexusInitializedEvent;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.common.io.LimitedInputStream;
import org.sonatype.nexus.common.log.LogConfigurationCustomizer;
import org.sonatype.nexus.common.log.LogConfigurationCustomizer.Configuration;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.google.common.eventbus.Subscribe;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Logback {@link LogManager}.
 */
@Named
@Singleton
public class LogbackLogManager
    extends StateGuardLifecycleSupport
    implements LogManager
{
  private final List<LogConfigurationCustomizer> customizers;

  private final EventBus eventBus;

  private final Map<String, LoggerLevel> overrides;

  private final Map<String, LoggerLevel> customisations;

  private final File overridesFile;

  @Inject
  public LogbackLogManager(final ApplicationDirectories applicationDirectories,
                           final List<LogConfigurationCustomizer> customizers,
                           final EventBus eventBus)
  {
    checkNotNull(applicationDirectories);
    this.customizers = checkNotNull(customizers);
    this.eventBus = checkNotNull(eventBus);
    this.overrides = new HashMap<>();
    this.customisations = new HashMap<>();

    this.overridesFile = new File(applicationDirectories.getWorkDirectory("logback"), "logback-overrides.xml");
    log.debug("Overrides file: {}", overridesFile);

    eventBus.register(this);
  }

  @Override
  protected void doStart() throws Exception {
    configure();
  }

  /**
   * Re-configure after start to pick up customizations provided by plugins.
   */
  @Subscribe
  @Guarded(by = STARTED)
  public void on(final NexusInitializedEvent event) throws Exception {
    configure();
  }

  private void configure() {
    log.info("Configuring");
    applyCustomizers();
  }

  @Override
  protected void doStop() throws Exception {
    eventBus.unregister(this);

    // inform logback to shutdown
    loggerContext().stop();
  }

  @Override
  @Guarded(by = STARTED)
  public Set<File> getLogFiles() {
    HashSet<File> files = new HashSet<>();

    for (Appender appender : appenders()) {
      if (appender instanceof FileAppender) {
        String path = ((FileAppender) appender).getFile();
        files.add(new File(path));
      }
    }

    return files;
  }

  @Override
  @Nullable
  @Guarded(by = STARTED)
  public File getLogFile(final String fileName) {
    Set<File> files = getLogFiles();
    for (File file : files) {
      if (file.getName().equals(fileName)) {
        return file;
      }
    }
    return null;
  }

  @Override
  @Nullable
  @Guarded(by = STARTED)
  public InputStream getLogFileStream(final String fileName, final long from, final long count) throws IOException {
    log.debug("Retrieving log file: {}", fileName);

    if (fileName.contains(File.pathSeparator)) {
      log.warn("Nexus refuses to retrieve log files with path separators in its name.");
      return null;
    }

    File file = getLogFile(fileName);
    if (file == null || !file.exists()) {
      log.warn("Log file does not exist: {}", fileName);
      return null;
    }

    long fromByte = from;
    long bytesCount = count;
    if (count < 0) {
      bytesCount = Math.abs(count);
      fromByte = Math.max(0, file.length() - bytesCount);
    }

    InputStream input = new BufferedInputStream(new FileInputStream(file));
    if (fromByte == 0 && bytesCount >= file.length()) {
      return input;
    }
    else {
      return new LimitedInputStream(input, fromByte, bytesCount);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public Map<String, LoggerLevel> getLoggers() {
    Map<String, LoggerLevel> loggers = new HashMap<>();

    LoggerContext ctx = loggerContext();
    for (ch.qos.logback.classic.Logger logger : ctx.getLoggerList()) {
      String name = logger.getName();
      Level level = logger.getLevel();
      // only include loggers which explicit levels configured
      if (level != null) {
        loggers.put(name, LogbackLevels.convert(level));
      }
    }

    for (Entry<String, LoggerLevel> entry : customisations.entrySet()) {
      if (LoggerLevel.DEFAULT.equals(entry.getValue()) && !loggers.containsKey(entry.getKey())) {
        loggers.put(entry.getKey(), getLoggerEffectiveLevel(entry.getKey()));
      }
    }

    return loggers;
  }

  @Override
  @Guarded(by = STARTED)
  public void resetLoggers() {
    log.debug("Resetting loggers");

    for (Map.Entry<String, LoggerLevel> entry : overrides.entrySet()) {
      if (!Logger.ROOT_LOGGER_NAME.equals(entry.getKey())) {
        setLogbackLoggerLevel(entry.getKey(), null);
      }
    }
    overrides.clear();
    LogbackOverrides.write(overridesFile, overrides);
    setLoggerLevel(Logger.ROOT_LOGGER_NAME, LoggerLevel.DEFAULT);
    applyCustomisations();

    log.debug("Loggers reset to their default levels");
  }

  //
  // Logger levels
  //

  @Override
  @Guarded(by = STARTED)
  public void setLoggerLevel(final String name, final @Nullable LoggerLevel level) {
    if (level == null) {
      unsetLoggerLevel(name);
      return;
    }

    log.debug("Set logger level: {}={}", name, level);
    LoggerLevel calculated = null;

    if (Logger.ROOT_LOGGER_NAME.equals(name)) {
      calculated = LoggerLevel.DEFAULT.equals(level) ? LoggerLevel.INFO : level;
      overrides.put(name, calculated);
    }
    else {
      // else we customize the logger overrides configuration
      if (LoggerLevel.DEFAULT.equals(level)) {
        boolean customizedByUser = overrides.containsKey(name) && !customisations.containsKey(name);
        unsetLoggerLevel(name);
        if (customizedByUser) {
          overrides.put(name, calculated = getLoggerEffectiveLevel(name));
          LogbackOverrides.write(overridesFile, overrides);
        }
        else {
          LoggerLevel customizedLevel = customisations.get(name);
          if (customizedLevel != null && !LoggerLevel.DEFAULT.equals(customizedLevel)) {
            calculated = customizedLevel;
          }
        }
      }
      else {
        overrides.put(name, calculated = level);
      }
    }

    LogbackOverrides.write(overridesFile, overrides);

    if (calculated != null) {
      setLogbackLoggerLevel(name, LogbackLevels.convert(calculated));
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void unsetLoggerLevel(final String name) {
    log.debug("Unset logger level: {}", name);

    if (overrides.remove(name) != null) {
      LogbackOverrides.write(overridesFile, overrides);
    }
    if (Logger.ROOT_LOGGER_NAME.equals(name)) {
      setLoggerLevel(name, LoggerLevel.DEFAULT);
    }
    else {
      setLogbackLoggerLevel(name, null);
    }
  }

  @Override
  @Nullable
  @Guarded(by = STARTED)
  public LoggerLevel getLoggerLevel(final String name) {
    Level level = loggerContext().getLogger(name).getLevel();
    if (level != null) {
      return LogbackLevels.convert(level);
    }
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public LoggerLevel getLoggerEffectiveLevel(final String name) {
    Level level = loggerContext().getLogger(name).getEffectiveLevel();
    return LogbackLevels.convert(level);
  }

  /**
   * Set named logback logger level.
   */
  private void setLogbackLoggerLevel(final String name, final Level level) {
    loggerContext().getLogger(name).setLevel(level);
  }

  //
  // Customizations
  //

  /**
   * Ask all configured {@link LogConfigurationCustomizer customiers} to register customisations.
   */
  private void applyCustomizers() {
    Configuration config = new Configuration()
    {
      @Override
      public void setLoggerLevel(final String name, final LoggerLevel level) {
        customisations.put(checkNotNull(name), checkNotNull(level));
      }
    };

    customisations.clear();
    for (LogConfigurationCustomizer customizer : customizers) {
      log.debug("Applying customizer: {}", customizer);
      customizer.customize(config);
    }
  }

  /**
   * Apply all registered customizations.
   */
  private void applyCustomisations() {
    for (Entry<String, LoggerLevel> entry : customisations.entrySet()) {
      if (!LoggerLevel.DEFAULT.equals(entry.getValue())) {
        setLogbackLoggerLevel(entry.getKey(), LogbackLevels.convert(entry.getValue()));
      }
    }
  }

  //
  // Helpers
  //

  /**
   * Returns the current logger-context.
   */
  private static LoggerContext loggerContext() {
    ILoggerFactory factory = LoggerFactory.getILoggerFactory();
    if (factory instanceof LoggerContext) {
      return (LoggerContext) factory;
    }
    // temporary workaround for situations where SLF4j is not backed by logback
    return (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
  }

  /**
   * Returns all configured appenders.
   */
  private static Collection<Appender> appenders() {
    List<Appender> result = new ArrayList<>();
    for (Logger l : loggerContext().getLoggerList()) {
      ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) l;
      Iterator<Appender<ILoggingEvent>> iter = log.iteratorForAppenders();
      while (iter.hasNext()) {
        result.add(iter.next());
      }
    }
    return result;
  }
}
