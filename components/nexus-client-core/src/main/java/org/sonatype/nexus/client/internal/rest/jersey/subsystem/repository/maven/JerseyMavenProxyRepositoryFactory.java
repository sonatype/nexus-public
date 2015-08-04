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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.maven;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyProxyRepositoryFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;

/**
 * {@link JerseyMavenProxyRepository} factory.
 *
 * @since 2.3
 */
@Named
@Singleton
public class JerseyMavenProxyRepositoryFactory
    extends JerseyProxyRepositoryFactory
{

  @Override
  public int canAdapt(final RepositoryBaseResource resource) {
    int score = super.canAdapt(resource);
    if (score > 0) {
      if (JerseyMavenProxyRepository.PROVIDER.equals(resource.getProvider())) {
        score++;
      }
    }
    return score;
  }

  @Override
  public JerseyMavenProxyRepository adapt(final JerseyNexusClient nexusClient,
                                          final RepositoryBaseResource resource)
  {
    return new JerseyMavenProxyRepository(nexusClient, (RepositoryProxyResource) resource);
  }

  @Override
  public boolean canCreate(final Class<? extends Repository> type) {
    return MavenProxyRepository.class.equals(type);
  }

  @Override
  public JerseyMavenProxyRepository create(final JerseyNexusClient nexusClient, final String id) {
    return new JerseyMavenProxyRepository(nexusClient, id);
  }

}
