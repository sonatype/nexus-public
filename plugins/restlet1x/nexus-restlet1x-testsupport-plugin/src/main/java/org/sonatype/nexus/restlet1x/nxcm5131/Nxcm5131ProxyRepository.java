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
package org.sonatype.nexus.restlet1x.nxcm5131;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.registry.AbstractIdContentClass;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * A proxy repository extending AbstractProxyRepository and using the maven external configuration,
 * but with a different facet.
 */
@Named("nxcm5131")
public class Nxcm5131ProxyRepository
    extends AbstractProxyRepository
    implements Repository, Nxcm5131HostedRepository, ProxyRepository
{

  private final RepositoryKind repositoryKind =
      new MutableProxyRepositoryKind(this, null, new DefaultRepositoryKind(Nxcm5131HostedRepository.class, null),
          new DefaultRepositoryKind(Nxcm5131ProxyRepository.class, null));

  private final Nxcm5131RepositoryConfigurator configurator;

  @Inject
  public Nxcm5131ProxyRepository(final Nxcm5131RepositoryConfigurator configurator) {
    this.configurator = configurator;
  }

  @Override
  protected Configurator getConfigurator() {
    return configurator;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<AbstractProxyRepositoryConfiguration>()
    {
      @Override
      public AbstractProxyRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new AbstractProxyRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration())
        {
        };
      }
    };
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return new AbstractIdContentClass()
    {
      @Override
      public String getId() {
        return "nxcm5131";
      }
    };
  }
}
