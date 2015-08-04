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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.model.GlobalConfigurationListResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationListResourceResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * The GlobalConfigurationList resource. This is a read only resource that simply returns a list of known configuration
 * resources.
 *
 * @author cstamas
 * @author tstevens
 */
@Named
@Singleton
@Path(GlobalConfigurationListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class GlobalConfigurationListPlexusResource
    extends AbstractGlobalConfigurationPlexusResource
{
  public static final String RESOURCE_URI = "/global_settings";

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
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:settings]");
  }

  /**
   * Get the list of global configuration objects in nexus.
   */
  @Override
  @GET
  @ResourceMethodSignature(output = GlobalConfigurationListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    GlobalConfigurationListResourceResponse result = new GlobalConfigurationListResourceResponse();

    GlobalConfigurationListResource data = new GlobalConfigurationListResource();

    data.setName(GlobalConfigurationPlexusResource.DEFAULT_CONFIG_NAME);

    data.setResourceURI(createChildReference(request, this, data.getName()).toString());

    result.addData(data);

    data = new GlobalConfigurationListResource();

    data.setName(GlobalConfigurationPlexusResource.CURRENT_CONFIG_NAME);

    data.setResourceURI(createChildReference(request, this, data.getName()).toString());

    result.addData(data);

    return result;
  }

}
