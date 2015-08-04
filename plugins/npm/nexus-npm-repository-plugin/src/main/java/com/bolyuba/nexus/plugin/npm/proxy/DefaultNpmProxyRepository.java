/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.proxy;

import java.io.IOException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

import com.bolyuba.nexus.plugin.npm.NpmContentClass;
import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.internal.NpmMimeRulesSource;
import com.bolyuba.nexus.plugin.npm.service.MetadataServiceFactory;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;
import com.bolyuba.nexus.plugin.npm.service.ProxyMetadataService;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballSource;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
@Named(DefaultNpmProxyRepository.ROLE_HINT)
@Typed(Repository.class)
@Description("Npm registry proxy repo")
public class DefaultNpmProxyRepository
    extends AbstractProxyRepository
    implements NpmProxyRepository, Repository
{

  public static final String ROLE_HINT = "npm-proxy";

  /**
   * Key to stash under the created tarball request in resource store request context.
   */
  private static final String TARBALL_REQUEST = "npm.tarballRequest";

  private final ContentClass contentClass;

  private final NpmProxyRepositoryConfigurator configurator;

  private final RepositoryKind repositoryKind;

  private final NpmMimeRulesSource mimeRulesSource;

  private final ProxyMetadataService proxyMetadataService;

  private final TarballSource tarballSource;

  @Inject
  public DefaultNpmProxyRepository(final @Named(NpmContentClass.ID) ContentClass contentClass,
                                   final NpmProxyRepositoryConfigurator configurator,
                                   final MetadataServiceFactory metadataServiceFactory,
                                   final TarballSource tarballSource)
  {
    this.proxyMetadataService = metadataServiceFactory.createProxyMetadataService(this);
    this.tarballSource = checkNotNull(tarballSource);
    this.contentClass = checkNotNull(contentClass);
    this.configurator = checkNotNull(configurator);

    this.repositoryKind = new DefaultRepositoryKind(NpmProxyRepository.class, null);
    this.mimeRulesSource = new NpmMimeRulesSource();
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onEvent(final RepositoryRegistryEventRemove event) {
    if (event.getRepository() == this) {
      getMetadataService().deleteAllMetadata();
    }
  }

  @Override
  public ProxyMetadataService getMetadataService() { return proxyMetadataService; }

  @Override
  protected Configurator getConfigurator() {
    return configurator;
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
  protected CRepositoryExternalConfigurationHolderFactory<NpmProxyRepositoryConfiguration> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<NpmProxyRepositoryConfiguration>()
    {
      @Override
      public NpmProxyRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new NpmProxyRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public MimeRulesSource getMimeRulesSource() {
    return mimeRulesSource;
  }

  @Override
  protected boolean doExpireProxyCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    // for Walker to operate, we must shut down the metadata service
    request.getRequestContext().put(NpmRepository.NPM_METADATA_NO_SERVICE, Boolean.TRUE);
    boolean result = super.doExpireProxyCaches(request, filter);
    try {
      boolean npmResult = proxyMetadataService.expireMetadataCaches(new PackageRequest(request));
      return result || npmResult;
    }
    catch (IllegalArgumentException ignore) {
      // ignore
      return result;
    }
  }

  // TODO: npm hosted and proxy repositories have similar retrieve code, factor it out or keep in sync
  @Override
  protected AbstractStorageItem doRetrieveLocalItem(ResourceStoreRequest storeRequest)
      throws ItemNotFoundException, LocalStorageException
  {
    try {
      if (!getMetadataService().isNpmMetadataServiced(storeRequest)) {
        // shut down NPM MD+tarball service completely
        return delegateDoRetrieveLocalItem(storeRequest);
      }
      PackageRequest packageRequest = null;
      try {
        packageRequest = new PackageRequest(storeRequest);
      }
      catch (IllegalArgumentException ignore) {
        // ignore, will see is this a tarball req or just do it standard way if needed
      }
      if (packageRequest != null) {
        packageRequest.getStoreRequest().getRequestContext().put(NpmRepository.NPM_METADATA_SERVICED, Boolean.TRUE);
        if (packageRequest.isMetadata()) {
          ContentLocator contentLocator;
          if (packageRequest.isRegistryRoot()) {
            log.debug("Serving registry root...");
            contentLocator = proxyMetadataService.produceRegistryRoot(packageRequest);
          }
          else if (packageRequest.isPackageRoot()) {
            log.debug("Serving package {} root...", packageRequest.getName());
            contentLocator = proxyMetadataService.producePackageRoot(packageRequest);
          }
          else {
            log.debug("Serving package {} version {}...", packageRequest.getName(), packageRequest.getVersion());
            contentLocator = proxyMetadataService.producePackageVersion(packageRequest);
          }
          if (contentLocator == null) {
            log.debug("No NPM metadata for path {}", storeRequest.getRequestPath());
            throw new ItemNotFoundException(
                reasonFor(storeRequest, this, "No content for path %s", storeRequest.getRequestPath()));
          }
          return createStorageFileItem(storeRequest, contentLocator);
        }
        else {
          // registry special
          if (packageRequest.isRegistrySpecial() && packageRequest.getPath().startsWith("/-/all")) {
            log.debug("Serving registry root from /-/all...");
            return createStorageFileItem(storeRequest,
                proxyMetadataService.produceRegistryRoot(packageRequest));
          }
          log.debug("Unknown registry special {}", packageRequest.getPath());
          throw new ItemNotFoundException(
              reasonFor(storeRequest, this, "No content for path %s", storeRequest.getRequestPath()));
        }
      }
      // this must be tarball, check it out do we have it locally, and if yes, and metadata checksum matches, give it
      final TarballRequest tarballRequest = getMetadataService().createTarballRequest(storeRequest);
      if (tarballRequest != null) {
        // stash it into context if needed to get from remote
        storeRequest.getRequestContext().put(TARBALL_REQUEST, tarballRequest);
        try {
          final AbstractStorageItem item = delegateDoRetrieveLocalItem(storeRequest);
          // TODO: explanation: NPM metadata contains SHA1, and we do maintain metadata. Hence, if SHA1 in metadata
          // matches with item's SHA1, whatever item we have, we know it's the "real thing".
          if (item instanceof StorageFileItem) {
            final PackageVersion version = tarballRequest.getPackageVersion();
            // if metadata contains sha1 and it equals to items sha1, we have it
            if (!Strings.isNullOrEmpty(version.getDistShasum())
                && version.getDistShasum()
                .equals(item.getRepositoryItemAttributes().get(PackageVersion.TARBALL_NX_SHASUM))) {
              // we have it and is up to date (hash matches metadata)
              item.setRemoteChecked(Long.MAX_VALUE);
              item.setExpired(false);
              return item;
            }
          }
        }
        catch (ItemNotFoundException e) {
          // no problem, just continue then
        }
      }
      throw new ItemNotFoundException(
          reasonFor(storeRequest, this, "No local content for path %s", storeRequest.getRequestPath()));
    }
    catch (IOException e) {
      throw new LocalStorageException("Metadata service error", e);
    }
  }

  /**
   * Prepares a file item. The "catch" is that this is proxy repository, and we don't want to have NX core aging
   * interfere with proxying of NPM metadata service, so whatever we have here we mark as "fresh" to not have
   * proxy logic of core kick in redoing all the generation again. To core, this file item looks like coming
   * from local store (cache), hence "aging" will be applied.
   */
  @VisibleForTesting
  DefaultStorageFileItem createStorageFileItem(final ResourceStoreRequest storeRequest,
                                               final ContentLocator contentLocator)
  {
    final DefaultStorageFileItem result = new DefaultStorageFileItem(this, storeRequest, true, true, contentLocator);
    result.setRemoteChecked(Long.MAX_VALUE); // do not handle it as expired at any cost
    result.setExpired(false); // do not handle it as expired at any cost
    result.getItemContext().put(NpmRepository.NPM_METADATA_SERVICED, Boolean.TRUE); // mark item as NPM md serviced
    return result;
  }

  /**
   * Beside original behaviour, only try remote the non-metadata requests.
   */
  @Override
  protected void shouldTryRemote(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException
  {
    super.shouldTryRemote(request);
    if (request.getRequestContext().containsKey(NpmRepository.NPM_METADATA_SERVICED)) {
      throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
          "Request is serviced by NPM metadata service, remote access not needed from %s", this));
    }
  }

  /**
   * Beside original behavior, only add to NFC non-metadata requests.
   */
  @Override
  protected boolean shouldAddToNotFoundCache(final ResourceStoreRequest request) {
    boolean shouldAddToNFC = super.shouldAddToNotFoundCache(request);
    if (shouldAddToNFC) {
      return !request.getRequestContext().containsKey(NpmRepository.NPM_METADATA_SERVICED);
    }
    return shouldAddToNFC;
  }

  @Override
  public AbstractStorageItem doCacheItem(AbstractStorageItem item) throws LocalStorageException {
    NpmBlob tarball = null;
    if (item instanceof StorageFileItem && ((StorageFileItem) item).getContentLocator() instanceof NpmBlob) {
      tarball = (NpmBlob) ((StorageFileItem) item).getContentLocator();
    }
    try {
      ResourceStoreRequest storeRequest = item.getResourceStoreRequest();
      PackageRequest packageRequest = new PackageRequest(storeRequest);
      log.info("NPM cache {}", packageRequest.getPath());
      if (packageRequest.isMetadata()) {
        // no cache, is done by MetadataService (should not get here at all)
        return item;
      }
      else {
        return delegateDoCacheItem(item);
      }
    }
    catch (IllegalArgumentException ignore) {
      // do it old style
      return delegateDoCacheItem(item);
    }
    finally {
      if (tarball != null) {
        try {
          tarball.delete();
        }
        catch (IOException e) {
          // suppress it but warn
          log.warn("Cannot delete temporary file: " + tarball.getFile().getAbsolutePath(), e);
        }
      }
    }
  }

  AbstractStorageItem delegateDoCacheItem(AbstractStorageItem item) throws LocalStorageException {
    return super.doCacheItem(item);
  }

  AbstractStorageItem delegateDoRetrieveLocalItem(ResourceStoreRequest storeRequest)
      throws LocalStorageException, ItemNotFoundException
  {
    return super.doRetrieveLocalItem(storeRequest);
  }

  @Override
  protected AbstractStorageItem doRetrieveRemoteItem(final ResourceStoreRequest request)
      throws ItemNotFoundException, RemoteAccessException, StorageException
  {
    if (request.getRequestContext().containsKey(NpmRepository.NPM_METADATA_NO_SERVICE, false)) {
      // shut down NPM MD+tarball service completely
      throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
          "Request shut down NPM proxy for %s", this));
    }

    final RepositoryItemUid itemUid = createUid(request.getRequestPath());
    final RepositoryItemUidLock itemUidLock = itemUid.getLock();
    itemUidLock.lock(Action.create);
    try {
      // get the stashed already created tarball request for resource store request
      final TarballRequest tarballRequest = (TarballRequest) request.getRequestContext().get(TARBALL_REQUEST, false);
      if (tarballRequest != null) {
        final NpmBlob tarball = tarballSource.get(this, tarballRequest);
        if (tarball != null) {
          // update the packageRoot document with version's known sha1 sum (this one is calculated by NX, is not the one from metadata)
          tarballRequest.getPackageRoot().getProperties().put(
              PackageVersion.createShasumVersionKey(tarballRequest.getPackageVersion().getVersion()),
              tarball.getSha1sum());
          getMetadataService().consumeRawPackageRoot(tarballRequest.getPackageRoot());

          // cache and return tarball wrapped into item
          final DefaultStorageFileItem result = new DefaultStorageFileItem(this, request, true, true, tarball);
          result.getRepositoryItemAttributes().put(PackageVersion.TARBALL_NX_SHASUM, tarball.getSha1sum());
          return doCacheItem(result);
        }
        throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
            "Request cannot be serviced by NPM proxy %s: tarball for package %s version %s not found on remote", this,
            tarballRequest.getPackageVersion().getName(), tarballRequest.getPackageVersion().getVersion()));
      }
      throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
          "Request cannot be serviced by NPM proxy %s: tarball package not found", this));
    }
    catch (IOException e) {
      throw new RemoteStorageException("NPM service error", e);
    }
    finally {
      itemUidLock.unlock();
    }
  }

  // TODO: npm hosted and proxy repositories have same deleteItem code, factor it out or keep in sync
  @Override
  public void deleteItem(boolean fromTask, ResourceStoreRequest storeRequest)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    PackageRequest packageRequest = null;
    try {
      packageRequest = new PackageRequest(storeRequest);
    }
    catch (IllegalArgumentException e) {
      // something completely different
    }
    boolean supressItemNotFound = false;
    if (packageRequest != null) {
      if (packageRequest.isMetadata()) {
        if (packageRequest.isRegistryRoot()) {
          log.debug("Deleting registry root...");
          getMetadataService().deleteAllMetadata();
        }
        else if (packageRequest.isPackageRoot()) {
          log.debug("Deleting package {} root...", packageRequest.getName());
          supressItemNotFound = getMetadataService().deletePackage(packageRequest.getName());
        }
      }
    }
    try {
      super.deleteItem(fromTask, storeRequest);
    }
    catch (ItemNotFoundException e) {
      // this allows to delete package metadata only, where no tarballs were cached/deployed for some reason
      if (!supressItemNotFound) {
        throw e;
      }
    }
  }

  @Override
  protected boolean isOld(int maxAge, StorageItem item, boolean shouldCalculate) {
    if (item.getItemContext().containsKey(NpmRepository.NPM_METADATA_SERVICED)) {
      // this item is serviced by NPM metadata service, should NEVER be handled as expired
      return false;
    }
    return super.isOld(maxAge, item, shouldCalculate);
  }
}