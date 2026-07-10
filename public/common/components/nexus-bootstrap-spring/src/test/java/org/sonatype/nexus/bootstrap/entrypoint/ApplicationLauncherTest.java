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
package org.sonatype.nexus.bootstrap.entrypoint;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusPropertiesVerifier.COMMUNITY;

@ExtendWith(MockitoExtension.class)
class ApplicationLauncherTest
{
  private static final String PRO_EDITION_ID = "nexus-professional-edition";

  @Mock
  private NexusEditionSelector nexusEditionSelector;

  @Mock
  private ConfigurableApplicationContext context;

  @Mock
  private SpringComponentScan springComponentScan;

  @Mock
  private NexusProperties nexusProperties;

  @Mock
  private NexusEdition communityEdition;

  @Mock
  private NexusEdition proEdition;

  private ApplicationLauncher applicationLauncher;

  private ConfigurableConversionService conversionService;

  @BeforeEach
  void setUp() {
    // Mock the Spring environment
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
    MutablePropertySources propertySources = new MutablePropertySources();
    conversionService = mock(ConfigurableConversionService.class);
    when(context.getEnvironment()).thenReturn(environment);
    when(environment.getPropertySources()).thenReturn(propertySources);
    when(environment.getConversionService()).thenReturn(conversionService);

    applicationLauncher = new ApplicationLauncher(
        nexusEditionSelector,
        context,
        springComponentScan,
        nexusProperties);

    // Setup mock editions - use lenient() since not all tests use all editions
    lenient().when(communityEdition.getId()).thenReturn(COMMUNITY);
    lenient().when(communityEdition.getShortName()).thenReturn("COMMUNITY");
    lenient().when(proEdition.getId()).thenReturn(PRO_EDITION_ID);
    lenient().when(proEdition.getShortName()).thenReturn("PRO");
  }

  @Test
  void initialize_WithCommunityEdition_WhenAnalyticsDisabled_EnforcesAnalytics() {
    when(nexusEditionSelector.getCurrent()).thenReturn(communityEdition);
    when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn("false");

    applicationLauncher.initialize();

    verify(nexusProperties).enforceCommunityEditionAnalytics();
  }

  @Test
  void initialize_WithCommunityEdition_WhenAnalyticsEnabled_StillEnforcesAnalytics() {
    when(nexusEditionSelector.getCurrent()).thenReturn(communityEdition);
    when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn("true");

    applicationLauncher.initialize();

    verify(nexusProperties).enforceCommunityEditionAnalytics();
  }

  @Test
  void initialize_WithCommunityEdition_WhenAnalyticsUnset_EnforcesAnalytics() {
    when(nexusEditionSelector.getCurrent()).thenReturn(communityEdition);
    when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn(null);

    applicationLauncher.initialize();

    verify(nexusProperties).enforceCommunityEditionAnalytics();
  }

  @Test
  void initialize_WithProEdition_DoesNotEnforceAnalytics() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);
    // Use lenient() because Pro Edition doesn't check analytics property at all
    lenient().when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn("false");

    applicationLauncher.initialize();

    verify(nexusProperties, never()).enforceCommunityEditionAnalytics();
  }

  @Test
  void initialize_WithProEdition_WhenAnalyticsEnabled_DoesNotEnforceAnalytics() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);
    // Use lenient() because Pro Edition doesn't check analytics property at all
    lenient().when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn("true");

    applicationLauncher.initialize();

    verify(nexusProperties, never()).enforceCommunityEditionAnalytics();
  }

  @Test
  void initialize_RegistersNexusEditionToStringConverter() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);

    applicationLauncher.initialize();

    verify(conversionService).addConverter(eq(NexusEdition.class), eq(String.class), any());
  }
}
