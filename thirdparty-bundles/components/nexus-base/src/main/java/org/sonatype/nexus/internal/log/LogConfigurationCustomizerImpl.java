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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.log.LogConfigurationCustomizer;
import org.sonatype.nexus.common.log.LoggerLevel;

/**
 * Configures core Nexus loggers.
 *
 * @since 2.7
 */
@Named
@Singleton
public class LogConfigurationCustomizerImpl
    implements LogConfigurationCustomizer
{
  @Override
  public void customize(final Configuration configuration) {
    // non Nexus loggers
    configuration.setLoggerLevel("org.apache.commons", LoggerLevel.WARN);

    configuration.setLoggerLevel("org.eclipse.jetty", LoggerLevel.INFO);
    configuration.setLoggerLevel("eu.medsea.mimeutil.MimeUtil2", LoggerLevel.INFO);

    // NEXUS-5456: limit noisy guice timing logger
    configuration.setLoggerLevel("com.google.inject.internal.util.Stopwatch", LoggerLevel.INFO);

    // NEXUS-5835: limit noisy jmx connections to Nexus when root.level is DEBUG
    configuration.setLoggerLevel("javax.management", LoggerLevel.INFO);
    configuration.setLoggerLevel("sun.rmi", LoggerLevel.INFO);

    // Useful loggers (level will be calculated as effective level)
    configuration.setLoggerLevel("org.sonatype.nexus", LoggerLevel.DEFAULT);

    configuration.setLoggerLevel("org.sonatype.nexus.jmx", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.internal.log", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.plugins", LoggerLevel.DEFAULT);
  }
}
