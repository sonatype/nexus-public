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
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenM1VirtualRepository;
import org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository.JerseyVirtualRepositoryFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;

/**
 * {@link JerseyMavenM1VirtualRepository} factory.
 *
 * @since 2.3
 */
@Named
@Singleton
public class JerseyMavenM1VirtualRepositoryFactory
    extends JerseyVirtualRepositoryFactory
{

  @Override
  public int canAdapt(final RepositoryBaseResource resource) {
    int score = super.canAdapt(resource);
    if (score > 0) {
      if (JerseyMavenM1VirtualRepository.PROVIDER.equals(resource.getProvider())) {
        score++;
      }
    }
    return score;
  }

  @Override
  public JerseyMavenM1VirtualRepository adapt(final JerseyNexusClient nexusClient,
                                              final RepositoryBaseResource resource)
  {
    return new JerseyMavenM1VirtualRepository(nexusClient, (RepositoryShadowResource) resource);
  }

  @Override
  public boolean canCreate(final Class<? extends Repository> type) {
    return MavenM1VirtualRepository.class.equals(type);
  }

  @Override
  public JerseyMavenM1VirtualRepository create(final JerseyNexusClient nexusClient, final String id) {
    return new JerseyMavenM1VirtualRepository(nexusClient, id);
  }

}
