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
package org.sonatype.nexus.rest.schedules;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.component.AbstractComponentListPlexusResource;
import org.sonatype.nexus.rest.model.PlexusComponentListResourceResponse;
import org.sonatype.nexus.tasks.descriptors.ScheduledTaskDescriptor;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named
@Singleton
@Path(ScheduledTaskTypeComponentListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ScheduledTaskTypeComponentListPlexusResource
    extends AbstractComponentListPlexusResource
{
  public static final String RESOURCE_URI = "/components/schedule_types";

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:componentscheduletypes]");
  }

  @Override
  protected String getRole(Request request) {
    return ScheduledTaskDescriptor.class.getName();
  }

  /**
   * Retrieve the list of scheduled task type plexus components.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusComponentListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return super.get(context, request, response, variant);
  }
}
