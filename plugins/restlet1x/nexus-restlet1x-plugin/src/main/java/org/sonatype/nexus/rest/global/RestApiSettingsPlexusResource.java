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
package org.sonatype.nexus.rest.global;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.sonatype.nexus.rest.model.RestApiResourceResponse;
import org.sonatype.nexus.rest.model.RestApiSettings;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The Smtp settings validation resource.
 *
 * @author velo
 */
@Named
@Singleton
@Path(RestApiSettingsPlexusResource.RESOURCE_URI)
@Consumes({"application/xml", "application/json"})
public class RestApiSettingsPlexusResource
    extends AbstractGlobalConfigurationPlexusResource
{
  public static final String RESOURCE_URI = "/rest_api_settings";

  public RestApiSettingsPlexusResource() {
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    // everybody needs to know the UI timeout
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  /**
   * Validate smtp settings, send a test email using the configuration.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = RestApiSettings.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    RestApiResourceResponse resp = new RestApiResourceResponse();
    resp.setData(convert(getGlobalRestApiSettings()));
    return resp;
  }

}
