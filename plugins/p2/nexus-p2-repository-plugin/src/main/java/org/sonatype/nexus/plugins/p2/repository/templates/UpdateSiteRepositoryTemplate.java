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
package org.sonatype.nexus.plugins.p2.repository.templates;

import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.plugins.p2.repository.P2ContentClass;
import org.sonatype.nexus.plugins.p2.repository.UpdateSiteProxyRepository;
import org.sonatype.nexus.plugins.p2.repository.updatesite.UpdateSiteProxyRepositoryImpl;
import org.sonatype.nexus.plugins.p2.repository.updatesite.UpdateSiteRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class UpdateSiteRepositoryTemplate
    extends AbstractRepositoryTemplate
{
  public UpdateSiteRepositoryTemplate(final P2RepositoryTemplateProvider provider, final String id,
                                      final String description)
  {
    super(provider, id, description, new P2ContentClass(), UpdateSiteProxyRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(Repository.class.getName());
    repo.setProviderHint(UpdateSiteProxyRepositoryImpl.ROLE_HINT);

    repo.setRemoteStorage(new CRemoteStorage());
    repo.getRemoteStorage().setProvider(getTemplateProvider().getRemoteProviderHintFactory().getDefaultHttpRoleHint());
    repo.getRemoteStorage().setUrl("http://some-remote-repository/repo-root");

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final UpdateSiteRepositoryConfiguration exConf = new UpdateSiteRepositoryConfiguration(ex);

    exConf.setArtifactMaxAge(-1);
    exConf.setMetadataMaxAge(1440);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());
    repo.setNotFoundCacheActive(true);
    repo.setNotFoundCacheTTL(1440);

    final CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
            new CRepositoryExternalConfigurationHolderFactory<UpdateSiteRepositoryConfiguration>()
            {
              @Override
              public UpdateSiteRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
                return new UpdateSiteRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
              }
            });

    return result;
  }
}
