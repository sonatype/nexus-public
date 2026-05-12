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
package org.sonatype.nexus.siesta.internal;

import org.sonatype.nexus.common.log.LogConfigurationCustomizer.Configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.common.log.LoggerLevel.DEFAULT;

class LogConfigurationCustomizerImplTest
{
  private LogConfigurationCustomizerImpl customizer;

  private Configuration configuration;

  @BeforeEach
  void setUp() {
    customizer = new LogConfigurationCustomizerImpl();
    configuration = mock(Configuration.class);
  }

  @Test
  void testCustomize() {
    customizer.customize(configuration);

    verify(configuration).setLoggerLevel("org.sonatype.nexus.rest", DEFAULT);
    verify(configuration).setLoggerLevel("org.sonatype.nexus.plugins.siesta", DEFAULT);
    verify(configuration).setLoggerLevel("org.jboss.resteasy", DEFAULT);
  }
}
