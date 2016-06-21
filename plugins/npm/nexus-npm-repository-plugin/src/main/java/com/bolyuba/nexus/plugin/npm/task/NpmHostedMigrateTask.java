/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.scheduling.TaskUtil;

import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.service.internal.migrator.Migrator;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NX Task that migrates hosted NPM MetadataStore.
 *
 * @since 2.14.0
 */
@Named(NpmHostedMigrateTaskDescriptor.ID)
public class NpmHostedMigrateTask
    extends NpmTaskSupport<NpmHostedRepository>
{
  private final Migrator migrator;

  @Inject
  public NpmHostedMigrateTask(final NexusConfiguration nexusConfiguration, final Migrator migrator) {
    super(nexusConfiguration);
    this.migrator = checkNotNull(migrator);
  }

  @Override
  protected void doExecute(final List<NpmHostedRepository> repositories) throws Exception {
    for (NpmHostedRepository repository : repositories) {
      TaskUtil.checkInterruption();
      try {
        migrator.migrate(repository);
      }
      catch (Exception e) {
        logger.info("Hosted npm repository error during metadata migration of {}", repository, e);
      }
    }
  }

  @Override
  protected String getAction() {
    return "NPM-MD-MIGRATE";
  }

  @Override
  protected String getMessage() {
    final List<String> repositories = Lists
        .transform(getAffectedRepositories(), new Function<NpmHostedRepository, String>()
        {
          @Override
          public String apply(final NpmHostedRepository input) {
            return input.getId();
          }
        });
    return "Migrate hosted npm metadata of repositories: " + repositories;
  }

  @Override
  protected List<NpmHostedRepository> getAffectedRepositories() {
    if (Strings.isNullOrEmpty(getRepositoryId())) {
      return getRepositoryRegistry().getRepositoriesWithFacet(NpmHostedRepository.class);
    }
    else {
      try {
        return Lists.newArrayList(
            getRepositoryRegistry().getRepositoryWithFacet(getRepositoryId(), NpmHostedRepository.class)
        );
      }
      catch (NoSuchRepositoryException e) {
        throw new IllegalArgumentException(
            "Migrate hosted npm metadata task misconfiguration: no npm repository with ID " + getRepositoryId(), e);
      }
    }
  }

  @Override
  protected String getRepositoryFieldId() {
    return NpmHostedMigrateTaskDescriptor.FLD_REPOSITORY_ID;
  }
}
