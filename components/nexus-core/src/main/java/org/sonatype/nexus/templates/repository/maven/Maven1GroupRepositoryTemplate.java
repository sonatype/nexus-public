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
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.maven1.M1GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.templates.repository.DefaultRepositoryTemplateProvider;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Maven1GroupRepositoryTemplate
    extends AbstractMavenRepositoryTemplate
{
  public Maven1GroupRepositoryTemplate(DefaultRepositoryTemplateProvider provider, String id, String description) {
    super(provider, id, description, new Maven1ContentClass(), MavenGroupRepository.class, null);
  }

  public M1GroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (M1GroupRepositoryConfiguration) getCoreConfiguration().getExternalConfiguration()
        .getConfiguration(forWrite);
  }

  @Override
  protected CRepositoryCoreConfiguration initCoreConfiguration() {
    CRepository repo = new DefaultCRepository();

    repo.setId("");
    repo.setName("");

    repo.setProviderRole(GroupRepository.class.getName());
    repo.setProviderHint("maven1");

    // groups should not participate in searches
    repo.setSearchable(false);

    Xpp3Dom ex = new Xpp3Dom(DefaultCRepository.EXTERNAL_CONFIGURATION_NODE_NAME);
    repo.setExternalConfiguration(ex);

    M1GroupRepositoryConfiguration exConf = new M1GroupRepositoryConfiguration(ex);
    repo.externalConfigurationImple = exConf;

    repo.setWritePolicy(RepositoryWritePolicy.READ_ONLY.name());

    CRepositoryCoreConfiguration result =
        new CRepositoryCoreConfiguration(
            getTemplateProvider().getApplicationConfiguration(),
            repo,
            new CRepositoryExternalConfigurationHolderFactory<M1GroupRepositoryConfiguration>()
            {
              public M1GroupRepositoryConfiguration createExternalConfigurationHolder(
                  CRepository config)
              {
                return new M1GroupRepositoryConfiguration((Xpp3Dom) config
                    .getExternalConfiguration());
              }
            });

    return result;
  }
}
