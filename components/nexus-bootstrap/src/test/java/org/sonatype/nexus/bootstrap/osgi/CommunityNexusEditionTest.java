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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_EDITION;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_FEATURES;

public class CommunityNexusEditionTest
    extends TestSupport
{
  @Mock
  private Bundle mockBundle;

  @Mock
  private BundleContext mockContext;

  @Mock
  private ServiceReference<FeaturesService> mockServiceReference;

  @Mock
  private FeaturesService mockFeaturesService;

  @Mock
  private Path mockWorkDirPath;

  @InjectMocks
  private CommunityNexusEdition underTest;

  @Test
  public void testDoesApplyIsFalse() {
    try (MockedStatic<FrameworkUtil> frameworkUtil = Mockito.mockStatic(FrameworkUtil.class)) {
      frameworkUtil.when(() -> FrameworkUtil.getBundle(CommunityNexusEdition.class)).thenReturn(mockBundle);
      when(mockBundle.getBundleContext()).thenReturn(mockContext);
      when(mockContext.getServiceReference(FeaturesService.class)).thenReturn(mockServiceReference);
      when(mockContext.getService(mockServiceReference)).thenReturn(mockFeaturesService);
      when(mockFeaturesService.getFeatures("nexus-community-edition")).thenReturn(new Feature[0]);

      boolean result = underTest.applies(new Properties(), mockWorkDirPath);

      assertFalse(result);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testDoesApplyIsTrue() {
    try (MockedStatic<FrameworkUtil> frameworkUtil = Mockito.mockStatic(FrameworkUtil.class)) {
      frameworkUtil.when(() -> FrameworkUtil.getBundle(CommunityNexusEdition.class)).thenReturn(mockBundle);
      when(mockBundle.getBundleContext()).thenReturn(mockContext);
      when(mockContext.getServiceReference(FeaturesService.class)).thenReturn(mockServiceReference);
      when(mockContext.getService(mockServiceReference)).thenReturn(mockFeaturesService);
      when(mockFeaturesService.getFeatures("nexus-community-edition"))
          .thenReturn(new Feature[]{Mockito.mock(Feature.class)});

      boolean result = underTest.applies(new Properties(), mockWorkDirPath);

      assertTrue(result);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testDoApply() {
    Properties properties = new Properties();
    properties.put(NEXUS_FEATURES, "test-feature" + NexusEditionFeature.PRO_FEATURE.featureString);

    underTest.apply(properties, mockWorkDirPath);

    assertEquals(NexusEditionType.COMMUNITY.editionString, properties.getProperty(NEXUS_EDITION));
    assertEquals("test-feature" + NexusEditionFeature.COMMUNITY_FEATURE.featureString,
        properties.getProperty(NEXUS_FEATURES));
  }
}
