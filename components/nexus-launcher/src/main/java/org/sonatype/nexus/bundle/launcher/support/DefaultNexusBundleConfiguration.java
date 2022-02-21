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
package org.sonatype.nexus.bundle.launcher.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.sisu.bl.jmx.JMXConfiguration;
import org.sonatype.sisu.bl.support.DefaultWebBundleConfiguration;
import org.sonatype.sisu.bl.support.resolver.BundleResolver;
import org.sonatype.sisu.bl.support.resolver.TargetDirectoryResolver;
import org.sonatype.sisu.filetasks.FileTask;
import org.sonatype.sisu.filetasks.FileTaskBuilder;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.tools.ant.taskdefs.condition.Os;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;
import static org.sonatype.sisu.filetasks.builder.FileRef.path;

/**
 * Default Nexus bundle configuration.
 *
 * @since 2.0
 */
@Named
public class DefaultNexusBundleConfiguration
    extends DefaultWebBundleConfiguration<NexusBundleConfiguration>
    implements NexusBundleConfiguration
{
  /**
   * Default start timeout value in seconds.
   */
  public static final int DEFAULT_START_TIMEOUT = 120;

  /**
   * Start timeout configuration property key.
   */
  public static final String START_TIMEOUT = "nexus.launcher.startTimeout";

  /**
   * Default log level.
   */
  private static final String DEFAULT_LOG_LEVEL = "INFO";

  /**
   * Default logging pattern.
   */
  private static final String DEFAULT_LOG_PATTERN = "%d{\"yyyy-MM-dd HH:mm:ss,SSSZ\"} %-5p [%thread] - %c - %m%n";

  /**
   * File task builder.
   * Cannot be null.
   */
  private final FileTaskBuilder fileTaskBuilder;

  /**
   * List of Nexus plugins to be installed. Should never be null.
   */
  private final List<File> plugins;

  /**
   * One of TRACE/DEBUG/INFO/ERROR or {@code null} if bundle defaults should be used.
   * Can be null.
   */
  private String logLevel;

  /**
   * Map between logger name and logging level.
   */
  private Map<String, String> logLevelsPerLoggerName;

  /**
   * Logging pattern. When null, default one should be used.
   */
  private String logPattern;

  /**
   * Optional SSL port.
   */
  private int sslPort = -1;

  /**
   * Optional keystore location.
   */
  private File keystoreLocation;

  /**
   * Optional keystore password.
   */
  private String keystorePassword;

  /**
   * Webapp context path where Nexus Bundle should be started on
   */
  private String contextPath;

  @Inject
  public DefaultNexusBundleConfiguration(final FileTaskBuilder fileTaskBuilder,
                                         final Provider<JMXConfiguration> jmxConfigurationProvider)
  {
    super(jmxConfigurationProvider);
    this.fileTaskBuilder = checkNotNull(fileTaskBuilder);
    this.plugins = Lists.newArrayList();
    this.logLevelsPerLoggerName = Maps.newHashMap();
  }

  /**
   * Sets number of seconds to wait for Nexus to boot. If injected will use the timeout bounded to
   * {@link #START_TIMEOUT} with a default of {@link #DEFAULT_START_TIMEOUT} seconds.
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Inject
  protected void configureNexusStartTimeout(
      final @Nullable @Named("${" + START_TIMEOUT + ":-" + DEFAULT_START_TIMEOUT + "}") Integer startTimeout)
  {
    if (startTimeout != null) {
      super.setStartTimeout(startTimeout);
    }
  }

  /**
   * Sets a Nexus specific bundle resolver.
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Inject
  protected void setBundleResolver(final @Nullable @NexusSpecific BundleResolver bundleResolver) {
    super.setBundleResolver(bundleResolver);
  }

  /**
   * Sets a Nexus specific target directory resolver.
   * <p/>
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Inject
  protected void setTargetDirectoryResolver(
      final @Nullable @NexusSpecific TargetDirectoryResolver targetDirectoryResolver)
  {
    super.setTargetDirectoryResolver(targetDirectoryResolver);
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public List<File> getPlugins() {
    return plugins;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public NexusBundleConfiguration setPlugins(final List<File> plugins) {
    this.plugins.clear();
    if (plugins != null) {
      this.plugins.addAll(plugins);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public NexusBundleConfiguration setPlugins(final File... plugins) {
    return setPlugins(Arrays.asList(plugins));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0
   */
  @Override
  public NexusBundleConfiguration addPlugins(final File... plugins) {
    this.plugins.addAll(Arrays.asList(plugins));
    return this;
  }

  @Override
  public NexusBundleConfiguration setLogLevel(final String level) {
    logLevel = level;
    return this;
  }

  @Override
  public String getLogLevel() {
    return logLevel;
  }

  @Override
  public NexusBundleConfiguration setLogLevel(final String loggerName, final String level) {
    logLevelsPerLoggerName.put(checkNotNull(loggerName), checkNotNull(level));
    return this;
  }

  @Override
  public Map<String, String> getLogLevels() {
    return logLevelsPerLoggerName;
  }

  @Override
  public NexusBundleConfiguration setLogPattern(final String pattern) {
    this.logPattern = checkNotNull(pattern);
    return this;
  }

  @Override
  public String getLogPattern() {
    return logPattern;
  }

  @Override
  public List<FileTask> getOverlays() {
    final List<FileTask> overlays = Lists.newArrayList(super.getOverlays());

    for (final File plugin : getPlugins()) {
      if (plugin.isDirectory()) {
        overlays.add(
            fileTaskBuilder.copy()
                .directory(file(plugin))
                .to().directory(path("sonatype-work/nexus/plugin-repository"))
        );
      }
      else {
        overlays.add(
            fileTaskBuilder.expand(file(plugin))
                .to().directory(path("sonatype-work/nexus/plugin-repository"))
        );
      }
    }

    if (getLogLevel() != null || getLogPattern() != null) {
      overlays.add(
          fileTaskBuilder.properties(path("sonatype-work/nexus/conf/logback.properties"))
              .property("root.level", getLogLevel() == null ? DEFAULT_LOG_LEVEL : getLogLevel())
              .property("appender.pattern", getLogPattern() == null ? DEFAULT_LOG_PATTERN : getLogPattern())
              .property("appender.file", "${nexus.log-config-dir}/../logs/nexus.log")
      );
    }

    final Map<String, String> logLevels = getLogLevels();
    if (logLevels != null && !logLevels.isEmpty()) {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (PrintWriter writer = new PrintWriter(baos)) {
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println();
        writer.println("<included>");
        for (final Map.Entry<String, String> entry : logLevels.entrySet()) {
          writer.printf("  <logger name=\"%s\" level=\"%s\" />", entry.getKey(), entry.getValue());
          writer.println();
        }
        writer.println("</included>");
        writer.flush();
        overlays.add(
            fileTaskBuilder.create().file(path("sonatype-work/nexus/conf/logback-test.xml"))
                .containing(baos.toString("UTF-8"))
                .encodedAs("UTF-8")
        );
      }
      catch (UnsupportedEncodingException e) {
        throw Throwables.propagate(e);
      }
    }

    if (keystoreLocation != null) {
      overlays.add(
          fileTaskBuilder.replace()
              .inFile(path("nexus/conf/jetty-https.xml"))
              .replace(
                  "./conf/ssl/keystore.jks",
                  keystoreLocation.getAbsolutePath()
              )
              .replace(
                  ">password<",
                  ">" + keystorePassword + "<"
              )
              .failIfFileDoesNotExist()
      );
    }

    return overlays;
  }

  @Override
  public NexusBundleConfiguration enableHttps(final int port, final File keystore, final String password) {
    sslPort = port;
    keystoreLocation = keystore;
    keystorePassword = password;
    return this;
  }

  @Override
  public int getSslPort() {
    return sslPort;
  }

  @Override
  public File getKeystoreLocation() {
    return keystoreLocation;
  }

  @Override
  public String getKeystorePassword() {
    return keystorePassword;
  }

  @Override
  public NexusBundleConfiguration setContextPath(final String contextPath) {
    checkArgument(contextPath.startsWith("/"), "context path must start with forward slash");
    this.contextPath = contextPath;
    return this;
  }

  @Override
  public String getContextPath() {
    return this.contextPath;
  }
}
