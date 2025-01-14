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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_FEATURES;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_LOAD_AS_CE_PROP_NAME;

public class ProNexusEditionTest
    extends TestSupport
{
  @Spy
  private ProNexusEdition underTest = new ProNexusEdition();

  @Mock
  private Path workDirPath;

  @Mock
  private Path proPath;

  @Mock
  private File proEditionMarker;

  @Before
  public void setUp() {
    when(workDirPath.resolve("edition_pro")).thenReturn(proPath);
    when(proPath.toFile()).thenReturn(proEditionMarker);
  }

  @Test
  public void testDoesApply_shouldSwitchToFree() {
    Properties properties = new Properties();
    properties.setProperty(NEXUS_FEATURES, NexusEditionFeature.PRO_FEATURE.featureString);
    when(underTest.shouldSwitchToFree(workDirPath)).thenReturn(true);
    when(underTest.doesApply(properties, workDirPath)).thenReturn(false);
  }

  @Test
  public void testDoesApply_shouldSwitchToFreeFalse_loadAsCeTrue() {
    Properties properties = new Properties();
    properties.setProperty(NEXUS_FEATURES, NexusEditionFeature.PRO_FEATURE.featureString);
    properties.setProperty(NEXUS_LOAD_AS_CE_PROP_NAME, "true");

    when(underTest.shouldSwitchToFree(workDirPath)).thenReturn(false);
    when(underTest.doesApply(properties, workDirPath)).thenReturn(false);
  }

  @Test
  public void testDoesApply_shouldSwitchToFreeFalse_loadAsCeFalse() {
    Properties properties = new Properties();
    properties.setProperty(NEXUS_FEATURES, NexusEditionFeature.PRO_FEATURE.featureString);
    properties.setProperty(NEXUS_LOAD_AS_CE_PROP_NAME, "false");

    when(underTest.shouldSwitchToFree(workDirPath)).thenReturn(false);
    when(underTest.doesApply(properties, workDirPath)).thenReturn(true);
  }
}
