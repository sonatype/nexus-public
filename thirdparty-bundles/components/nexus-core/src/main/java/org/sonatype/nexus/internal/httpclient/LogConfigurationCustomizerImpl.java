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
package org.sonatype.nexus.internal.httpclient;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.log.LogConfigurationCustomizer;
import org.sonatype.nexus.common.log.LoggerLevel;

import static org.sonatype.nexus.internal.httpclient.HttpClientManagerImpl.HTTPCLIENT_OUTBOUND_LOGGER_NAME;

/**
 * HTTP-client {@link LogConfigurationCustomizer}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class LogConfigurationCustomizerImpl
    implements LogConfigurationCustomizer
{
  @Override
  public void customize(final Configuration config) {
    // NEXUS-6134: make it easy for user to debug outbound request headers
    config.setLoggerLevel("org.apache.http", LoggerLevel.INFO);
    config.setLoggerLevel("org.apache.http.wire", LoggerLevel.ERROR);

    config.setLoggerLevel("org.sonatype.nexus.httpclient", LoggerLevel.DEFAULT);
    config.setLoggerLevel("org.sonatype.nexus.internal.httpclient", LoggerLevel.DEFAULT);

    config.setLoggerLevel(HTTPCLIENT_OUTBOUND_LOGGER_NAME, LoggerLevel.DEFAULT);
  }
}
