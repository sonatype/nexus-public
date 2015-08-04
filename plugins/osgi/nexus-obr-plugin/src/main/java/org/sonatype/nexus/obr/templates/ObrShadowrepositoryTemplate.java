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
import org.sonatype.nexus.obr.shadow.ObrShadowRepository;
import org.sonatype.nexus.obr.shadow.ObrShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ObrShadowrepositoryTemplate
    extends AbstractRepositoryTemplate
{
  public ObrShadowrepositoryTemplate(final ObrRepositoryTemplateProvider provider, final String id,
                                     final String description)
  {
    super(provider, id, description, new ObrContentClass(), ObrShadowRepository.class);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(ShadowRepository.class.getName());
    repo.setProviderHint(ObrShadowRepository.ROLE_HINT);

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final ObrShadowRepositoryConfiguration exConf = new ObrShadowRepositoryConfiguration(ex);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());

    final CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(
            getTemplateProvider().getApplicationConfiguration(),
            repo,
            new CRepositoryExternalConfigurationHolderFactory<ObrShadowRepositoryConfiguration>()
            {
              public ObrShadowRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
                return new ObrShadowRepositoryConfiguration(
                    (Xpp3Dom) config.getExternalConfiguration());
              }
            });

    return result;
  }
}
