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

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * NX Task that deletes all the metadata from npm repository (hosted or proxy).
 *
 * @since 2.11.3
 */
@Named(NpmMetadataDeleteTaskDescriptor.ID)
@Singleton
public class NpmMetadataDeleteTask
    extends NpmTaskSupport<NpmRepository>
{
  @Inject
  public NpmMetadataDeleteTask(final NexusConfiguration nexusConfiguration) {
    super(nexusConfiguration);
  }

  @Override
  protected void doExecute(final List<NpmRepository> repositories) throws Exception {
    for (NpmRepository repository : repositories) {
      TaskUtil.checkInterruption();
      try {
        if (repository instanceof NpmHostedRepository) {
          ((NpmHostedRepository) repository).getMetadataService().deleteAllMetadata();
        }
        else if (repository instanceof NpmProxyRepository) {
          ((NpmProxyRepository) repository).getMetadataService().deleteAllMetadata();
        }
      }
      catch (Exception e) {
        logger.info("npm repository error during delete metadata of {}", repository, e);
      }
    }
  }

  @Override
  protected String getAction() {
    return "NPM-MD-DELETE";
  }

  @Override
  protected String getMessage() {
    final List<String> repositories = Lists
        .transform(getAffectedRepositories(), new Function<NpmRepository, String>()
        {
          @Override
          public String apply(final NpmRepository input) {
            return input.getId();
          }
        });
    return "Delete npm metadata of repositories: " + repositories;
  }

  @Override
  protected List<NpmRepository> getAffectedRepositories() {
    if (Strings.isNullOrEmpty(getRepositoryId())) {
      return getRepositoryRegistry().getRepositoriesWithFacet(NpmRepository.class);
    }
    else {
      try {
        return Lists.newArrayList(
            getRepositoryRegistry().getRepositoryWithFacet(getRepositoryId(), NpmRepository.class)
        );
      }
      catch (NoSuchRepositoryException e) {
        throw new IllegalArgumentException(
            "Delete npm metadata task misconfiguration: no npm repository with ID " + getRepositoryId(), e);
      }
    }
  }

  @Override
  protected String getRepositoryFieldId() {
    return NpmMetadataDeleteTaskDescriptor.FLD_REPOSITORY_ID;
  }
}
