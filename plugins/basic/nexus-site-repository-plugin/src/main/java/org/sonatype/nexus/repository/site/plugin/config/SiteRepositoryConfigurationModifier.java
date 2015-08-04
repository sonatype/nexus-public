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
package org.sonatype.nexus.repository.site.plugin.config;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ConfigurationModifier;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.repository.site.plugin.SiteRepository;

/**
 * Modifies nexus.xml if needed:
 * - repository hint from "maven-site" to "site"
 * - repository target from "maven-site" to "site"
 *
 * @since site-repository 1.0
 */
@Named
@Singleton
public class SiteRepositoryConfigurationModifier
    implements ConfigurationModifier
{

  @Override
  public boolean apply(org.sonatype.nexus.configuration.model.Configuration configuration) {
    boolean modified = false;
    final List<CRepository> repositories = configuration.getRepositories();
    if (repositories != null && repositories.size() > 0) {
      for (final CRepository repository : repositories) {
        if ("maven-site".equals(repository.getProviderHint())) {
          repository.setProviderHint(SiteRepository.ID);
          modified = true;
        }
      }
    }
    final List<CRepositoryTarget> repositoryTargets = configuration.getRepositoryTargets();
    if (repositoryTargets != null && repositoryTargets.size() > 0) {
      for (final CRepositoryTarget repositoryTarget : repositoryTargets) {
        if ("maven-site".equals(repositoryTarget.getId())) {
          repositoryTarget.setId(SiteRepository.ID);
          modified = true;
        }
        if ("maven-site".equals(repositoryTarget.getContentClass())) {
          repositoryTarget.setContentClass(SiteRepository.ID);
          modified = true;
        }
        if (repositoryTarget.getName() != null && repositoryTarget.getName().contains("maven-site")) {
          repositoryTarget.setName(repositoryTarget.getName().replace("maven-site", SiteRepository.ID));
          modified = true;
        }
      }
    }
    return modified;
  }

}
