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
package com.bolyuba.nexus.plugin.npm.templates;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import com.bolyuba.nexus.plugin.npm.NpmContentClass;
import com.bolyuba.nexus.plugin.npm.group.DefaultNpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepositoryConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
public class NpmGroupRepositoryTemplate
    extends AbstractRepositoryTemplate
{

  public NpmGroupRepositoryTemplate(final NpmRepositoryTemplateProvider provider, final String id,
                                    final String description)
  {
    super(provider, id, description, new NpmContentClass(), DefaultNpmGroupRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();
    repo.setId("test");
    repo.setName("test");

    repo.setProviderRole(GroupRepository.class.getName());
    repo.setProviderHint(DefaultNpmGroupRepository.ROLE_HINT);

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final NpmGroupRepositoryConfiguration exConf = new NpmGroupRepositoryConfiguration(ex);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setNotFoundCacheTTL(1440);
    repo.setIndexable(true);
    repo.setSearchable(true);

    return new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
        new CRepositoryExternalConfigurationHolderFactory<NpmGroupRepositoryConfiguration>()
        {
          @Override
          public NpmGroupRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
            return new NpmGroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
          }
        });
  }

  @Override
  public NpmGroupRepository create() throws ConfigurationException, IOException {
    return (NpmGroupRepository) super.create();
  }
}
