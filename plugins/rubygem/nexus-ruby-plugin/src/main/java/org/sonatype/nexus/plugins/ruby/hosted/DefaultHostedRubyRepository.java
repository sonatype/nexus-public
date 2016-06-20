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
package org.sonatype.nexus.plugins.ruby.hosted;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.ruby.NexusRubygemsFacade;
import org.sonatype.nexus.plugins.ruby.NexusStorage;
import org.sonatype.nexus.plugins.ruby.RubyContentClass;
import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.layout.HostedRubygemsFileSystem;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Default {@link HostedRubyRepository} implementation.
 *
 * @since 2.11
 */
@Named(DefaultHostedRubyRepository.ID)
public class DefaultHostedRubyRepository
    extends AbstractRepository
    implements HostedRubyRepository, Repository
{
  public static final String ID = "rubygems-hosted";

  private final ContentClass contentClass;

  private final HostedRubyRepositoryConfigurator configurator;

  private final RubygemsGateway gateway;

  private final RepositoryKind repositoryKind;

  private final NexusRubygemsFacade facade;

  @Inject
  public DefaultHostedRubyRepository(final @Named(RubyContentClass.ID) ContentClass contentClass,
                                     final HostedRubyRepositoryConfigurator configurator,
                                     final RubygemsGateway gateway)
  {
    this.contentClass = contentClass;
    this.configurator = configurator;
    this.repositoryKind = new DefaultRepositoryKind(HostedRubyRepository.class,
        Arrays.asList(new Class<?>[]{RubyRepository.class}));
    this.gateway = gateway;
    this.facade = new NexusRubygemsFacade(new HostedRubygemsFileSystem(gateway, new NexusStorage(this)));
  }

  @Override
  protected Configurator<Repository, CRepositoryCoreConfiguration> getConfigurator() {
    return configurator;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<DefaultHostedRubyRepositoryConfiguration>()
    {
      public DefaultHostedRubyRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new DefaultHostedRubyRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
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
  protected DefaultHostedRubyRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (DefaultHostedRubyRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void storeItem(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    RubygemsFile file = facade.file(request.getRequestPath());
    if (file == null) {
      throw new UnsupportedStorageOperationException("only gem-files can be stored");
    }
    request.setRequestPath(file.storagePath());
    // first check permissions, i.e. is redeploy allowed
    try {
      checkConditions(request, getResultingActionOnWrite(request));
    }
    catch (ItemNotFoundException e) {
      throw new AccessDeniedException(request, e.getMessage());
    }

    // now store the gem
    facade.handleMutation(this, facade.post(is, file));
  }

  @SuppressWarnings("deprecation")
  @Override
  public void deleteItem(ResourceStoreRequest request)
      throws StorageException, UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException
  {
    facade.handleMutation(this, facade.delete(request.getRequestPath()));
  }

  @Override
  public void moveItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException
  {
    throw new UnsupportedStorageOperationException(from.getRequestPath());
  }

  @Override
  public StorageItem retrieveDirectItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, IOException
  {
    // bypass access control
    return super.retrieveItem(false, request);
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

  @Override
  public void recreateMetadata() throws LocalStorageException, ItemNotFoundException {
    log.info("Recreating Rubygems index in hosted repository {}", this);
    final RecreateIndexRubygemsWalkerProcessor wp = new RecreateIndexRubygemsWalkerProcessor(log, gateway, facade);
    final DefaultWalkerContext ctx = new DefaultWalkerContext(this, new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT));
    ctx.getProcessors().add(wp);
    try {
      getWalker().walk(ctx);
    }
    catch (WalkerException e) {
      if (!(e.getWalkerContext().getStopCause() instanceof ItemNotFoundException)) {
        // everything that is not ItemNotFound should be reported,
        // otherwise just neglect it
        throw e;
      }
    }
    wp.storeIndex();
  }
}
