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
package org.sonatype.nexus.rest.restore;

import java.util.concurrent.RejectedExecutionException;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.nexus.scheduling.NexusTask;

import org.apache.commons.lang.StringUtils;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public abstract class AbstractRestorePlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String DOMAIN = "domain";

  public static final String DOMAIN_REPOSITORIES = "repositories";

  public static final String DOMAIN_REPO_GROUPS = "repo_groups";

  public static final String TARGET_ID = "target";

  private NexusScheduler nexusScheduler;

  public AbstractRestorePlexusResource() {
    this.setModifiable(true);
  }

  @Inject
  public void setNexusScheduler(final NexusScheduler nexusScheduler) {
    this.nexusScheduler = nexusScheduler;
  }

  protected NexusScheduler getNexusScheduler() {
    return nexusScheduler;
  }

  protected String getRepositoryId(Request request)
      throws ResourceException
  {
    String repoId = null;

    if ((request.getAttributes().containsKey(DOMAIN) && request.getAttributes().containsKey(TARGET_ID))
        && DOMAIN_REPOSITORIES.equals(request.getAttributes().get(DOMAIN))) {
      repoId = request.getAttributes().get(TARGET_ID).toString();

      try {
        // simply to throw NoSuchRepository exception
        getRepositoryRegistry().getRepositoryWithFacet(repoId, Repository.class);
      }
      catch (NoSuchRepositoryException e) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Repository not found!", e);
      }
    }

    return repoId;
  }

  protected String getRepositoryGroupId(Request request)
      throws ResourceException
  {
    String groupId = null;

    if ((request.getAttributes().containsKey(DOMAIN) && request.getAttributes().containsKey(TARGET_ID))
        && DOMAIN_REPO_GROUPS.equals(request.getAttributes().get(DOMAIN))) {
      groupId = request.getAttributes().get(TARGET_ID).toString();

      try {
        // simply to throw NoSuchRepository exception
        getRepositoryRegistry().getRepositoryWithFacet(groupId, GroupRepository.class);
      }
      catch (NoSuchRepositoryException e) {
        throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Repository group not found!", e);
      }
    }

    return groupId;
  }

  protected String getResourceStorePath(Request request)
      throws ResourceException
  {
    String path = null;

    if (getRepositoryId(request) != null || getRepositoryGroupId(request) != null) {
      path = request.getResourceRef().getRemainingPart();

      // get rid of query part
      if (path.contains("?")) {
        path = path.substring(0, path.indexOf('?'));
      }

      // get rid of reference part
      if (path.contains("#")) {
        path = path.substring(0, path.indexOf('#'));
      }

      if (StringUtils.isEmpty(path)) {
        path = "/";
      }
    }
    return path;
  }

  public void handleDelete(NexusTask<?> task, Request request)
      throws ResourceException
  {
    try {
      // check reposes
      if (getRepositoryGroupId(request) != null) {
        getRepositoryRegistry().getRepositoryWithFacet(getRepositoryGroupId(request), GroupRepository.class);
      }
      else if (getRepositoryId(request) != null) {
        try {
          getRepositoryRegistry().getRepository(getRepositoryId(request));
        }
        catch (NoSuchRepositoryException e) {
          getRepositoryRegistry().getRepositoryWithFacet(getRepositoryId(request), ShadowRepository.class);
        }
      }

      getNexusScheduler().submit("Internal", task);

      throw new ResourceException(Status.SUCCESS_NO_CONTENT);
    }
    catch (RejectedExecutionException e) {
      throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, e.getMessage());
    }
    catch (NoSuchRepositoryException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
    }
  }

}
