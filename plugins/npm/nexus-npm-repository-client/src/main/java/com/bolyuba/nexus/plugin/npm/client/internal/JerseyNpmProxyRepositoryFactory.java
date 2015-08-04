/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm.client.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyProxyRepositoryFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;

import com.bolyuba.nexus.plugin.npm.client.NpmProxyRepository;

@Named
@Singleton
public class JerseyNpmProxyRepositoryFactory
    extends JerseyProxyRepositoryFactory
{
  @Override
  public int canAdapt(final RepositoryBaseResource resource) {
    int score = super.canAdapt(resource);
    if (score > 0) {
      if (JerseyNpmProxyRepository.PROVIDER_ROLE.equals(resource.getProviderRole()) &&
          JerseyNpmProxyRepository.PROVIDER.equals(resource.getProvider())) {
        score++;
      }
    }
    return score;
  }

  @Override
  public JerseyNpmProxyRepository adapt(final JerseyNexusClient nexusClient,
                                        final RepositoryBaseResource resource)
  {
    return new JerseyNpmProxyRepository(nexusClient, (RepositoryProxyResource) resource);
  }

  @Override
  public boolean canCreate(final Class<? extends Repository> type) {
    return NpmProxyRepository.class.equals(type);
  }

  @Override
  public JerseyNpmProxyRepository create(final JerseyNexusClient nexusClient, final String id) {
    return new JerseyNpmProxyRepository(nexusClient, id);
  }

}
