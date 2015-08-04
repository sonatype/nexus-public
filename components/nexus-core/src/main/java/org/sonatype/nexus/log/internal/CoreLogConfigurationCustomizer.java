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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.log.LogConfigurationCustomizer;
import org.sonatype.nexus.log.LoggerLevel;

/**
 * Configures core Nexus loggers.
 *
 * @since 2.7
 */
@Singleton
@Named
public class CoreLogConfigurationCustomizer
    implements LogConfigurationCustomizer
{

  @Override
  public void customize(final Configuration configuration) {
    // non Nexus loggers
    configuration.setLoggerLevel("org.restlet", LoggerLevel.INFO);
    configuration.setLoggerLevel("org.apache.commons", LoggerLevel.WARN);
    configuration.setLoggerLevel("org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter", LoggerLevel.INFO);
    configuration.setLoggerLevel("org.apache.shiro.web.filter.mgt.DefaultFilterChainManager", LoggerLevel.INFO);
    configuration.setLoggerLevel("org.eclipse.jetty", LoggerLevel.INFO);
    configuration.setLoggerLevel("eu.medsea.mimeutil.MimeUtil2", LoggerLevel.INFO);

    // NEXUS-6134: make it easy for user to debug outbound request headers
    configuration.setLoggerLevel("org.apache.http", LoggerLevel.INFO);
    configuration.setLoggerLevel("org.apache.http.wire", LoggerLevel.ERROR);

    // NEXUS-5456: limit noisy guice timing logger
    configuration.setLoggerLevel("com.google.inject.internal.util.Stopwatch", LoggerLevel.INFO);

    // NEXUS-5835: limit noisy jmx connections to Nexus when root.level is DEBUG
    configuration.setLoggerLevel("javax.management", LoggerLevel.INFO);
    configuration.setLoggerLevel("sun.rmi", LoggerLevel.INFO);

    // Nexus loggers
    configuration.setLoggerLevel("org.sonatype.nexus.rest.NexusApplication", LoggerLevel.WARN);

    // Useful loggers (level will be calculated as effective level)
    configuration.setLoggerLevel("org.sonatype.nexus", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.log", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.apachehttpclient", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.configuration", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.plugins", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.proxy", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.tasks", LoggerLevel.DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.threads", LoggerLevel.DEFAULT);

    // nexus-csrfguard loggers
    // HACK: Disable CSRFGuard support for now, its too problematic
    //configuration.setLoggerLevel("org.sonatype.nexus.csrfguard", LoggerLevel.DEFAULT);
    //configuration.setLoggerLevel("org.owasp.csrfguard", LoggerLevel.DEFAULT);
  }

}
