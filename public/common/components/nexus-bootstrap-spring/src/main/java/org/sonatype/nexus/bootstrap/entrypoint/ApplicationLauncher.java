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

import java.util.Map;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEdition;
import org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionSelector;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ApplicationLauncher
{
  public static final String SYSTEM_USERID = "*SYSTEM";

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationLauncher.class);

  private final NexusEditionSelector nexusEditionSelector;

  private final ConfigurableApplicationContext context;

  private final SpringComponentScan springComponentScan;

  private final NexusProperties nexusProperties;

  @Autowired
  public ApplicationLauncher(
      final NexusEditionSelector nexusEditionSelector,
      final ConfigurableApplicationContext context,
      final SpringComponentScan springComponentScan,
      final NexusProperties nexusProperties)

  {
    this.nexusEditionSelector = checkNotNull(nexusEditionSelector);
    this.context = checkNotNull(context);
    this.springComponentScan = checkNotNull(springComponentScan);
    this.nexusProperties = checkNotNull(nexusProperties);
  }

  @PostConstruct
  void initialize() {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    NexusEdition nexusEdition = nexusEditionSelector.getCurrent();

    LOG.info("Starting nexus with edition {}", nexusEdition.getShortName());

    warnIfDeprecatedAnalyticsPropertySet();

    // Allow the edition value below to resolve as a String when requested that way.
    ConfigurableConversionService conversionService = context.getEnvironment().getConversionService();
    conversionService.addConverter(NexusEdition.class, String.class, NexusEdition::getShortName);

    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(new MapPropertySource(
            "application-launcher",
            Map.of("nexus.edition", nexusEdition)));
  }

  private void warnIfDeprecatedAnalyticsPropertySet() {
    String analyticsEnabled = nexusProperties.getProperty("nexus.analytics.enabled");
    if (analyticsEnabled != null) {
      LOG.warn("{} property is deprecated and has no effect. " +
          "Telemetry configuration is managed through license provisioning. " +
          "For opt-out provisioning, please contact Sonatype Sales.",
          "nexus.analytics.enabled");
    }
  }

  /**
   * This method is called when the application context is refreshed. It will trigger the 2nd level component scanning
   * and start the jetty server.
   */
  @EventListener
  public void onContextRefreshed(final ContextRefreshedEvent event) {
    // we only want this event from the parent context to start jetty
    if (event.getApplicationContext().getParent() != null) {
      LOG.debug("Application already started, skipping event");
      return;
    }

    try {
      springComponentScan.finishBootstrapComponentScanning();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to configure application", e);
    }
  }
}
