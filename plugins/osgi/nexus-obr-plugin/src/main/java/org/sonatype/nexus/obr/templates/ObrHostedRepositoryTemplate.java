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
package org.sonatype.nexus.obr.templates;

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.obr.ObrContentClass;
import org.sonatype.nexus.obr.proxy.ObrHostedRepository;
import org.sonatype.nexus.obr.proxy.ObrRepository;
import org.sonatype.nexus.obr.proxy.ObrRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ObrHostedRepositoryTemplate
    extends AbstractRepositoryTemplate
{
  public ObrHostedRepositoryTemplate(final ObrRepositoryTemplateProvider provider, final String id,
                                     final String description)
  {
    super(provider, id, description, new ObrContentClass(), ObrHostedRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(Repository.class.getName());
    repo.setProviderHint(ObrRepository.ROLE_HINT);

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final ObrRepositoryConfiguration exConf = new ObrRepositoryConfiguration(ex);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE_ONCE.name());
    repo.setNotFoundCacheTTL(1440);

    final CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(
            getTemplateProvider().getApplicationConfiguration(),
            repo,
            new CRepositoryExternalConfigurationHolderFactory<ObrRepositoryConfiguration>()
            {
              public ObrRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
                return new ObrRepositoryConfiguration(
                    (Xpp3Dom) config.getExternalConfiguration());
              }
            });

    return result;
  }
}
