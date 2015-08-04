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

import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyGroupRepository;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyHostedRepository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;

import com.bolyuba.nexus.plugin.npm.client.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;

public class JerseyNpmGroupRepository
    extends JerseyGroupRepository<NpmGroupRepository>
    implements NpmGroupRepository
{
  static final String PROVIDER_ROLE = "org.sonatype.nexus.proxy.repository.GroupRepository";

  static final String PROVIDER = "npm-group";

  public JerseyNpmGroupRepository(final JerseyNexusClient nexusClient, final String id) {
    super(nexusClient, id);
  }

  public JerseyNpmGroupRepository(final JerseyNexusClient nexusClient,
                                  final RepositoryGroupResource settings)
  {
    super(nexusClient, settings);
  }

  @Override
  protected RepositoryGroupResource createSettings() {
    final RepositoryGroupResource settings = super.createSettings();

    settings.setProviderRole(JerseyNpmGroupRepository.PROVIDER_ROLE);
    settings.setProvider(JerseyNpmGroupRepository.PROVIDER);

    return settings;
  }
}
