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
package org.sonatype.nexus.plugins.ruby.group;

import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.ruby.NexusRubygemsFacade;
import org.sonatype.nexus.plugins.ruby.RubyContentClass;
import org.sonatype.nexus.plugins.ruby.RubyGroupRepository;
import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractGroupRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;

import org.codehaus.plexus.util.xml.Xpp3Dom;

import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * Default {@link RubyGroupRepository} implementation.
 *
 * @since 2.11
 */
@Named(DefaultRubyGroupRepository.ID)
public class DefaultRubyGroupRepository
    extends AbstractGroupRepository
    implements RubyGroupRepository, GroupRepository
{
  public static final String ID = "rubygems-group";

  private final ContentClass contentClass;

  private final GroupRubyRepositoryConfigurator configurator;

  private final RepositoryKind repositoryKind;

  private final NexusRubygemsFacade facade;

  @Inject
  public DefaultRubyGroupRepository(final @Named(RubyContentClass.ID) ContentClass contentClass,
                                    final GroupRubyRepositoryConfigurator configurator,
                                    final RubygemsGateway gateway)
  {
    this.contentClass = contentClass;
    this.configurator = configurator;
    this.facade = new NexusRubygemsFacade(new ProxiedRubygemsFileSystem(gateway, new GroupNexusStorage(this, gateway)));
    this.repositoryKind = new DefaultRepositoryKind(RubyGroupRepository.class,
        Arrays.asList(new Class<?>[]{RubyRepository.class}));
  }

  @Override
  protected Configurator<Repository, CRepositoryCoreConfiguration> getConfigurator() {
    return configurator;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<DefaultRubyGroupRepositoryConfiguration>()
    {
      public DefaultRubyGroupRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new DefaultRubyGroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  protected DefaultRubyGroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (DefaultRubyGroupRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  public void deleteItem(ResourceStoreRequest request) throws UnsupportedStorageOperationException {
    throw new UnsupportedStorageOperationException(request.getRequestPath());
  }

  @Override
  public void moveItem(ResourceStoreRequest from, ResourceStoreRequest to) throws UnsupportedStorageOperationException {
    throw new UnsupportedStorageOperationException(from.getRequestPath());
  }

  @SuppressWarnings("deprecation")
  @Override
  public StorageItem retrieveItem(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (fromTask && request.getRequestPath().startsWith("/.nexus")) {
      return super.retrieveItem(true, request);
    }
    return facade.handleRetrieve(this, request, facade.get(request));
  }

  @SuppressWarnings("deprecation")
  public StorageItem retrieveDirectItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, IOException
  {
    for (Repository repo : getMemberRepositories()) {
      try {
        return repo.retrieveItem(false, request);
      }
      catch (ItemNotFoundException e) {
        // ignore
      }
    }
    throw new ItemNotFoundException(reasonFor(request, this,
        "Could not find content for path %s in local storage of repository %s",
        request.getRequestPath(),
        RepositoryStringUtils.getHumanizedNameString(this)));
  }
}