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
package org.sonatype.nexus.plugins.ruby.proxy;

import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.plugins.ruby.AbstractRubyGemRepositoryTemplate;
import org.sonatype.nexus.plugins.ruby.RubyContentClass;
import org.sonatype.nexus.plugins.ruby.RubyRepositoryTemplateProvider;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * {@link DefaultProxyRubyRepository} template.
 *
 * @since 2.11
 */
public class DefaultProxyRubyRepositoryTemplate
    extends AbstractRubyGemRepositoryTemplate
{
  public DefaultProxyRubyRepositoryTemplate(RubyRepositoryTemplateProvider provider, String id, String description) {
    super(provider, id, description, new RubyContentClass(), ProxyRubyRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(Repository.class.getName());
    repo.setProviderHint(DefaultProxyRubyRepository.ID);

    repo.setRemoteStorage(new CRemoteStorage());
    repo.getRemoteStorage().setProvider(
        getTemplateProvider().getRemoteProviderHintFactory().getDefaultHttpRoleHint());
    repo.getRemoteStorage().setUrl("http://some-remote-repository/repo-root");

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    DefaultProxyRubyRepositoryConfiguration exConf = new DefaultProxyRubyRepositoryConfiguration(ex);
    exConf.setMetadataMaxAge(30);
    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());
    repo.setNotFoundCacheActive(true);
    repo.setNotFoundCacheTTL(1440);

    repo.setIndexable(true);
    repo.setSearchable(true);

    CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
            new CRepositoryExternalConfigurationHolderFactory<DefaultProxyRubyRepositoryConfiguration>()
            {
              public DefaultProxyRubyRepositoryConfiguration createExternalConfigurationHolder(
                  CRepository config)
              {
                return new DefaultProxyRubyRepositoryConfiguration((Xpp3Dom) config
                    .getExternalConfiguration());
              }
            });

    return result;
  }
}
