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
package org.sonatype.nexus.plugins.events.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriberHost;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Singleton
@Named
public class EventInspectorsPlexusResource
    extends AbstractPlexusResource
{
  private static final String RESOURCE_URI = "/eventInspectors/isCalmPeriod";

  private final EventSubscriberHost eventSubscriberHost;

  @Inject
  public EventInspectorsPlexusResource(final EventSubscriberHost eventSubscriberHost) {
    this.eventSubscriberHost = eventSubscriberHost;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();
    boolean waitForCalm = Boolean.parseBoolean(form.getFirstValue("waitForCalm"));

    if (waitForCalm) {
      for (int i = 0; i < 100; i++) {
        try {
          Thread.sleep(500);
        }
        catch (InterruptedException e) {
        }

        if (eventSubscriberHost.isCalmPeriod()) {
          response.setStatus(Status.SUCCESS_OK);
          return "Ok";
        }
      }

      response.setStatus(Status.SUCCESS_ACCEPTED);
      return "Still munching on them...";
    }
    else {
      if (eventSubscriberHost.isCalmPeriod()) {
        response.setStatus(Status.SUCCESS_OK);
        return "Ok";
      }
      else {
        response.setStatus(Status.SUCCESS_ACCEPTED);
        return "Still munching on them...";
      }
    }
  }

}
