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
package org.sonatype.nexus.rest.configurations;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.ConfigurationsListResource;
import org.sonatype.nexus.rest.model.ConfigurationsListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * A resource that is able to retrieve list of configurations.
 *
 * @author cstamas
 */
@Named
@Singleton
@Path(ConfigurationListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class ConfigurationListPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String RESOURCE_URI = "/configs";

  @Override
  public Object getPayloadInstance() {
    // RO resource, no payload
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:configuration]");
  }

  /**
   * Get the list of configuration files in Nexus.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = ConfigurationsListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    ConfigurationsListResourceResponse result = new ConfigurationsListResourceResponse();

    Map<String, String> configFileNames = getNexusConfiguration().getConfigurationFiles();

    for (Map.Entry<String, String> entry : configFileNames.entrySet()) {
      ConfigurationsListResource resource = new ConfigurationsListResource();

      resource.setResourceURI(createChildReference(request, this, entry.getKey()).toString());

      resource.setName(entry.getValue());

      result.addData(resource);
    }

    return result;
  }
}
