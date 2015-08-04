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
package org.sonatype.nexus.templates.repository.maven;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Maven2HostedRepositoryTemplate
    extends AbstractMavenRepositoryTemplate
{
  public Maven2HostedRepositoryTemplate(DefaultRepositoryTemplateProvider provider, String id, String description,
                                        RepositoryPolicy repositoryPolicy)
  {
    super(provider, id, description, new Maven2ContentClass(), MavenHostedRepository.class, repositoryPolicy);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(Repository.class.getName());
    repo.setProviderHint("maven2");

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
    // huh? see initConfig classes
    if (getRepositoryPolicy() != null) {
      exConf.setRepositoryPolicy(getRepositoryPolicy());
    }

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE_ONCE.name());
    repo.setNotFoundCacheTTL(1440);
    repo.setIndexable(true);
    repo.setSearchable(true);

    CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(
            getTemplateProvider().getApplicationConfiguration(),
            repo,
            new CRepositoryExternalConfigurationHolderFactory<M2RepositoryConfiguration>()
            {
              public M2RepositoryConfiguration createExternalConfigurationHolder(
                  CRepository config)
              {
                return new M2RepositoryConfiguration((Xpp3Dom) config
                    .getExternalConfiguration());
              }
            });

    return result;
  }
}
