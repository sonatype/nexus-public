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
package org.sonatype.nexus.log.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.sonatype.nexus.LimitedInputStream;
import org.sonatype.nexus.NexusStreamResponse;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.log.DefaultLogConfiguration;
import org.sonatype.nexus.log.DefaultLogManagerMBean;
import org.sonatype.nexus.log.LogConfiguration;
import org.sonatype.nexus.log.LogConfigurationCustomizer;
import org.sonatype.nexus.log.LogConfigurationCustomizer.Configuration;
import org.sonatype.nexus.log.LogConfigurationParticipant;
import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.nexus.proxy.events.NexusInitializedEvent;
import org.sonatype.nexus.util.file.FileSupport;
import org.sonatype.nexus.util.io.StreamSupport;
import org.sonatype.sisu.goodies.common.io.FileReplacer;
import org.sonatype.sisu.goodies.common.io.FileReplacer.ContentWriter;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

//TODO configuration operations should be locking

/**
 * @author cstamas
 * @author juven
 * @author adreghiciu@gmail.com
 */
@Singleton
@Named
public class LogbackLogManager
    implements LogManager
{
  private static final String JMX_DOMAIN = "org.sonatype.nexus.log";

  private static final String KEY_APPENDER_FILE = "appender.file";

  private static final String KEY_APPENDER_PATTERN = "appender.pattern";

  private static final String KEY_ROOT_LEVEL = "root.level";

  private static final String KEY_LOG_CONFIG_DIR = "nexus.log-config-dir";

  private static final String LOG_CONF = "logback.xml";

  private static final String LOG_CONF_PROPS = "logback.properties";

  private static final String LOG_CONF_PROPS_RESOURCE = "/META-INF/log/" + LOG_CONF_PROPS;

  private final Logger logger = LoggerFactory.getLogger(LogbackLogManager.class);

  private final Injector injector;

  private final ApplicationConfiguration applicationConfiguration;

  private final List<LogConfigurationParticipant> logConfigurationParticipants;

  private final List<LogConfigurationCustomizer> logConfigurationCustomizers;

  private final NexusLoggerContextListener loggerContextListener;

  private final EventBus eventBus;

  private final Map<String, LoggerLevel> overrides;

  private final Map<String, LoggerLevel> customisations;

  private ObjectName jmxName;

  @Inject
  public LogbackLogManager(final Injector injector,
                           final ApplicationConfiguration applicationConfiguration,
                           final List<LogConfigurationParticipant> logConfigurationParticipants,
                           final List<LogConfigurationCustomizer> logConfigurationCustomizers,
                           final EventBus eventBus)
  {
    this.injector = checkNotNull(injector);
    this.applicationConfiguration = checkNotNull(applicationConfiguration);
    this.logConfigurationParticipants = checkNotNull(logConfigurationParticipants);
    this.logConfigurationCustomizers = checkNotNull(logConfigurationCustomizers);
    this.loggerContextListener = new NexusLoggerContextListener();
    this.eventBus = checkNotNull(eventBus);
    this.overrides = Maps.newHashMap();
    this.customisations = Maps.newHashMap();
    try {
      jmxName = ObjectName.getInstance(JMX_DOMAIN, "name", LogManager.class.getSimpleName());
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      server.registerMBean(new DefaultLogManagerMBean(this), jmxName);
    }
    catch (Exception e) {
      jmxName = null;
      logger.warn("Problem registering MBean for: " + getClass().getName(), e);
    }
    eventBus.register(this);
  }

  @Subscribe
  public void on(final NexusInitializedEvent evt) {
    configure();
  }

  private LoggerContext getLoggerContext() {
    return (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public synchronized void configure() {
    // TODO maybe do some optimization that if participants does not change, do not reconfigure
    prepareConfigurationFiles();
    readCustomisations();
    overrides.clear();
    File logOverridesConfigFile = getLogOverridesConfigFile();
    if (logOverridesConfigFile.exists()) {
      overrides.putAll(LogbackOverrides.read(logOverridesConfigFile));
    }
    mayInstallNexusLoggerContextListener();
    reconfigure();
  }

  @Override
  public synchronized void shutdown() {
    if (null != jmxName) {
      try {
        ManagementFactory.getPlatformMBeanServer().unregisterMBean(jmxName);
      }
      catch (final Exception e) {
        logger.warn("Problem unregistering MBean for: " + getClass().getName(), e);
      }
    }
    eventBus.unregister(this);
  }

  /**
   * @since 2.7
   */
  private File getLogConfigFile(final String name) {
    return new File(getLogConfigDir(), name);
  }

  /**
   * @since 2.7
   */
  private File getLogOverridesConfigFile() {
    return getLogConfigFile("logback-overrides.xml");
  }

  /**
   * @since 2.7
   */
  @Override
  public Map<String, LoggerLevel> getLoggers() {
    Map<String, LoggerLevel> loggers = Maps.newHashMap();

    LoggerContext loggerContext = getLoggerContext();
    for (ch.qos.logback.classic.Logger logger : loggerContext.getLoggerList()) {
      String name = logger.getName();
      Level level = logger.getLevel();
      // only include loggers which explicit levels configured
      if (level != null) {
        loggers.put(name, convert(level));
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
  public Set<File> getLogFiles() {
    HashSet<File> files = new HashSet<File>();

    LoggerContext ctx = getLoggerContext();

    for (Logger l : ctx.getLoggerList()) {
      ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) l;
      Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders();

      while (it.hasNext()) {
        Appender<ILoggingEvent> ap = it.next();

        if (ap instanceof FileAppender<?> || ap instanceof RollingFileAppender<?>) {
          FileAppender<?> fileAppender = (FileAppender<?>) ap;
          String path = fileAppender.getFile();
          files.add(new File(path));
        }
      }
    }

    return files;
  }

  @Override
  public File getLogFile(String filename) {
    Set<File> logFiles = getLogFiles();

    for (File logFile : logFiles) {
      if (logFile.getName().equals(filename)) {
        return logFile;
      }
    }

    return null;
  }

  @Override
  public LogConfiguration getConfiguration()
      throws IOException
  {
    Properties logProperties = loadConfigurationProperties();
    DefaultLogConfiguration configuration = new DefaultLogConfiguration();

    configuration.setRootLoggerLevel(logProperties.getProperty(KEY_ROOT_LEVEL));
    // TODO
    configuration.setRootLoggerAppenders("console,file");
    configuration.setFileAppenderPattern(logProperties.getProperty(KEY_APPENDER_PATTERN));
    configuration.setFileAppenderLocation(logProperties.getProperty(KEY_APPENDER_FILE));

    return configuration;
  }

  @Override
  public void setConfiguration(LogConfiguration configuration)
      throws IOException
  {
    Properties logProperties = loadConfigurationProperties();

    logProperties.setProperty(KEY_ROOT_LEVEL, configuration.getRootLoggerLevel());
    String pattern = configuration.getFileAppenderPattern();

    if (pattern == null) {
      pattern = getDefaultProperties().getProperty(KEY_APPENDER_PATTERN);
    }

    logProperties.setProperty(KEY_APPENDER_PATTERN, pattern);

    saveConfigurationProperties(logProperties);
    // TODO this will do a reconfiguration but would be just enough to "touch" logback.xml"
    reconfigure();
  }

  private Properties getDefaultProperties()
      throws IOException
  {
    Properties properties = new Properties();
    final InputStream stream = this.getClass().getResourceAsStream(LOG_CONF_PROPS_RESOURCE);
    try {
      properties.load(stream);
    }
    finally {
      stream.close();
    }
    return properties;
  }

  /**
   * Retrieves a stream to the requested log file. This method ensures that the file is rooted in the log folder to
   * prevent browsing of the file system.
   *
   * @param logFile path of the file to retrieve
   * @returns InputStream to the file or null if the file is not allowed or doesn't exist.
   */
  @Override
  public NexusStreamResponse getApplicationLogAsStream(String logFile, long from, long count)
      throws IOException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Retrieving " + logFile + " log file.");
    }

    if (logFile.contains(File.pathSeparator)) {
      logger.warn("Nexus refuses to retrieve log files with path separators in its name.");

      return null;
    }

    File log = getLogFile(logFile);

    if (log == null || !log.exists()) {
      logger.warn("Log file does not exist: [" + logFile + "]");

      return null;
    }

    NexusStreamResponse response = new NexusStreamResponse();

    response.setName(logFile);

    response.setMimeType("text/plain");

    response.setSize(log.length());

    if (count >= 0) {
      response.setFromByte(from);
      response.setBytesCount(count);
    }
    else {
      response.setBytesCount(Math.abs(count));
      response.setFromByte(Math.max(0, response.getSize() - response.getBytesCount()));
    }

    response.setInputStream(
        new LimitedInputStream(new FileInputStream(log), response.getFromByte(), response.getBytesCount())
    );

    return response;
  }

  private Properties loadConfigurationProperties()
      throws IOException
  {
    prepareConfigurationFiles();
    String logConfigDir = getLogConfigDir();
    File logConfigPropsFile = new File(logConfigDir, LOG_CONF_PROPS);
    try (final InputStream in = new FileInputStream(logConfigPropsFile)) {
      Properties properties = new Properties();
      properties.load(in);
      return properties;
    }
  }

  private void saveConfigurationProperties(final Properties properties)
      throws IOException
  {
    final File configurationFile = new File(getLogConfigDir(), LOG_CONF_PROPS);
    logger.debug("Saving configuration: {}", configurationFile);
    final FileReplacer fileReplacer = new FileReplacer(configurationFile);
    // we save this file many times, don't litter backups
    fileReplacer.setDeleteBackupFile(true);
    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output)
          throws IOException
      {
        properties.store(output, "Saved by Nexus");
      }
    });
  }

  private String getLogConfigDir() {
    String logConfigDir = System.getProperty(KEY_LOG_CONFIG_DIR);

    if (Strings.isNullOrEmpty(logConfigDir)) {
      logConfigDir = applicationConfiguration.getConfigurationDirectory().getAbsolutePath();

      System.setProperty(KEY_LOG_CONFIG_DIR, logConfigDir);
    }

    return logConfigDir;
  }

  private void prepareConfigurationFiles() {
    String logConfigDir = getLogConfigDir();

    File logConfigPropsFile = new File(logConfigDir, LOG_CONF_PROPS);
    if (!logConfigPropsFile.exists()) {
      try {
        URL configUrl = this.getClass().getResource(LOG_CONF_PROPS_RESOURCE);
        try (final InputStream is = configUrl.openStream()) {
          FileSupport.copy(is, logConfigPropsFile.toPath());
        }
      }
      catch (IOException e) {
        throw new IllegalStateException("Could not create logback.properties as "
            + logConfigPropsFile.getAbsolutePath());
      }
    }

    if (logConfigurationParticipants != null) {
      for (final LogConfigurationParticipant participant : logConfigurationParticipants) {
        final String name = participant.getName();
        final File logConfigFile = new File(logConfigDir, name);
        if (participant instanceof LogConfigurationParticipant.NonEditable || !logConfigFile.exists()) {
          try {
            final FileReplacer fileReplacer = new FileReplacer(logConfigFile);
            // we save this file many times, don't litter backups
            fileReplacer.setDeleteBackupFile(true);
            fileReplacer.replace(new ContentWriter()
            {
              @Override
              public void write(final BufferedOutputStream output)
                  throws IOException
              {
                try (final InputStream in = participant.getConfiguration()) {
                  StreamSupport.copy(in, output, StreamSupport.BUFFER_SIZE);
                }
              }
            });
          }
          catch (IOException e) {
            throw new IllegalStateException(String.format("Could not create %s as %s", name,
                logConfigFile.getAbsolutePath()), e);
          }
        }
      }
    }
    final File logConfigFile = new File(logConfigDir, LOG_CONF);
    try {
      final FileReplacer fileReplacer = new FileReplacer(logConfigFile);
      // we save this file many times, don't litter backups
      fileReplacer.setDeleteBackupFile(true);
      fileReplacer.replace(new ContentWriter()
      {
        @Override
        public void write(final BufferedOutputStream output)
            throws IOException
        {
          try (final PrintWriter out = new PrintWriter(output)) {
            out.println("<?xml version='1.0' encoding='UTF-8'?>");
            out.println();
            out.println("<!--");
            out.println(
                "    DO NOT EDIT - This file aggregates log configuration from Nexus and its plugins, and is automatically generated.");
            out.println("-->");
            out.println();
            out.println("<configuration scan='true'>");
            out.println("  <property file='${nexus.log-config-dir}/logback.properties'/>");
            if (logConfigurationParticipants != null) {
              for (LogConfigurationParticipant participant : logConfigurationParticipants) {
                out.println(String.format(
                    "  <include file='${nexus.log-config-dir}/%s'/>", participant.getName())
                );
              }
            }
            File logOverridesConfigFile = getLogOverridesConfigFile();
            if (logOverridesConfigFile.exists()) {
              out.println(String.format(
                  "  <include file='${nexus.log-config-dir}/%s'/>", logOverridesConfigFile.getName())
              );
            }
            out.write("</configuration>");
          }
        }
      });
    }
    catch (IOException e) {
      throw new IllegalStateException("Could not create logback.xml as " + logConfigFile.getAbsolutePath());
    }
  }

  private void reconfigure() {
    String logConfigDir = getLogConfigDir();
    File file = new File(logConfigDir, LOG_CONF);
    logger.debug("Reconfiguring: {}", file);

    LoggerContext context = getLoggerContext();
    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      context.reset();
      context.getStatusManager().clear();
      installNonResetResistantListeners();
      configurator.doConfigure(file);
    }
    catch (JoranException e) {
      e.printStackTrace();
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    injectAppenders();
  }

  /**
   * Invoked after {@link LoggerContext#reset()} by logger manager, to reinstall all listeners that are non
   * reset resistant.
   *
   * @since 2.8
   */
  private void installNonResetResistantListeners() {
    installLevelChangePropagator();
  }

  /**
   * Installs {@link LevelChangePropagator} in context.

   * @since 2.8
   */
  private void installLevelChangePropagator() {
    LoggerContext context = getLoggerContext();
    final LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
    levelChangePropagator.setResetJUL(true);
    levelChangePropagator.setContext(context);
    context.addListener(levelChangePropagator);
  }


  private void injectAppenders() {
    LoggerContext ctx = getLoggerContext();

    for (Logger l : ctx.getLoggerList()) {
      ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) l;
      Iterator<Appender<ILoggingEvent>> it = log.iteratorForAppenders();

      while (it.hasNext()) {
        Appender<ILoggingEvent> ap = it.next();
        injector.injectMembers(ap);
      }
    }
  }

  /**
   * Convert a Logback {@link Level} into a {@link LoggerLevel}.
   */
  private LoggerLevel convert(final Level level) {
    switch (level.toInt()) {

      case Level.OFF_INT:
        return LoggerLevel.OFF;

      case Level.ERROR_INT:
        return LoggerLevel.ERROR;

      case Level.WARN_INT:
        return LoggerLevel.WARN;

      case Level.INFO_INT:
        return LoggerLevel.INFO;

      case Level.DEBUG_INT:
        return LoggerLevel.DEBUG;

      case Level.TRACE_INT:
        return LoggerLevel.TRACE;

      default:
        return LoggerLevel.TRACE;
    }
  }

  /**
   * Convert a {@link LoggerLevel} into a Logback {@link Level}.
   */
  private Level convert(final LoggerLevel level) {
    return Level.valueOf(level.name());
  }

  @Override
  public void setLoggerLevel(final String name, final @Nullable LoggerLevel level) {
    if (level == null) {
      unsetLoggerLevel(name);
      return;
    }

    logger.debug("Set logger level: {}={}", name, level);
    LoggerLevel calculated = null;
    if (Logger.ROOT_LOGGER_NAME.equals(name)) {
      try {
        calculated = LoggerLevel.DEFAULT.equals(level) ? LoggerLevel.INFO : level;
        Properties logProperties = loadConfigurationProperties();
        logProperties.setProperty(KEY_ROOT_LEVEL, calculated.name());
        saveConfigurationProperties(logProperties);
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
    else {
      if (LoggerLevel.DEFAULT.equals(level)) {
        boolean customizedByUser = overrides.containsKey(name) && !customisations.containsKey(name);
        unsetLoggerLevel(name);
        if (customizedByUser) {
          overrides.put(name, calculated = getLoggerEffectiveLevel(name));
          LogbackOverrides.write(getLogOverridesConfigFile(), overrides);
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
        LogbackOverrides.write(getLogOverridesConfigFile(), overrides);
      }
    }
    if (calculated != null) {
      setLogbackLoggerLevel(name, convert(calculated));
    }
  }

  @Override
  public void unsetLoggerLevel(final String name) {
    logger.debug("Unset logger level: {}", name);

    if (overrides.remove(name) != null) {
      LogbackOverrides.write(getLogOverridesConfigFile(), overrides);
    }
    if (Logger.ROOT_LOGGER_NAME.equals(name)) {
      setLoggerLevel(name, LoggerLevel.DEFAULT);
    }
    else {
      setLogbackLoggerLevel(name, null);
    }
  }

  @Override
  public void resetLoggers() {
    logger.debug("Resetting loggers");

    for (Map.Entry<String, LoggerLevel> entry : overrides.entrySet()) {
      if (!Logger.ROOT_LOGGER_NAME.equals(entry.getKey())) {
        setLogbackLoggerLevel(entry.getKey(), null);
      }
    }
    overrides.clear();
    LogbackOverrides.write(getLogOverridesConfigFile(), overrides);
    setLoggerLevel(Logger.ROOT_LOGGER_NAME, LoggerLevel.DEFAULT);
    applyCustomisations();

    logger.debug("Loggers reset to their default levels");
  }

  @Override
  @Nullable
  public LoggerLevel getLoggerLevel(final String name) {
    Level level = getLoggerContext().getLogger(name).getLevel();
    if (level != null) {
      return convert(level);
    }
    return null;
  }

  @Override
  public LoggerLevel getLoggerEffectiveLevel(final String name) {
    return convert(getLoggerContext().getLogger(name).getEffectiveLevel());
  }

  private void setLogbackLoggerLevel(final String name, final Level level) {
    getLoggerContext().getLogger(name).setLevel(level);
  }

  /**
   * Installs {@link NexusLoggerContextListener} if not already present in context.
   */
  private void mayInstallNexusLoggerContextListener() {
    LoggerContext context = getLoggerContext();
    if (!context.getCopyOfListenerList().contains(loggerContextListener)) {
      context.addListener(loggerContextListener);
      logger.debug("Nexus logger context listener installed");
    }
  }

  private void readCustomisations() {
    Configuration customizerConfiguration = new Configuration()
    {
      @Override
      public void setLoggerLevel(final String name, final LoggerLevel level) {
        customisations.put(checkNotNull(name), checkNotNull(level));
      }
    };
    customisations.clear();
    for (LogConfigurationCustomizer customizer : logConfigurationCustomizers) {
      customizer.customize(customizerConfiguration);
    }
  }

  private void applyCustomisations() {
    for (Entry<String, LoggerLevel> entry : customisations.entrySet()) {
      if (!LoggerLevel.DEFAULT.equals(entry.getValue())) {
        setLogbackLoggerLevel(entry.getKey(), convert(entry.getValue()));
      }
    }
  }

  private class NexusLoggerContextListener
      implements LoggerContextListener
  {

    @Override
    public boolean isResetResistant() {
      return true;
    }

    @Override
    public void onStart(final LoggerContext context) {
      // do nothing
    }

    @Override
    public void onReset(final LoggerContext context) {
      applyCustomisations();
    }

    @Override
    public void onStop(final LoggerContext context) {
      // do nothing
    }

    @Override
    public void onLevelChange(final ch.qos.logback.classic.Logger logger, final Level level) {
      // do nothing
    }
  }

}
