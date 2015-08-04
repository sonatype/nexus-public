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
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.scheduling.TaskUtil;

import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * NX Task that rebuilds NPM MetadataStore for hosted repositories.
 *
 * @since 2.11
 */
@Named(NpmHostedMetadataRebuildTaskDescriptor.ID)
@Singleton
public class NpmHostedMetadataRebuildTask
    extends NpmTaskSupport<NpmHostedRepository>
{
  @Inject
  public NpmHostedMetadataRebuildTask(final NexusConfiguration nexusConfiguration) {
    super(nexusConfiguration);
  }

  @Override
  protected void doExecute(final List<NpmHostedRepository> repositories) throws Exception {
    for (NpmHostedRepository repository : repositories) {
      TaskUtil.checkInterruption();
      try {
        repository.recreateNpmMetadata();
      }
      catch (Exception e) {
        logger.info("Hosted npm repository error during rebuilding metadata of {}", repository, e);
      }
    }
  }

  @Override
  protected String getAction() {
    return "NPM-MD-REBUILD";
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
    return "Rebuild hosted npm metadata of repositories: " + repositories;
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
            "Rebuild hosted npm metadata task misconfiguration: no npm repository with ID " + getRepositoryId(), e);
      }
    }
  }

  @Override
  protected String getRepositoryFieldId() {
    return NpmHostedMetadataRebuildTaskDescriptor.FLD_REPOSITORY_ID;
  }
}
