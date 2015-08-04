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
package org.sonatype.nexus.plugins.repository.api;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

@Singleton
@Named
public class RepositoryForceDeletePlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String REPOSITORY_ID_KEY = "repositoryId";

  public static final String RESOURCE_URI = "/repository_force_delete/{" + REPOSITORY_ID_KEY + "}";

  public RepositoryForceDeletePlexusResource() {
    this.setModifiable(true);
    this.setReadable(false);
  }

  @Override
  public Object getPayloadInstance() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repository_force_delete/*", "anon");
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public void delete(Context context, Request request, Response response)
      throws ResourceException
  {
    String repoId = this.getRepositoryId(request);
    try {
      getNexusConfiguration().deleteRepository(repoId, true);

      response.setStatus(Status.SUCCESS_NO_CONTENT);
    }
    catch (Exception e) {
      getLogger().warn("Unable to delete repository, id=" + repoId);

      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to delete repository, id=" + repoId);
    }
  }

  protected String getRepositoryId(Request request) {
    return request.getAttributes().get(REPOSITORY_ID_KEY).toString();
  }
}
