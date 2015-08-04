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
package org.sonatype.nexus.repository.site.plugin;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.repository.WebSiteRepository;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class SiteRepositoryTemplate
    extends AbstractRepositoryTemplate
{

  public SiteRepositoryTemplate(SiteRepositoryTemplateProvider provider, String id, String description) {
    super(provider, id, description, new SiteContentClass(), SiteRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");

    repo.setProviderRole(WebSiteRepository.class.getName());
    repo.setProviderHint(SiteRepository.ID);

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    repo.setIndexable(false);

    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE.name());
    repo.setNotFoundCacheTTL(1440);

    CRepositoryCoreConfiguration result = new CRepositoryCoreConfiguration(
        getTemplateProvider().getApplicationConfiguration(),
        repo,
        new CRepositoryExternalConfigurationHolderFactory<DefaultSiteRepositoryConfiguration>()
        {
          public DefaultSiteRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
            return new DefaultSiteRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
          }
        });

    return result;
  }

}
