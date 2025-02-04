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

import java.nio.file.Path;
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class OpenCoreNexusEditionTest
    extends TestSupport
{
  @Mock
  private Path mockWorkDirPath;

  @InjectMocks
  private OpenCoreNexusEdition underTest;

  @Test
  public void testDoApply() {
    Properties properties = new Properties();
    properties.put(NexusEdition.NEXUS_FEATURES, "test-feature" + NexusEditionFeature.PRO_FEATURE.featureString);

    underTest.doApply(properties, mockWorkDirPath);

    assertEquals(NexusEditionType.OC.editionString, properties.getProperty(NexusEdition.NEXUS_EDITION));
    assertEquals("test-feature" + NexusEditionFeature.OC_FEATURE.featureString,
        properties.getProperty(NexusEdition.NEXUS_FEATURES));
  }
}
