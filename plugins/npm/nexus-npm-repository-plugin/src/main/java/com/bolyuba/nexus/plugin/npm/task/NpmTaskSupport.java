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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NX Task for npm support, allowing to put repositories into read only mode. Needed, as some task perform OrientDB
 * operations that makes DB read only, that would result in errors via content endpoints (but would not corrupt
 * database). This way, user accessing content for publish will receive meaningful error during task run.
 *
 * @since 2.11
 */
public abstract class NpmTaskSupport<T extends NpmRepository>
    extends AbstractNexusRepositoriesTask<Void>
{
  private final NexusConfiguration nexusConfiguration;

  @Inject
  public NpmTaskSupport(final NexusConfiguration nexusConfiguration) {
    this.nexusConfiguration = checkNotNull(nexusConfiguration);
  }

  @Override
  protected final Void doRun() throws Exception {
    final List<T> repositories = getAffectedRepositories();
    if (repositories.isEmpty()) {
      return null; // avoid at least 3 save configs
    }
    final List<RepositoryWritePolicy> writePolicies = setWritePolicyReadOnly(repositories);
    try {
      doExecute(repositories);
    }
    finally {
      resetWritePolicy(repositories, writePolicies);
    }
    return null;
  }

  protected abstract List<T> getAffectedRepositories();

  protected abstract void doExecute(final List<T> repositories) throws Exception;

  protected List<RepositoryWritePolicy> setWritePolicyReadOnly(final List<? extends NpmRepository> repositories)
      throws IOException
  {
    final List<RepositoryWritePolicy> result = Lists.newArrayList();
    for (NpmRepository repository : repositories) {
      result.add(repository.getWritePolicy());
      repository.setWritePolicy(RepositoryWritePolicy.READ_ONLY);
    }
    nexusConfiguration.saveConfiguration();
    return result;
  }

  protected void resetWritePolicy(final List<? extends NpmRepository> repositories,
                                  final List<RepositoryWritePolicy> writePolicies) throws IOException
  {
    checkArgument(repositories.size() == writePolicies.size(), "inconsistent original write policies");
    for (int i = 0; i < repositories.size(); i++) {
      repositories.get(i).setWritePolicy(writePolicies.get(i));
    }
    nexusConfiguration.saveConfiguration();
  }
}
