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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationLauncherTest
{
  @Mock
  private NexusEditionSelector nexusEditionSelector;

  @Mock
  private ConfigurableApplicationContext context;

  @Mock
  private SpringComponentScan springComponentScan;

  @Mock
  private NexusProperties nexusProperties;

  @Mock
  private NexusEdition proEdition;

  private ApplicationLauncher applicationLauncher;

  private ConfigurableConversionService conversionService;

  private Logger launcherLogger;

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
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

    lenient().when(proEdition.getShortName()).thenReturn("PRO");

    launcherLogger = (Logger) LoggerFactory.getLogger(ApplicationLauncher.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    launcherLogger.addAppender(logAppender);
  }

  @AfterEach
  void tearDown() {
    launcherLogger.detachAppender(logAppender);
  }

  @Test
  void initialize_WhenAnalyticsSet_LogsDeprecation() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);
    when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn("false");

    applicationLauncher.initialize();

    assertThat(logAppender.list, hasItem(warnLogWithMessage("nexus.analytics.enabled")));
    assertThat(logAppender.list, hasItem(warnLogWithMessage("deprecated and has no effect")));
  }

  @Test
  void initialize_WhenAnalyticsUnset_NoDeprecationWarning() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);
    when(nexusProperties.getProperty("nexus.analytics.enabled")).thenReturn(null);

    applicationLauncher.initialize();

    assertThat(warnEvents(), is(empty()));
  }

  @Test
  void initialize_RegistersNexusEditionToStringConverter() {
    when(nexusEditionSelector.getCurrent()).thenReturn(proEdition);

    applicationLauncher.initialize();

    verify(conversionService).addConverter(eq(NexusEdition.class), eq(String.class), any());
  }

  private static org.hamcrest.Matcher<ILoggingEvent> warnLogWithMessage(final String messageFragment) {
    return org.hamcrest.Matchers.allOf(
        hasProperty("level", is(Level.WARN)),
        hasProperty("formattedMessage", containsString(messageFragment)));
  }

  private java.util.List<ILoggingEvent> warnEvents() {
    return logAppender.list.stream()
        .filter(e -> e.getLevel() == Level.WARN)
        .toList();
  }
}
