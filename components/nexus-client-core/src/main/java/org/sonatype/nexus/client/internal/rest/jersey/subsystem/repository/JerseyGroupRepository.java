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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.client.core.subsystem.repository.GroupRepository;
import org.sonatype.nexus.client.core.subsystem.repository.RepositoryStatus;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryGroupResourceResponse;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jersey based {@link GroupRepository} implementation.
 *
 * @since 2.3
 */
public class JerseyGroupRepository<T extends GroupRepository>
    extends JerseyRepository<T, RepositoryGroupResource, RepositoryStatus>
    implements GroupRepository<T>
{

  static final String REPO_TYPE = "group";

  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.GroupRepository";

  public JerseyGroupRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyGroupRepository(final JerseyNexusClient nexusClient, final RepositoryGroupResource resource) {
    super(nexusClient, resource);
  }

  @Override
  protected RepositoryGroupResource createSettings() {
    final RepositoryGroupResource settings = new RepositoryGroupResource();

    settings.setRepoType(REPO_TYPE);
    settings.setProviderRole(PROVIDER_ROLE);
    settings.setExposed(true);

    return settings;
  }

  private T me() {
    return (T) this;
  }

  @Override
  String uri() {
    return "repo_groups";
  }

  @Override
  RepositoryGroupResource doGet() {
    try {
      return getNexusClient()
          .serviceResource(uri() + "/" + id())
          .get(RepositoryGroupResourceResponse.class)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  RepositoryGroupResource doCreate() {
    final RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
    request.setData(settings());

    try {
      return getNexusClient()
          .serviceResource(uri())
          .post(RepositoryGroupResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  RepositoryGroupResource doUpdate() {
    final RepositoryGroupResourceResponse request = new RepositoryGroupResourceResponse();
    request.setData(settings());

    try {
      return getNexusClient()
          .serviceResource(uri() + "/" + id())
          .put(RepositoryGroupResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @Override
  public List<String> memberRepositories() {
    final List<RepositoryGroupMemberRepository> memberRepositories = settings().getRepositories();
    if (memberRepositories == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(
        Lists.transform(memberRepositories, new Function<RepositoryGroupMemberRepository, String>()
        {
          @Override
          public String apply(@Nullable final RepositoryGroupMemberRepository member) {
            return member == null ? null : member.getId();
          }
        }));
  }

  @Override
  public T ofRepositories(final String... memberRepositoryIds) {
    checkNotNull(memberRepositoryIds);
    ensureMemberListsIsValid();
    settings().getRepositories().clear();
    return addMember(memberRepositoryIds);
  }

  @Override
  public T addMember(final String... memberRepositoryIds) {
    ensureMemberListsIsValid();
    for (final String memberRepositoryId : checkNotNull(memberRepositoryIds)) {
      final RepositoryGroupMemberRepository repository = new RepositoryGroupMemberRepository();
      repository.setId(memberRepositoryId);
      settings().addRepository(repository);
    }
    return me();
  }

  @Override
  public T removeMember(final String... memberRepositoryIds) {
    final List<String> toRemove = Arrays.asList(checkNotNull(memberRepositoryIds));
    ensureMemberListsIsValid();
    final Iterator<RepositoryGroupMemberRepository> it = settings().getRepositories().iterator();
    while (it.hasNext()) {
      if (toRemove.contains(it.next().getId())) {
        it.remove();
      }
    }
    return me();
  }

  private void ensureMemberListsIsValid() {
    if (settings().getRepositories() == null) {
      settings().setRepositories(Lists.<RepositoryGroupMemberRepository>newArrayList());
    }
  }

}
