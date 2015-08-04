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
package org.sonatype.nexus.plugins.repository;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

@Named("default")
@Typed(SimpleRepository.class)
@Description("Simple Repository")
public class SimpleRepositoryImpl
    extends AbstractRepository
    implements SimpleRepository
{
  private final ContentClass contentClass;

  private final SimpleRepositoryConfigurator simpleRepositoryConfigurator;

  private final RepositoryKind repositoryKind = new DefaultRepositoryKind(SimpleRepository.class, null);

  @Inject
  public SimpleRepositoryImpl(final @Named(SimpleContentClass.ID) ContentClass contentClass,
                              final SimpleRepositoryConfigurator simpleRepositoryConfigurator) {
    this.contentClass = contentClass;
    this.simpleRepositoryConfigurator = simpleRepositoryConfigurator;
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  protected Configurator getConfigurator() {
    return simpleRepositoryConfigurator;
  }

  @Override
  protected SimpleRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (SimpleRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<SimpleRepositoryConfiguration> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<SimpleRepositoryConfiguration>()
    {
      public SimpleRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new SimpleRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public synchronized String sayHello() {
    int cnt = getExternalConfiguration(false).getSaidHelloCount();

    getExternalConfiguration(true).setSaidHelloCount(cnt++);

    log.info(String.format("Saying \"Hello\" for %s time.", cnt));

    return "hello";
  }
}
