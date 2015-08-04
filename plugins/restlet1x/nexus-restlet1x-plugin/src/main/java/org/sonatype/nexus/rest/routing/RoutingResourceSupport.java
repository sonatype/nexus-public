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
package org.sonatype.nexus.rest.routing;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;

import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Autorouting REST resource support.
 *
 * @author cstamas
 * @since 2.4
 */
public abstract class RoutingResourceSupport
    extends AbstractNexusPlexusResource
{
  protected static final String REPOSITORY_ID_KEY = "repositoryId";

  private Manager manager;

  /**
   * Constructor needed to set resource modifiable.
   */
  public RoutingResourceSupport() {
    setModifiable(true);
  }

  @Inject
  public void setManager(final Manager manager) {
    this.manager = manager;
  }

  protected Manager getManager() {
    return manager;
  }

  /**
   * Returns properly adapted {@link MavenRepository} instance, or handles cases like not exists or not having
   * required type (kind in Nx lingo).
   */
  protected <T extends MavenRepository> T getMavenRepository(final Request request, Class<T> clazz)
      throws ResourceException
  {
    final String repositoryId = request.getAttributes().get(REPOSITORY_ID_KEY).toString();
    try {
      final Repository repository = getRepositoryRegistry().getRepository(repositoryId);
      final T mavenRepository = repository.adaptToFacet(clazz);
      if (mavenRepository != null) {
        if (!getManager().isMavenRepositorySupported(mavenRepository)) {
          throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Repository with ID=\""
              + repositoryId + "\" unsupported!");
        }
        return mavenRepository;
      }
      else {
        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Repository with ID=\"" + repositoryId
            + "\" is not a required type of " + clazz.getSimpleName() + ".");
      }
    }
    catch (NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No repository with ID=\"" + repositoryId
          + "\" found.", e);
    }
  }
}
