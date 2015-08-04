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
package org.sonatype.nexus.plugins.ruby.group;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.plugins.ruby.AbstractRubyGemRepositoryTemplate;
import org.sonatype.nexus.plugins.ruby.RubyContentClass;
import org.sonatype.nexus.plugins.ruby.RubyGroupRepository;
import org.sonatype.nexus.plugins.ruby.RubyRepositoryTemplateProvider;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * {@link DefaultRubyGroupRepository} template.
 *
 * @since 2.11
 */
public class DefaultRubyGroupRepositoryTemplate
    extends AbstractRubyGemRepositoryTemplate
{
  public DefaultRubyGroupRepositoryTemplate(RubyRepositoryTemplateProvider provider, String id, String description) {
    super(provider, id, description, new RubyContentClass(), RubyGroupRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(GroupRepository.class.getName());
    repo.setProviderHint(DefaultRubyGroupRepository.ID);

    // groups should not participate in searches
    repo.setSearchable(false);

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    DefaultRubyGroupRepositoryConfiguration exConf = new DefaultRubyGroupRepositoryConfiguration(ex);
    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());

    CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
            new CRepositoryExternalConfigurationHolderFactory<DefaultRubyGroupRepositoryConfiguration>()
            {
              public DefaultRubyGroupRepositoryConfiguration createExternalConfigurationHolder(
                  CRepository config)
              {
                return new DefaultRubyGroupRepositoryConfiguration((Xpp3Dom) config
                    .getExternalConfiguration());
              }
            });

    return result;
  }
}
