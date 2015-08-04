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
package org.sonatype.nexus.rest.repositories;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.rest.AbstractResourceStoreContentPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.data.Request;
import org.restlet.resource.ResourceException;

/**
 * Resource handler for Repository content resource.
 *
 * @author cstamas
 */
@Named
@Singleton
public class RepositoryContentPlexusResource
    extends AbstractResourceStoreContentPlexusResource
{
  private static final String USE_WELCOME_FILES = "useWelcomeFiles";

  public RepositoryContentPlexusResource() {
    this.setModifiable(true);
    this.setRequireStrictChecking(false);
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public String getResourceUri() {
    return "/repositories/{" + AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY + "}/content";
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repositories/*/content/**", "authcBasic,trperms");
  }

  public boolean acceptsUpload() {
    return true;
  }

  @Override
  protected ResourceStore getResourceStore(final Request request)
      throws NoSuchRepositoryException,
             ResourceException
  {
    return getUnprotectedRepositoryRegistry().getRepository(
        request.getAttributes().get(AbstractRepositoryPlexusResource.REPOSITORY_ID_KEY).toString());
  }

  @Override
  protected ResourceStoreRequest getResourceStoreRequest(Request request, String resourceStorePath) {
    ResourceStoreRequest resourceStoreRequest = super.getResourceStoreRequest(request, resourceStorePath);

    // welcome files should not be used with this resource.
    resourceStoreRequest.getRequestContext().put(USE_WELCOME_FILES, Boolean.FALSE);

    return resourceStoreRequest;
  }

}
