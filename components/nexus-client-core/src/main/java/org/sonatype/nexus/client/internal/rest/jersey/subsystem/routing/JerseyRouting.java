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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.routing;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;
import org.sonatype.nexus.client.core.subsystem.routing.Routing;
import org.sonatype.nexus.client.core.subsystem.routing.Status;
import org.sonatype.nexus.client.core.subsystem.routing.Status.DiscoveryStatus;
import org.sonatype.nexus.client.core.subsystem.routing.Status.Outcome;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RoutingConfigMessage;
import org.sonatype.nexus.rest.model.RoutingConfigMessageWrapper;
import org.sonatype.nexus.rest.model.RoutingStatusMessage;
import org.sonatype.nexus.rest.model.RoutingStatusMessageWrapper;

import com.google.common.base.Throwables;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

/**
 * Jersey based {@link Routing} implementation.
 *
 * @author cstamas
 * @since 2.4
 */
public class JerseyRouting
    extends SubsystemSupport<JerseyNexusClient>
    implements Routing
{

  /**
   * Constructor.
   */
  public JerseyRouting(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public Status getStatus(final String mavenRepositoryId) {
    try {
      final RoutingStatusMessage message =
          getNexusClient().serviceResource(routingPath(mavenRepositoryId)).get(RoutingStatusMessageWrapper.class)
              .getData();

      final DiscoveryStatus discoveryStatus;
      if (message.getDiscovery() == null) {
        // not a proxy
        discoveryStatus = null;
      }
      else {
        final Outcome discoveryOutcome = Outcome.values()[message.getDiscovery().getDiscoveryLastStatus() + 1];
        discoveryStatus =
            new DiscoveryStatus(message.getDiscovery().isDiscoveryEnabled(),
                message.getDiscovery().getDiscoveryIntervalHours(), discoveryOutcome,
                message.getDiscovery().getDiscoveryLastStrategy(),
                message.getDiscovery().getDiscoveryLastMessage(),
                message.getDiscovery().getDiscoveryLastRunTimestamp());
      }

      final Outcome publishOutcome = Outcome.values()[message.getPublishedStatus() + 1];
      return new Status(publishOutcome, message.getPublishedMessage(), message.getPublishedTimestamp(),
          message.getPublishedUrl(), discoveryStatus);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public void updatePrefixFile(final String mavenProxyRepositoryId)
      throws IllegalArgumentException, NexusClientNotFoundException
  {
    try {
      getNexusClient().serviceResource(routingPath(mavenProxyRepositoryId)).delete();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public DiscoveryConfiguration getDiscoveryConfigurationFor(final String mavenProxyRepositoryId)
      throws IllegalArgumentException, NexusClientNotFoundException
  {
    try {
      final RoutingConfigMessage message =
          getNexusClient().serviceResource(routingConfigPath(mavenProxyRepositoryId)).get(
              RoutingConfigMessageWrapper.class).getData();
      return new DiscoveryConfiguration(message.isDiscoveryEnabled(), message.getDiscoveryIntervalHours());
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public void setDiscoveryConfigurationFor(final String mavenProxyRepositoryId,
                                           final DiscoveryConfiguration configuration)
      throws IllegalArgumentException, NexusClientNotFoundException
  {
    try {
      final RoutingConfigMessage message = new RoutingConfigMessage();
      message.setDiscoveryEnabled(configuration.isEnabled());
      message.setDiscoveryIntervalHours(configuration.getIntervalHours());
      final RoutingConfigMessageWrapper wrapper = new RoutingConfigMessageWrapper();
      wrapper.setData(message);
      getNexusClient().serviceResource(routingConfigPath(mavenProxyRepositoryId)).put(wrapper);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  // ==

  static String routingPath(final String mavenRepositoryId) {
    try {
      return "repositories/" + URLEncoder.encode(mavenRepositoryId, "UTF-8") + "/routing";
    }
    catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }

  static String routingConfigPath(final String mavenRepositoryId) {
    return routingPath(mavenRepositoryId) + "/config";
  }
}
