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
package org.sonatype.nexus.plexusplugin.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.plexusplugin.PlexusPlugin;
import org.sonatype.nexus.plexusplugin.rest.dto.PlexusPluginResponse;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.thoughtworks.xstream.XStream;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Example resource defined as Plexus component.
 */
@Path(PlexusPluginPlexusResource.PLEXUSPLUGIN_PATHPREFIX)
@Produces("text/xml")
@Component(role = PlexusResource.class, hint = "PlexusPluginPlexusResource")
public class PlexusPluginPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String PLEXUSPLUGIN_PATHPREFIX = "/plexusplugin";

  @Requirement
  private PlexusPlugin plexusPlugin;

  @Override
  public Object getPayloadInstance() {
    // RO resource, no payload
    return null;
  }

  @Override
  public String getResourceUri() {
    return PLEXUSPLUGIN_PATHPREFIX;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(PLEXUSPLUGIN_PATHPREFIX, "authcBasic,perms[nexus:status]");
  }

  @Override
  public void configureXStream(final XStream xstream) {
    xstream.processAnnotations(PlexusPluginResponse.class);
  }

  /**
   * Returns the response of PlexusPlugin resource.
   *
   * @return The Plexus plugin response.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = PlexusPluginResponse.class)
  public PlexusPluginResponse get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    final PlexusPluginResponse plexusPluginResponse = new PlexusPluginResponse(plexusPlugin.getEventReceived());

    plexusPluginResponse.getRepositoryIds().addAll(plexusPlugin.getRegisteredRepositoryIds());
    plexusPluginResponse.getRepositoryTypes().addAll(plexusPlugin.getRegisteredRepositoryTypes());
    plexusPluginResponse.getContentClasses().addAll(plexusPlugin.getContentClasses());
    plexusPluginResponse.getScheduledTaskNames().addAll(plexusPlugin.getScheduledTaskNames());

    return plexusPluginResponse;
  }
}
