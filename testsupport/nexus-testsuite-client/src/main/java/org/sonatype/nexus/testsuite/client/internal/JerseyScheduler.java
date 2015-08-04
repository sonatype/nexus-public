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

import java.util.Map;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.client.exception.TasksAreStillRunningException;
import org.sonatype.sisu.goodies.common.Time;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jersey based {@link Scheduler} Nexus Client Subsystem implementation.
 *
 * @since 2.3
 */
public class JerseyScheduler
    extends SubsystemSupport<JerseyNexusClient>
    implements Scheduler
{

  private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

  public JerseyScheduler(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  public void run(final String type, final Map<String, String> properties) {
    try {
      final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
      if (properties != null && !properties.isEmpty()) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          params.add(entry.getKey(), entry.getValue());
        }
      }

      getNexusClient()
          .serviceResource("tasks/run/" + type, params)
          .post();
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public void waitForAllTasksToStop() {
    waitForAllTasksToStop(null);
  }


  @Override
  public void waitForAllTasksToStop(@Nullable final Time timeout) {
    waitForAllTasksToStop(timeout, null);
  }

  @Override
  public void waitForAllTasksToStop(final Time timeout, final Time window) {
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
          "Waiting for Nexus to not execute any task for {} (timeouts in {})",
          actualWindow.toString(), actualTimeout.toString()
      );

      final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
      params.add("timeout", String.valueOf(actualTimeout.toMillis()));
      params.add("window", String.valueOf(actualWindow.toMillis()));

      final ClientResponse response = getNexusClient()
          .serviceResource("tasks/waitFor", params)
          .get(ClientResponse.class);

      response.close();

      if (Response.Status.ACCEPTED.getStatusCode() == response.getStatus()) {
        throw new TasksAreStillRunningException(actualTimeout);
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
