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
package org.sonatype.nexus.obr.task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.obr.metadata.ObrMetadataSource;
import org.sonatype.nexus.obr.util.ObrUtils;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import static com.google.common.base.Preconditions.checkNotNull;

@Named("PublishObrDescriptorTask")
public class PublishObrDescriptorTask
    extends AbstractNexusRepositoriesTask<Object>
{

  /**
   * System event action: publish indexes
   */
  public static final String ACTION = "PUBLISHINDEX";

  public static final String REPO_OR_GROUP_FIELD_ID = "repositoryId";

  private final ObrMetadataSource obrMetadataSource;

  private final Walker walker;

  @Inject
  public PublishObrDescriptorTask(final @Named("obr-bindex") ObrMetadataSource obrMetadataSource,
                                  final Walker walker)
  {
    this.obrMetadataSource = checkNotNull(obrMetadataSource);
    this.walker = checkNotNull(walker);
  }

  @Override
  protected String getRepositoryFieldId() {
    return REPO_OR_GROUP_FIELD_ID;
  }

  @Override
  protected Object doRun()
      throws Exception
  {

    Repository repo;
    if (getRepositoryId() != null) {
      repo = getRepositoryRegistry().getRepository(getRepositoryId());
    }
    else {
      throw new IllegalArgumentException("Target repository must be set.");
    }

    buildObr(repo);

    return null;
  }

  private void buildObr(final GroupRepository repo)
      throws StorageException
  {
    final List<Repository> members = repo.getMemberRepositories();
    for (final Repository repository : members) {
      buildObr(repository);
    }
  }

  private void buildObr(final Repository repo)
      throws StorageException
  {
    if (repo.getRepositoryKind().isFacetAvailable(GroupRepository.class)) {
      buildObr(repo.adaptToFacet(GroupRepository.class));
    }
    else if (repo.getRepositoryKind().isFacetAvailable(ShadowRepository.class)) {
      buildObr(repo.adaptToFacet(ShadowRepository.class).getMasterRepository());
    }
    else {
      ObrUtils.buildObr(obrMetadataSource, ObrUtils.createObrUid(repo), repo, walker);
    }
  }

  @Override
  protected String getAction() {
    return ACTION;
  }

  @Override
  protected String getMessage() {
    return "Publishing obr.xml for repository " + getRepositoryName();
  }

}
