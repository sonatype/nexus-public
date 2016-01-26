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
package org.sonatype.nexus.plugins.ruby.proxy;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.plugins.ruby.NexusRubygemsFacade;
import org.sonatype.nexus.plugins.ruby.PurgeBrokenFilesRubygemsWalkerProcessor;
import org.sonatype.nexus.plugins.ruby.RubyContentClass;
import org.sonatype.nexus.plugins.ruby.RubyRepository;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.walker.WalkerProcessor;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.SpecsIndexType;
import org.sonatype.nexus.ruby.cuba.RootCuba;
import org.sonatype.nexus.ruby.cuba.api.ApiV1DependenciesCuba;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Default {@link ProxyRubyRepository} implementation.
 *
 * @since 2.11
 */
@Named(DefaultProxyRubyRepository.ID)
public class DefaultProxyRubyRepository
    extends AbstractProxyRepository
    implements ProxyRubyRepository, Repository
{
  public static final String ID = "rubygems-proxy";

  private final ContentClass contentClass;

  private final ProxyRubyRepositoryConfigurator configurator;

  private final RubygemsGateway gateway;

  private final RepositoryKind repositoryKind;

  private NexusRubygemsFacade facade;

  @Inject
  public DefaultProxyRubyRepository(final @Named(RubyContentClass.ID) ContentClass contentClass,
                                    final ProxyRubyRepositoryConfigurator configurator,
                                    final RubygemsGateway gateway)
  {
    this.contentClass = contentClass;
    this.configurator = configurator;
    this.gateway = gateway;
    this.repositoryKind = new DefaultRepositoryKind(ProxyRubyRepository.class,
        Arrays.asList(new Class<?>[]{RubyRepository.class}));
    this.facade = new NexusRubygemsFacade(new ProxiedRubygemsFileSystem(gateway, new ProxyNexusStorage(this)));
  }

  @Override
  protected Configurator<Repository, CRepositoryCoreConfiguration> getConfigurator() {
    return configurator;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<DefaultProxyRubyRepositoryConfiguration>()
    {
      public DefaultProxyRubyRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new DefaultProxyRubyRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
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
  protected DefaultProxyRubyRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (DefaultProxyRubyRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  public boolean isOld(StorageItem item) {
    if (item.getName().endsWith("specs.4.8")) {
      // whenever there is retrieve call to the ungzipped file it will be forwarded to call for the gzipped file
      return false;
    }
    int maxAge;
    if (item.getName().endsWith(RootCuba.GZ) ||
        item.getName().endsWith(ApiV1DependenciesCuba.RUBY) ||
        BUNDLER_API_REQUEST.matcher(item.getName()).matches()) {
      maxAge = getMetadataMaxAge();
    }
    else {
      // all other files use artifact max age
      maxAge = getArtifactMaxAge();
    }
    boolean isOld = isOld(maxAge, item);
    log.debug("{} needs remote update {}", item, isOld);
    return isOld;
  }

  public int getArtifactMaxAge() {
    return getExternalConfiguration(false).getArtifactMaxAge();
  }

  public void setArtifactMaxAge(int maxAge) {
    getExternalConfiguration(true).setArtifactMaxAge(maxAge);
  }

  public int getMetadataMaxAge() {
    return getExternalConfiguration(false).getMetadataMaxAge();
  }

  public void setMetadataMaxAge(int metadataMaxAge) {
    getExternalConfiguration(true).setMetadataMaxAge(metadataMaxAge);
  }

  private static Pattern BUNDLER_API_REQUEST = Pattern.compile(".*[?]gems=.*");

  @Override
  public RepositoryItemUid createUid(final String path) {
    RubygemsFile file = facade.file(path);
    if (file.type() == FileType.NOT_FOUND) {
      // nexus internal path like .nexus/**/*
      return super.createUid(path);
    }
    return super.createUid(file.storagePath());
  }

  @Override
  public void moveItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException
  {
    throw new UnsupportedStorageOperationException(from.getRequestPath());
  }

  @SuppressWarnings("deprecation")
  @Override
  public StorageItem retrieveDirectItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, IOException
  {
    // bypass access control
    return super.retrieveItem(false, request);
  }

  @SuppressWarnings("deprecation")
  @Override
  public StorageItem retrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException, AccessDeniedException
  {
    // TODO do not use this since it bypasses access control
    if (request.getRequestPath().startsWith("/.nexus")) {
      return super.retrieveItem(request);
    }

    return facade.handleRetrieve(this, request, facade.get(request));
  }

  @SuppressWarnings("deprecation")
  @Override
  public StorageItem retrieveItem(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, org.sonatype.nexus.proxy.StorageException
  {
    if (!fromTask && request.getRequestPath().contains("?gems=") && !request.getRequestPath().startsWith("/.nexus")) {
      return facade.handleRetrieve(this, request, facade.get(request));
    }
    else {
      return super.retrieveItem(fromTask, request);
    }
  }

  @Override
  public void syncMetadata() throws IllegalOperationException, ItemNotFoundException, IOException
  {
    log.debug("sync rubygems specs.4.8.gz, latest_specs.4.8.gz, prereleased_specs.4.8.gz");
    for (SpecsIndexType type : SpecsIndexType.values()) {
      ResourceStoreRequest request = new ResourceStoreRequest(type.filepathGzipped());
      request.setRequestRemoteOnly(true);
      retrieveItem(true, request);
    }
    purgeBrokenMetadataFiles();
  }

  @Override
  public void purgeBrokenMetadataFiles() throws IllegalOperationException, ItemNotFoundException, IOException
  {
    log.info("Purge broken Rubygems metadata files on proxy-repository {}", this);
    final WalkerProcessor wp = new PurgeBrokenFilesRubygemsWalkerProcessor(log, gateway);
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
  }

  @Override
  public void setRemoteStorage(final RemoteRepositoryStorage remoteStorage) {
    if (remoteStorage == null) {
      super.setRemoteStorage(null); // resetting, nothing to proxy
      return;
    }
    //
    // intercept request parameters to rewrite their paths when going remote
    //
    // an alternative solution is to override doRetrieveRemoteItem, copy over the
    // skeleton AbstractProxyRepository code and rewrite the request paths for each
    // call to remote storage (if you try to rewrite the request path for the whole
    // doRetrieveRemoteItem method that then messes up various NFC and expiry calls).
    //
    super.setRemoteStorage((RemoteRepositoryStorage) Proxy.newProxyInstance(
        RemoteRepositoryStorage.class.getClassLoader(),
        new Class[] { RemoteRepositoryStorage.class },
        new InvocationHandler()
        {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ResourceStoreRequest request = findRequest(args);
            if (request != null) {
              // rewrite path, but only for scope of remote storage
              RubygemsFile file = facade.file(request.getRequestPath());
              request.pushRequestPath(file.remotePath());
            }
            try {
              return method.invoke(remoteStorage, args);
            }
            catch (InvocationTargetException e) {
              throw e.getCause(); // unwrap and throw the real exception
            }
            finally {
              if (request != null) {
                request.popRequestPath();
              }
            }
          }

          private ResourceStoreRequest findRequest(Object[] args) {
            if (args != null) {
              for (Object arg : args) {
                if (arg instanceof ResourceStoreRequest) {
                  return (ResourceStoreRequest) arg;
                }
              }
            }
            return null;
          }
        }));
  }
}
