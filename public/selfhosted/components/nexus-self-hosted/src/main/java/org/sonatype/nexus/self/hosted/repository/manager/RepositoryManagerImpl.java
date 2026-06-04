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
package org.sonatype.nexus.self.hosted.repository.manager;

import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.manager.ConfigurationValidator;
import org.sonatype.nexus.repository.manager.DefaultRepositoriesContributor;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager;
import org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker;
import org.sonatype.nexus.repository.manager.internal.GroupMemberMappingCache;
import org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder;
import org.sonatype.nexus.repository.manager.internal.RepositoryAdminSecurityContributor;
import org.sonatype.nexus.repository.manager.internal.RepositoryFactory;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.REPOSITORIES;

/**
 * {@link BaseRepositoryManager} for self-hosted deployments.
 * No overrides needed, only annotations for DI.
 */
@Component
@ManagedLifecycle(phase = REPOSITORIES)
@ManagedObject(
    domain = "org.sonatype.nexus.repository.manager",
    typeClass = RepositoryManager.class,
    description = "Repository manager")
public class RepositoryManagerImpl
    extends BaseRepositoryManager<BlobStoreManager>
{
  @Autowired
  public RepositoryManagerImpl(
      final EventManager eventManager,
      final ConfigurationStore store,
      final RepositoryFactory factory,
      final Provider<ConfigurationFacet> configFacet,
      @Lazy final List<Recipe> recipes,
      final RepositoryAdminSecurityContributor securityContributor,
      final List<DefaultRepositoriesContributor> defaultRepositoriesContributors,
      @Value("${nexus.skipDefaultRepositories:false}") final boolean skipDefaultRepositories,
      final BlobStoreManager blobStoreManager,
      final GroupMemberMappingCache groupMemberMappingCache,
      final List<ConfigurationValidator> configurationValidators,
      final HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder,
      final FailedRepositoryTracker failedRepositoryTracker)
  {
    super(eventManager, store, factory, configFacet, recipes, securityContributor, defaultRepositoriesContributors,
        skipDefaultRepositories, blobStoreManager, groupMemberMappingCache, configurationValidators,
        httpAuthenticationSecretEncoder, failedRepositoryTracker);
  }
}
