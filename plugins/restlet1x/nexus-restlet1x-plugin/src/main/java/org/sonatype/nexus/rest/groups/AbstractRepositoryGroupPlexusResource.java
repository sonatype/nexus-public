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
package org.sonatype.nexus.rest.groups;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.InvalidGroupingException;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.NexusCompat;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.RepositoryURLBuilder;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.repositories.RepositoryBaseResourceConverter;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate.ProviderHint;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.plexus.rest.resource.PlexusResourceException;

import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

public abstract class AbstractRepositoryGroupPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String GROUP_ID_KEY = "groupId";

  private RepositoryTypeRegistry repositoryTypeRegistry;

  private RepositoryURLBuilder repositoryURLBuilder;

  @Inject
  public void setRepositoryTypeRegistry(final RepositoryTypeRegistry repositoryTypeRegistry) {
    this.repositoryTypeRegistry = repositoryTypeRegistry;
  }

  @Inject
  public void setRepositoryURLBuilder(final RepositoryURLBuilder repositoryURLBuilder) {
    this.repositoryURLBuilder = repositoryURLBuilder;
  }

  protected RepositoryURLBuilder getRepositoryURLBuilder() {
    return repositoryURLBuilder;
  }

  protected RepositoryGroupResource buildGroupResource(Request request, String groupId)
      throws NoSuchRepositoryException,
             ResourceException
  {
    Repository repo = getRepositoryRegistry().getRepository(groupId);

    if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      return buildGroupResource(request, repo.adaptToFacet(GroupRepository.class));
    }

    return null;
  }

  protected RepositoryGroupResource buildGroupResource(Request request, GroupRepository group)
      throws ResourceException
  {
    RepositoryGroupResource resource = new RepositoryGroupResource();

    resource.setContentResourceURI(getRepositoryURLBuilder().getExposedRepositoryContentUrl(group));

    resource.setId(group.getId());

    resource.setName(group.getName());

    resource.setProvider(NexusCompat.getRepositoryProviderHint(group));

    resource.setRepoType(RepositoryBaseResourceConverter.REPO_TYPE_GROUP);

    resource.setFormat(group.getRepositoryContentClass().getId());

    resource.setExposed(group.isExposed());

    // just to trigger list creation, and not stay null coz of XStream serialization
    resource.getRepositories();

    for (String repoId : group.getMemberRepositoryIds()) {
      RepositoryGroupMemberRepository member = new RepositoryGroupMemberRepository();

      member.setId(repoId);

      try {
        // NOTE: we must hit the registry each time and NOT call groupRepo.getMemberRepositories, that doesn't block access
        member.setName(getRepositoryRegistry().getRepository(repoId).getName());
      }
      catch (NoSuchRepositoryAccessException e) {
        // access denied 403
        getLogger().debug("Blocking access to repository group, based on permissions.");

        throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
      }
      catch (NoSuchRepositoryException e) {
        getLogger().debug("Found missing repo id: " + repoId + " contained in group");
      }

      member.setResourceURI(createChildReference(request, this, repoId).toString());

      resource.addRepository(member);
    }

    return resource;
  }

  protected void createOrUpdateRepositoryGroup(RepositoryGroupResource model, boolean create)
      throws ResourceException
  {
    try {
      if (create) {
        createRepositoryGroup(model);
      }
      else {
        updateRepositoryGroup(model);
      }
    }
    catch (NoSuchRepositoryException e) {
      //Both update and create catch and wrap a NoSuchRepositoryException when a member does not exist. Update however
      //will throw the exception without wrapping if the group itself does not exist. When the group does not exist
      //then there is nothing to rollback so we log, wrap and throw the exception here.
      throw logAndWrapNoSuchGroupRepositoryException(model, e);
    }
    catch (Exception e) {
      GroupRepository group = getGroupRepository(model);

      //Rollback any in memory changes for any failure while updating / creating the group.
      if (group instanceof AbstractGroupRepository && ((AbstractGroupRepository) group).rollbackChanges()) {
        getLogger().warn("Changes to group {} rolled back because an exception was encountered", group.getId());
      }
      throw e;
    }
  }

  private PlexusResourceException logAndWrapNoSuchGroupRepositoryException(final RepositoryGroupResource model,
                                                                           final NoSuchRepositoryException e)
  {
    getLogger().warn("Group repository does not exist, ID=" + model.getId(), e);

    return new PlexusResourceException(
        Status.CLIENT_ERROR_BAD_REQUEST,
        "Group repository does not exist, GroupId=" + model.getId(),
        e,
        getNexusErrorResponse("repositories", "Group repository does not exist"));
  }

  private GroupRepository getGroupRepository(final RepositoryGroupResource model) throws PlexusResourceException {
    try {
      return getRepositoryRegistry().getRepositoryWithFacet(model.getId(), GroupRepository.class);
    }
    catch (NoSuchRepositoryException e) {
      throw logAndWrapNoSuchGroupRepositoryException(model, e);
    }
  }

  protected void updateRepositoryGroup(RepositoryGroupResource model)
      throws ResourceException, NoSuchRepositoryException
  {
    GroupRepository group = getRepositoryRegistry().getRepositoryWithFacet(model.getId(), GroupRepository.class);
    try {
      group.setName(model.getName());

      group.setExposed(model.isExposed());

      ArrayList<String> members = new ArrayList<String>();

      for (RepositoryGroupMemberRepository member : (List<RepositoryGroupMemberRepository>) model
          .getRepositories()) {
        members.add(member.getId());
      }

      group.setMemberRepositoryIds(members);

      getNexusConfiguration().saveConfiguration();
    }
    catch (NoSuchRepositoryAccessException e) {
      // access denied 403
      getLogger().warn("Repository referenced by Repository Group Access Denied, ID=" + model.getId(), e);

      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST,
          "Repository referenced by Repository Group Access Denied, GroupId="
              + model.getId(),
          e,
          getNexusErrorResponse("repositories",
              "Repository referenced by Repository Group Access Denied"));
    }
    catch (NoSuchRepositoryException e) {
      getLogger().warn("Repository referenced by Repository Group Not Found, ID=" + model.getId(), e);

      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST,
          "Repository referenced by Repository Group Not Found, GroupId="
              + model.getId(),
          e,
          getNexusErrorResponse("repositories",
              "Repository referenced by Repository Group Not Found"));
    }
    catch (InvalidGroupingException e) {
      getLogger().warn("Invalid grouping detected!, GroupId=" + model.getId(), e);

      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST,
          "Invalid grouping requested, GroupId=" + model.getId(),
          e,
          getNexusErrorResponse("repositories",
              e.getMessage()));
    }
    catch (IOException e) {
      getLogger().warn("Got IO Exception!", e);

      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
    }
  }

  protected void createRepositoryGroup(RepositoryGroupResource model)
      throws ResourceException
  {
    try {
      verifyMembersExist(model);

      ContentClass contentClass =
          repositoryTypeRegistry.getRepositoryContentClass(GroupRepository.class, model.getProvider());

      RepositoryTemplate template = (RepositoryTemplate) getRepositoryTemplates().getTemplates(
          GroupRepository.class, contentClass, new ProviderHint(model.getProvider())
      ).pick();

      template.getConfigurableRepository().setId(model.getId());

      template.getConfigurableRepository().setName(model.getName());

      template.getConfigurableRepository().setExposed(model.isExposed());

      template.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);

      // we create an empty group
      GroupRepository groupRepository = (GroupRepository) template.create();

      ArrayList<String> memberIds = new ArrayList<String>(model.getRepositories().size());

      for (RepositoryGroupMemberRepository member : (List<RepositoryGroupMemberRepository>) model
          .getRepositories()) {
        memberIds.add(member.getId());
      }

      groupRepository.setMemberRepositoryIds(memberIds);

      getNexusConfiguration().saveConfiguration();
    }
    // FIXME: cstamas or toby?
        /*
         * catch ( NoSuchRepositoryAccessException e ) { // access denied 403 getLogger().warn(
         * "Repository referenced by Repository Group Access Denied, ID=" + model.getId(), e ); throw new
         * PlexusResourceException( Status.CLIENT_ERROR_BAD_REQUEST,
         * "Repository referenced by Repository Group Access Denied, GroupId=" + model.getId(), e,
         * getNexusErrorResponse( "repositories", "Repository referenced by Repository Group Access Denied" ) ); }
         */
    catch (NoSuchRepositoryException e) {
      getLogger().warn("Repository referenced by group does not exists!, GroupId=" + model.getId(), e);

      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST,
          "Repository referenced by group does not exists, GroupId="
              + model.getId(),
          e,
          getNexusErrorResponse("repositories",
              "Repository referenced by Repository Group does not exists!"));
    }
    catch (InvalidGroupingException e) {
      getLogger().warn("Invalid grouping detected!, GroupId=" + model.getId(), e);

      throw new PlexusResourceException(
          Status.CLIENT_ERROR_BAD_REQUEST,
          "Invalid grouping requested, GroupId=" + model.getId(),
          e,
          getNexusErrorResponse("repositories", e.getMessage()));
    }
    catch (IOException e) {
      getLogger().warn("Got IO Exception!", e);

      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
    }
    catch (ConfigurationException e) {
      handleConfigurationException(e);
    }
  }

  private void verifyMembersExist(final RepositoryGroupResource model) throws NoSuchRepositoryException {
    for (RepositoryGroupMemberRepository member : model.getRepositories()) {
      // this will throw NoSuchRepository if needed
      getRepositoryRegistry().getRepository(member.getId());
    }
  }
}
