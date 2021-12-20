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
package org.sonatype.nexus.plugins.lvo.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.lvo.config.model.Configuration;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NEXUS-6197 and NEXUS-6099: UT for configuration upgrade.
 */
public class DefaultLvoPluginConfigurationTest
    extends TestSupport
{
  File configDirectory;

  File lvoConfigurationFile;

  ApplicationConfiguration applicationConfiguration;

  @Before
  public void prepare() {
    configDirectory = new File("target/test-classes");
    lvoConfigurationFile = new File(configDirectory, "lvo-plugin.xml");
    applicationConfiguration = mock(ApplicationConfiguration.class);
    when(applicationConfiguration.getConfigurationDirectory())
        .thenReturn(configDirectory);
  }

  @Test
  public void perform100Upgrade() throws Exception {
    Files.copy(new File(configDirectory, "lvo-plugin-100.xml").toPath(), lvoConfigurationFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING);

    final DefaultLvoPluginConfiguration config = new DefaultLvoPluginConfiguration(applicationConfiguration);
    final Configuration configuration = config.getConfiguration();
    assertThat(configuration.getVersion(), equalTo(Configuration.MODEL_VERSION));
    assertThat(configuration.getLvoKeys(), hasSize(2));
  }

  @Test
  public void perform101Upgrade() throws Exception {
    Files.copy(new File(configDirectory, "lvo-plugin-101.xml").toPath(), lvoConfigurationFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING);

    final DefaultLvoPluginConfiguration config = new DefaultLvoPluginConfiguration(applicationConfiguration);
    final Configuration configuration = config.getConfiguration();
    assertThat(configuration.getVersion(), equalTo(Configuration.MODEL_VERSION));
    assertThat(configuration.getLvoKeys(), hasSize(2));
  }
}
