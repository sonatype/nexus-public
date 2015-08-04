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
package org.sonatype.nexus.testsuite.client.internal;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.client.Events;
import org.sonatype.nexus.testsuite.client.exception.EventsAreStillBeingHandledException;
import org.sonatype.sisu.goodies.common.Time;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jersey based {@link Events} Nexus Client Subsystem implementation.
 *
 * @since 2.3
 */
public class JerseyEvents
    extends SubsystemSupport<JerseyNexusClient>
    implements Events
{

  private static final Logger LOG = LoggerFactory.getLogger(Events.class);

  public JerseyEvents(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public void waitForCalmPeriod() {
    waitForCalmPeriod(null);
  }

  @Override
  public void waitForCalmPeriod(@Nullable final Time timeout) {
    waitForCalmPeriod(null, null);
  }

  @Override
  public void waitForCalmPeriod(@Nullable final Time timeout, @Nullable final Time window) {
    try {
      Time actualTimeout = timeout;
      if (actualTimeout == null) {
        actualTimeout = Time.minutes(1);
      }

      Time actualWindow = window;
      if (actualWindow == null) {
        actualWindow = Time.seconds(10);
      }

      LOG.info(
          "Waiting for Nexus to not handle events for {} (timeouts in {})",
          actualWindow.toString(), actualTimeout.toString()
      );

      final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
      params.add("timeout", String.valueOf(actualTimeout.toMillis()));
      params.add("window", String.valueOf(actualWindow.toMillis()));

      final ClientResponse response = getNexusClient()
          .serviceResource("events/waitFor", params)
          .get(ClientResponse.class);

      response.close();

      if (Response.Status.ACCEPTED.getStatusCode() == response.getStatus()) {
        throw new EventsAreStillBeingHandledException(actualTimeout);
      }
      else if (!response.getClientResponseStatus().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
        throw getNexusClient().convert(new UniformInterfaceException(response));
      }
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
  }

}
