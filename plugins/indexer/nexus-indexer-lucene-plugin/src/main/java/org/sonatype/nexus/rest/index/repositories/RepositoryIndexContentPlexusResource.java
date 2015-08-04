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
package org.sonatype.nexus.rest.index.repositories;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.indextreeview.AbstractIndexContentPlexusResource;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Repository index content resource.
 *
 * @author dip
 */
@Named
@Singleton
@Path(RepositoryIndexContentPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class RepositoryIndexContentPlexusResource
    extends AbstractIndexContentPlexusResource
{
  public static final String REPOSITORY_ID_KEY = "repositoryId";

  public static final String RESOURCE_URI = "/repositories/{" + REPOSITORY_ID_KEY + "}/index_content";

  public RepositoryIndexContentPlexusResource() {
    setRequireStrictChecking(false);
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repositories/*/index_content/**", "authcBasic,tiperms");
  }

  @Override
  protected String getRepositoryId(Request request) {
    return String.valueOf(request.getAttributes().get(REPOSITORY_ID_KEY));
  }

  /**
   * Get the index content from the specified repository. at the specified path (path is appended to the end of the
   * uri).
   *
   * @param repositoryId The repository to retrieve the index content for.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(RepositoryIndexContentPlexusResource.REPOSITORY_ID_KEY)},
      output = IndexBrowserTreeViewResponseDTO.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    return super.get(context, request, response, variant);
  }
}
