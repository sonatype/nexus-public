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
package org.sonatype.nexus.plugins.siesta.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.log.LogConfigurationCustomizer;

import static org.sonatype.nexus.log.LoggerLevel.DEFAULT;
import static org.sonatype.nexus.log.LoggerLevel.WARN;

/**
 * Siesta {@link LogConfigurationCustomizer}.
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
    configuration.setLoggerLevel("org.sonatype.sisu.siesta", DEFAULT);
    configuration.setLoggerLevel("org.sonatype.nexus.plugins.siesta", DEFAULT);

    // This will prevent duplicate 'Initiating Jersey application' messages, though won't omit them all... not sure why ATM.
    configuration.setLoggerLevel("com.sun.jersey.server.impl.application.WebApplicationImpl", WARN);

    // Jersey support (cdi,jmx) spits out exceptions at INFO when DEBUG is enabled; limit these to WARN to avoid this noise.
    configuration.setLoggerLevel("com.sun.jersey.server.impl.cdi.CDIComponentProviderFactoryInitializer", WARN);
    configuration.setLoggerLevel("com.sun.jersey.server.impl.ejb.EJBComponentProviderFactoryInitilizer", WARN);
    configuration.setLoggerLevel("com.sun.jersey.server.impl.managedbeans.ManagedBeanComponentProviderFactoryInitilizer", WARN);
  }
}