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

import java.io.IOException;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.NexusStreamResponse;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.global.GlobalConfigurationPlexusResource;
import org.sonatype.plexus.rest.representation.InputStreamRepresentation;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * A resource that is able to retrieve configurations as stream.
 *
 * @author cstamas
 */
@Named
@Singleton
@Path("/configs/{configName}")
@Produces("text/xml")
public class ConfigurationPlexusResource
    extends AbstractNexusPlexusResource
{
  /**
   * The config key used in URI and request attributes
   */
  public static final String CONFIG_NAME_KEY = "configName";

  @Override
  public Object getPayloadInstance() {
    // this is RO resource, and have no payload, it streams the file to client
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/configs/{" + CONFIG_NAME_KEY + "}";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/configs/*", "authcBasic,perms[nexus:configuration]");
  }

  @Override
  public List<Variant> getVariants() {
    List<Variant> result = super.getVariants();

    result.clear();

    result.add(new Variant(MediaType.TEXT_PLAIN));

    result.add(new Variant(MediaType.APPLICATION_XML));

    return result;
  }

  /**
   * Returns the requested Nexus configuration. The keys for various configurations should be discovered by querying
   *
   * the "/configs" resource first. This resource emits the raw configuration file used by Nexus as response body.
   *
   * @param configKey The configuration key for which we want to get the configuration.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam("configKey")}, output = String.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    String key = request.getAttributes().get(GlobalConfigurationPlexusResource.CONFIG_NAME_KEY).toString();

    try {
      NexusStreamResponse result;

      if (!getNexusConfiguration().getConfigurationFiles().containsKey(key)) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No configuration with key '" + key
            + "' found!");
      }
      else {
        result = getNexusConfiguration().getConfigurationAsStreamByKey(key);
      }

      // TODO: make this real resource being able to be polled (ETag and last modified support)
      return new InputStreamRepresentation(MediaType.valueOf(result.getMimeType()), result.getInputStream());
    }
    catch (IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "IOException during configuration retrieval!", e);
    }
  }

}
