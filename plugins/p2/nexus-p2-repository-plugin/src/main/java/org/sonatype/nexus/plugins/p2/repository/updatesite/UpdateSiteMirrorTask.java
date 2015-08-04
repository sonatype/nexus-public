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
package org.sonatype.nexus.plugins.p2.repository.updatesite;

import java.util.List;

import javax.inject.Named;

import org.sonatype.nexus.plugins.p2.repository.UpdateSiteProxyRepository;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;

import com.google.common.collect.Lists;

/**
 * Update Site mirror task.
 */
@Named(UpdateSiteMirrorTask.ROLE_HINT)
public class UpdateSiteMirrorTask
    extends AbstractNexusRepositoriesTask<Object>
{

  public static final String ROLE_HINT = "UpdateSiteMirrorTask";

  public static ScheduledTask<?> submit(final NexusScheduler scheduler, final UpdateSiteProxyRepository updateSite,
                                        final boolean force)
  {
    final UpdateSiteMirrorTask task = scheduler.createTaskInstance(UpdateSiteMirrorTask.class);
    task.setRepositoryId(updateSite.getId());
    task.setForce(force);
    return scheduler.submit("Eclipse Update Site Mirror (" + updateSite.getId() + ")", task);
  }

  @Override
  protected String getRepositoryFieldId() {
    return UpdateSiteMirrorTaskDescriptor.REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected Object doRun()
      throws Exception
  {
    List<UpdateSiteProxyRepository> repos = getRepositories();
    for (UpdateSiteProxyRepository updateSite : repos) {
      updateSite.mirror(getForce());
    }

    return null;
  }

  private List<UpdateSiteProxyRepository> getRepositories()
      throws NoSuchRepositoryException
  {
    if (getRepositoryId() != null) {
      Repository repo = getRepositoryRegistry().getRepository(getRepositoryId());
      if (repo.getRepositoryKind().isFacetAvailable(UpdateSiteProxyRepository.class)) {
        return Lists.newArrayList(repo.adaptToFacet(UpdateSiteProxyRepository.class));
      }
      else if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
        return updateSites(repo.adaptToFacet(GroupRepository.class));
      }
      else {
        throw new IllegalStateException(ROLE_HINT + " only applicable to Eclipse Update Sites");
      }
    }

    return getRepositoryRegistry().getRepositoriesWithFacet(UpdateSiteProxyRepository.class);
  }

  private List<UpdateSiteProxyRepository> updateSites(GroupRepository group) {
    List<UpdateSiteProxyRepository> us = Lists.newArrayList();

    for (Repository repo : group.getMemberRepositories()) {
      if (repo.getRepositoryKind().isFacetAvailable(UpdateSiteProxyRepository.class)) {
        us.add(repo.adaptToFacet(UpdateSiteProxyRepository.class));
      }
    }

    if (us.isEmpty()) {
      getLogger().warn(
          "Group '" + group.getId() + "' has no UpdateSite repositories members. " + ROLE_HINT
              + " will be silent skipped!");
    }

    return us;
  }

  @Override
  protected String getAction() {
    return ROLE_HINT;
  }

  @Override
  protected String getMessage() {
    if (getRepositoryId() == null) {
      return "Mirroring content of All Eclipse Update Sites.";
    }
    Repository repo;
    try {
      repo = getRepositoryRegistry().getRepository(getRepositoryId());
    }
    catch (NoSuchRepositoryException e) {
      return "Repository not found";
    }

    if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      return "Mirroring content of All Eclipse Update Sites in group ID='" + repo.getId() + "'.";
    }

    return "Mirroring content of Eclipse Update Site ID='" + repo.getId() + "'.";
  }

  @Override
  public void setRepositoryId(final String repositoryId) {
    try {
      getRepositoryRegistry().getRepository(repositoryId);
    }
    catch (final NoSuchRepositoryException e) {
      throw new IllegalStateException(e);
    }

    super.setRepositoryId(repositoryId);
  }

  public void setForce(final boolean force) {
    addParameter(UpdateSiteMirrorTaskDescriptor.FORCE_MIRROR_FIELD_ID, Boolean.toString(force));
  }

  public boolean getForce() {
    return Boolean.parseBoolean(getParameter(UpdateSiteMirrorTaskDescriptor.FORCE_MIRROR_FIELD_ID));
  }

}
