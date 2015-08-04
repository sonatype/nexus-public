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
import org.sonatype.nexus.proxy.maven.MavenShadowRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Maven1Maven2ShadowRepositoryTemplate
    extends AbstractMavenRepositoryTemplate
{
  public Maven1Maven2ShadowRepositoryTemplate(DefaultRepositoryTemplateProvider provider, String id,
                                              String description)
  {
    super(provider, id, description, new Maven2ContentClass(), MavenShadowRepository.class, null);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(ShadowRepository.class.getName());
    repo.setProviderHint("m1-m2-shadow");

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    M2LayoutedM1ShadowRepositoryConfiguration exConf = new M2LayoutedM1ShadowRepositoryConfiguration(ex);
    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());

    CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(
            getTemplateProvider().getApplicationConfiguration(),
            repo,
            new CRepositoryExternalConfigurationHolderFactory<M2LayoutedM1ShadowRepositoryConfiguration>()
            {
              public M2LayoutedM1ShadowRepositoryConfiguration createExternalConfigurationHolder(
                  CRepository config)
              {
                return new M2LayoutedM1ShadowRepositoryConfiguration(
                    (Xpp3Dom) config
                        .getExternalConfiguration());
              }
            });

    return result;
  }
}
