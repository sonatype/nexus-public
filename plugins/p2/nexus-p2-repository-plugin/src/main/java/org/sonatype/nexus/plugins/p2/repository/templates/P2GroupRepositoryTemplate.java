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

import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.plugins.p2.repository.P2ContentClass;
import org.sonatype.nexus.plugins.p2.repository.P2GroupRepository;
import org.sonatype.nexus.plugins.p2.repository.group.P2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.Preconditions.checkNotNull;

public class P2GroupRepositoryTemplate
    extends AbstractRepositoryTemplate
{

  private final String providerHint;

  public P2GroupRepositoryTemplate(final P2RepositoryTemplateProvider provider,
                                   final String id,
                                   final String description,
                                   final Class<? extends P2GroupRepository> mainFacet,
                                   final String providerHint)
  {
    super(provider, id, description, new P2ContentClass(), mainFacet);
    this.providerHint = checkNotNull(providerHint);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    final CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(GroupRepository.class.getName());
    repo.setProviderHint(providerHint);

    final Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    final P2GroupRepositoryConfiguration exConf = new P2GroupRepositoryConfiguration(ex);

    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());

    final CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(getTemplateProvider().getApplicationConfiguration(), repo,
            new CRepositoryExternalConfigurationHolderFactory<P2GroupRepositoryConfiguration>()
            {
              @Override
              public P2GroupRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
                return new P2GroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
              }
            });

    return result;
  }
}