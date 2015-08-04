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
package org.sonatype.nexus.obr.proxy;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.obr.ObrContentClass;
import org.sonatype.nexus.obr.ObrPluginConfiguration;
import org.sonatype.nexus.obr.metadata.ManagedObrSite;
import org.sonatype.nexus.obr.metadata.ObrMetadataSource;
import org.sonatype.nexus.obr.metadata.ObrResourceReader;
import org.sonatype.nexus.obr.metadata.ObrResourceWriter;
import org.sonatype.nexus.obr.util.ObrUtils;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;
import org.osgi.service.obr.Resource;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(ObrRepository.ROLE_HINT)
@Description("OBR")
public class ObrRepository
    extends AbstractProxyRepository
    implements ObrProxyRepository, ObrHostedRepository, Repository
{
  public static final String ROLE_HINT = "obr-proxy";

  private final ContentClass obrContentClass;

  private final ObrRepositoryConfigurator obrRepositoryConfigurator;

  private final ObrPluginConfiguration obrConfiguration;

  private final ObrMetadataSource obrMetadataSource;

  private final RepositoryKind obrRepositoryKind =
      new MutableProxyRepositoryKind(this, null, new DefaultRepositoryKind(ObrHostedRepository.class, null),
          new DefaultRepositoryKind(ObrProxyRepository.class, null));

  @Inject
  public ObrRepository(final @Named(ObrContentClass.ID) ContentClass obrContentClass,
                       final ObrRepositoryConfigurator obrRepositoryConfigurator,
                       final ObrPluginConfiguration obrConfiguration,
                       final @Named("obr-bindex") ObrMetadataSource obrMetadataSource)
  {
    this.obrContentClass = checkNotNull(obrContentClass);
    this.obrRepositoryConfigurator = checkNotNull(obrRepositoryConfigurator);
    this.obrConfiguration = checkNotNull(obrConfiguration);
    this.obrMetadataSource = checkNotNull(obrMetadataSource);
  }

  public ContentClass getRepositoryContentClass() {
    return obrContentClass;
  }

  @Override
  protected Configurator getConfigurator() {
    return obrRepositoryConfigurator;
  }

  public RepositoryKind getRepositoryKind() {
    return obrRepositoryKind;
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<ObrRepositoryConfiguration>()
    {
      public ObrRepositoryConfiguration createExternalConfigurationHolder(final CRepository config) {
        return new ObrRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  protected ObrRepositoryConfiguration getExternalConfiguration(final boolean forModification) {
    return (ObrRepositoryConfiguration) super.getExternalConfiguration(forModification);
  }

  @Override
  public void setLocalUrl(final String localUrl)
      throws LocalStorageException
  {
    super.setLocalUrl(localUrl);
  }

  @Override
  public void setRemoteUrl(final String remoteUrl)
      throws RemoteStorageException
  {
    final String[] siteAndPath = ObrUtils.splitObrSiteAndPath(remoteUrl, false);

    // does the remoteUrl have obrPath at the end?
    if (siteAndPath[1] == null) {
      // no, then require the obrPath to be set already
      if (!getExternalConfiguration(false).isObrPathSet()) {
        // it is not set, this is an error
        throw new RemoteStorageException(
            "Cannot set OBR URL! The OBR metadata path is not set, please specify a full URL including the OBR metadata file!");
      }
    }
    else {
      // yes, set it
      setObrPath(siteAndPath[1]);
    }

    // all is fine
    super.setRemoteUrl(siteAndPath[0]);
  }

  public String getObrPath() {
    return getExternalConfiguration(false).getObrPath();
  }

  public void setObrPath(final String obrPath) {
    getExternalConfiguration(true).setObrPath(obrPath);
  }

  public int getMetadataMaxAge() {
    return getExternalConfiguration(false).getMetadataMaxAge();
  }

  public void setMetadataMaxAge(final int metadataMaxAge) {
    getExternalConfiguration(true).setMetadataMaxAge(metadataMaxAge);
  }

  @Override
  public Collection<StorageItem> list(final boolean fromTask, final StorageCollectionItem item)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    return ObrUtils.augmentListedItems(item.getRepositoryItemUid(), super.list(fromTask, item));
  }

  @Override
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (getRemoteUrl() != null) {
      // may delegate to doRetrieveRemoteItem...
      return super.doRetrieveItem(request);
    }

    try {
      // treat expired items just like not found items
      final StorageItem item = super.doRetrieveItem(request);

      if (!item.isExpired()) {
        return item;
      }
    }
    catch (final ItemNotFoundException e) {
      // drop through...
    }

    if (ObrUtils.isObrMetadataRequest(request)) {
      final StorageItem backingItem = ObrUtils.getCachedItem(createUid(getObrPath()));

      if (null == backingItem) {
        ObrUtils.buildObr(obrMetadataSource, ObrUtils.createObrUid(this), this, getWalker());
      }
      else {
        // copy over the original OBR
        return refreshObr(backingItem);
      }
    }
    else if (request.getRequestPath().startsWith(CacheableResource.BUNDLES_PATH)) {
      final StorageItem cachedItem = doCacheBundle(request);

      if (null != cachedItem) {
        return cachedItem;
      }
    }

    return super.doRetrieveItem(request);
  }

  @Override
  protected AbstractStorageItem doRetrieveRemoteItem(final ResourceStoreRequest request)
      throws ItemNotFoundException, RemoteAccessException, StorageException
  {
    if (ObrUtils.isObrMetadataRequest(request)) {
      final ResourceStoreRequest metadataRequest = new ResourceStoreRequest(getObrPath());

      return refreshObr(getRemoteStorage().retrieveItem(this, metadataRequest, getRemoteUrl()));
    }
    else if (request.getRequestPath().startsWith(CacheableResource.BUNDLES_PATH)) {
      final AbstractStorageItem cachedItem = doCacheBundle(request);

      if (null != cachedItem) {
        return cachedItem;
      }
    }

    return super.doRetrieveRemoteItem(request);
  }

  /**
   * Attempts to cache the contents of a bundle referred to by an absolute URL.
   *
   * @param request the resource request
   * @return the cached bundle
   */
  private AbstractStorageItem doCacheBundle(final ResourceStoreRequest request)
      throws StorageException
  {
    try {
      // attempt to map path to the absolute URL
      final String url = getAbsoluteBundleUrl(request.getRequestPath());
      if (url != null) {
        // in order to properly use remote storage here, we need 2 things
        // a baseUrl and the relative url. The baseUrl is passed in as string
        // param, the relativeUrl is pulled from the ResourceStoreRequest.
        // so in simplest terms, i am taking the full absoluteUrl, chopping off
        // everything except the filename to get teh baseUrl, and using that
        // then updating the ResourceStoreRequest to use only the filename for relativePath
        final String baseUrl = url.substring(0, url.lastIndexOf("/"));
        final String fileName = url.substring(url.lastIndexOf("/"));

        request.pushRequestPath(fileName);
        final AbstractStorageItem item;
        try {
          item = getRemoteStorage().retrieveItem(this, request, baseUrl);
        }
        finally {
          request.popRequestPath();
        }
        // since NEXUS-5511, the line above does not modify the request carried by item!
        // Those two are detached
        item.getResourceStoreRequest().setRequestPath(request.getRequestPath());

        // update the repositoryItemUid, as it will contain the request path with only the filename
        item.setRepositoryItemUid(createUid(request.getRequestPath()));

        return doCacheItem(item);
      }

      return null; // not found, this might be a real path in the original OBR
    }
    catch (final IOException e) {
      throw new StorageException(e);
    }
    catch (final ItemNotFoundException e) {
      throw new StorageException(e);
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onEvent(final RepositoryItemEventStore evt) {
    updateObr(evt, true);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onEvent(final RepositoryItemEventDelete evt) {
    updateObr(evt, false);
  }

  private void updateObr(final RepositoryItemEvent evt, final boolean adding) {
    if (this.equals(evt.getRepository())) {
      try {
        final Resource resource = obrMetadataSource.buildResource(
            ObrUtils.getCachedItem(evt.getItemUid())
        );
        if (resource != null) {
          ObrUtils.updateObr(obrMetadataSource, ObrUtils.createObrUid(this), resource, adding);
        }
      }
      catch (final Exception e) {
        log.warn("Problem updating OBR " + getId(), e);
      }
    }
  }

  /**
   * Refreshes the locally cached OBR metadata if it is older than the proxied OBR metadata.
   *
   * @param backingItem the proxied metadata item
   * @return the locally cached metadata item
   */
  private AbstractStorageItem refreshObr(final StorageItem backingItem)
      throws StorageException, ItemNotFoundException
  {
    final RepositoryItemUid obrUid = ObrUtils.createObrUid(this);
    StorageItem obrItem = ObrUtils.getCachedItem(obrUid);

    if (null == obrItem || obrItem.getModified() < backingItem.getModified()) {
      final boolean caching = obrConfiguration.isBundleCacheActive();

      ObrResourceReader reader = null;
      ObrResourceWriter writer = null;

      try {
        reader = obrMetadataSource.getReader(new ManagedObrSite((StorageFileItem) backingItem));
        writer = obrMetadataSource.getWriter(obrUid);

        for (Resource i = reader.readResource(); i != null; i = reader.readResource()) {
          if (caching && !"file".equals(i.getURL().getProtocol())) {
            writer.append(new CacheableResource(i));
          }
          else {
            writer.append(i);
          }
        }

        writer.complete(); // the OBR is only updated once the stream is complete and closed
      }
      catch (final IOException e) {
        throw new StorageException(e);
      }
      finally {
        // avoid file locks by closing reader first
        IOUtils.closeQuietly(reader);
        IOUtils.closeQuietly(writer);
      }

      obrItem = ObrUtils.getCachedItem(obrUid);
      if (null == obrItem) {
        // this shouldn't happen as we just saved it, but just in case...
        throw new StorageException("Problem reading OBR metadata from repository " + getId());
      }
    }

    return (AbstractStorageItem) obrItem;
  }

  /**
   * Scan through the OBR and attempt to match the cached location with a {@link Resource} entry.
   *
   * @param path cached bundle location
   * @return the remote bundle URL
   */
  private String getAbsoluteBundleUrl(final String path) {
    ObrResourceReader reader = null;

    try {
      reader = obrMetadataSource.getReader(new ManagedObrSite(ObrUtils.retrieveObrItem(this)));
      for (Resource i = reader.readResource(); i != null; i = reader.readResource()) {
        // the right entry should have the same path
        if (path.equals(i.getURL().getPath())) {
          return StringUtils.defaultString(i.getProperties().get(CacheableResource.REMOTE_URL), null);
        }
      }
    }
    catch (final IOException e) {
      log.warn("Problem reading OBR metadata from repository " + getId(), e);
    }
    finally {
      IOUtils.closeQuietly(reader);
    }

    return null; // no match found!
  }

  @Override
  protected boolean isOld(final StorageItem item) {
    if (ObrUtils.isObrMetadataRequest(new ResourceStoreRequest(item))) {
      return isOld(getMetadataMaxAge(), item);
    }

    return super.isOld(item);
  }

  @Override
  protected boolean isRemoteStorageReachable(final ResourceStoreRequest request) {
    log.debug("isRemoteStorageReachable: RepositoryId=" + getId() + ": Trying to access " + getObrPath());
    request.setRequestPath(getObrPath());
    try {
      // We cannot use getRemoteStorage().isReachable() here because that forces the request path to be "/"
      if (getRemoteStorage().containsItem(this, request)) {
        log.debug("isRemoteStorageReachable: RepositoryId=" + getId() + ": Successfully accessed "
            + getObrPath());
        return true;
      }
    }
    catch (final Exception e) {
      log.debug("isRemoteStorageReachable: RepositoryId=" + getId()
          + ": Caught exception while trying to access " + getObrPath(), e);
    }

    return false;
  }
}
