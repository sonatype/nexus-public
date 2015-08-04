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
package org.sonatype.nexus.rest.attributes;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

@Named
@Singleton
@Path(RepositoryOrGroupAttributesPlexusResource.RESOURCE_URI)
public class RepositoryOrGroupAttributesPlexusResource
    extends AbstractAttributesPlexusResource
{
  public static final String RESOURCE_URI = "/attributes/{" + AbstractAttributesPlexusResource.DOMAIN + "}/{"
      + AbstractAttributesPlexusResource.TARGET_ID + "}/content";

  public RepositoryOrGroupAttributesPlexusResource() {
    setRequireStrictChecking(false);
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/attributes/*/**", "authcBasic,perms[nexus:cache]");
  }

  /**
   * Rebuild all attributes in the specified domain (repository or group).
   *
   * @param domain The domain that will be used, valid options are 'repositories' or 'repo_groups' (Required).
   * @param target The unique id in the domain to use (i.e. repository or group id) (Required).
   */
  @Override
  @DELETE
  @ResourceMethodSignature(pathParams = {
      @PathParam(AbstractAttributesPlexusResource.DOMAIN), @PathParam(AbstractAttributesPlexusResource.TARGET_ID)
  })
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    super.delete(context, request, response);
  }

}
