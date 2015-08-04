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
package org.sonatype.nexus.bundle.launcher;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.sonatype.sisu.bl.WebBundleConfiguration;

/**
 * An Nexus bundle configuration.
 *
 * @since 2.0
 */
public interface NexusBundleConfiguration
    extends WebBundleConfiguration<NexusBundleConfiguration>
{

  /**
   * Returns additional plugins to be installed in Nexus.
   * <p/>
   * Plugins can be zips/jars/tars to be unpacked or directories to be copied
   *
   * @return Nexus plugins to be installed
   * @since 2.0
   */
  List<File> getPlugins();

  /**
   * Sets plugins to be installed in Nexus. Provided plugins will overwrite existing configured plugins.
   * <p/>
   * Plugins can be zips/jars/tars to be unpacked or directories to be copied
   *
   * @param plugins Nexus plugins to be installed. Can be null, case when an empty list will be used
   * @return itself, for usage in fluent api
   * @since 2.0
   */
  NexusBundleConfiguration setPlugins(List<File> plugins);

  /**
   * Sets plugins to be installed in Nexus. Provided plugins will overwrite existing configured plugins.
   * <p/>
   * Plugins can be zips/jars/tars to be unpacked or directories to be copied
   *
   * @param plugins Nexus plugins to be installed
   * @return itself, for usage in fluent api
   * @since 2.0
   */
  NexusBundleConfiguration setPlugins(File... plugins);

  /**
   * Append plugins to existing set of plugins.
   * <p/>
   * Plugins can be zips/jars/tars to be unpacked or directories to be copied
   *
   * @param plugins Nexus plugins to be installed
   * @return itself, for usage in fluent api
   * @since 2.0
   */
  NexusBundleConfiguration addPlugins(File... plugins);

  /**
   * Sets log level.
   *
   * @param level one of TRACE/DEBUG/INFO/ERROR or {@code null} if bundle defaults should be used
   * @since 2.2
   */
  NexusBundleConfiguration setLogLevel(String level);

  /**
   * Gets log level, if configured
   *
   * @return one of TRACE/DEBUG/INFO/ERROR or {@code null} if bundle defaults should be used
   * @since 2.2
   */
  String getLogLevel();

  /**
   * Sets log level / logger name.
   *
   * @param loggerName logger name (cannot be null)
   * @param level      one of TRACE/DEBUG/INFO/ERROR (cannot be null)
   * @since 2.3
   */
  NexusBundleConfiguration setLogLevel(String loggerName, String level);

  /**
   * Gets the configured log levels / logger name.
   *
   * @return map between logger names and log level (should not be null)
   * @since 2.3
   */
  Map<String, String> getLogLevels();

  /**
   * Sets logging pattern.
   *
   * @param pattern logging pattern (cannot be null)
   * @since 2.3
   */
  NexusBundleConfiguration setLogPattern(String pattern);

  /**
   * Gets the configured logging pattern.
   *
   * @return logging pattern. Null in case default one should be used
   * @since 2.3
   */
  String getLogPattern();

  /**
   * Enables support for HTTPS with the given port, keystore, and password.
   * 
   * @param port SSL port
   * @param keystore Keystore location
   * @param password Keystore password
   */
  NexusBundleConfiguration enableHttps(int port, File keystore, String password);

  /**
   * Gets the SSL port, if configured.
   */
  int getSslPort();

  /**
   * Gets the keystore location, if configured.
   */
  File getKeystoreLocation();

  /**
   * Gets the keystore password, if configured.
   */
  String getKeystorePassword();

  /**
   * @return the webapp context path starting with slash or null if not set
   * @since 2.11.2
   */
  String getContextPath();

  /**
   * Set the context path where Nexus should run at.
   *
   * The path must start with forward slash.
   *
   * @param contextPath webapp context path to run at
   * @since 2.11.2
   */
  NexusBundleConfiguration setContextPath(final String contextPath);

}
