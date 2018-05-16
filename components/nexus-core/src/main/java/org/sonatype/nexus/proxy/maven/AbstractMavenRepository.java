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
package org.sonatype.nexus.proxy.maven;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryEventRecreateMavenMetadata;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.EvictUnusedMavenItemsWalkerProcessor.EvictUnusedMavenItemsWalkerFilter;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.ProxyRequestFilter;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_HASH_EXPIRED;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_MD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.ATTR_REMOTE_SHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.SUFFIX_MD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.SUFFIX_SHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doRetrieveMD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doRetrieveSHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doStoreMD5;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.doStoreSHA1;
import static org.sonatype.nexus.proxy.maven.ChecksumContentValidator.newHashItem;

/**
 * The abstract (layout unaware) Maven Repository.
 *
 * @author cstamas
 */
public abstract class AbstractMavenRepository
    extends AbstractProxyRepository
    implements MavenRepository, MavenHostedRepository, MavenProxyRepository
{

  /**
   * Metadata manager.
   */
  private MetadataManager metadataManager;

  /**
   * The artifact packaging mapper.
   */
  private ArtifactPackagingMapper artifactPackagingMapper;

  private ProxyRequestFilter proxyRequestFilter;

  private MutableProxyRepositoryKind repositoryKind;

  private ArtifactStoreHelper artifactStoreHelper;

  /**
   * if download remote index flag changed, need special handling after save
   */
  private boolean downloadRemoteIndexEnabled = false;
  
  @Inject
  public void populateAbstractMavenRepository(final MetadataManager metadataManager,
      final ArtifactPackagingMapper artifactPackagingMapper, final ProxyRequestFilter proxyRequestFilter)
  {
    this.metadataManager = checkNotNull(metadataManager);
    this.artifactPackagingMapper = checkNotNull(artifactPackagingMapper);
    this.proxyRequestFilter = checkNotNull(proxyRequestFilter);
    this.repositoryKind = new MutableProxyRepositoryKind(this, Arrays.asList(new Class<?>[] { MavenRepository.class }),
        new DefaultRepositoryKind(MavenHostedRepository.class, null), new DefaultRepositoryKind(
            MavenProxyRepository.class, null));
    this.artifactStoreHelper = new ArtifactStoreHelper(this);
  }

  @Override
  protected AbstractMavenRepositoryConfiguration getExternalConfiguration(boolean forModification) {
    return (AbstractMavenRepositoryConfiguration) super.getExternalConfiguration(forModification);
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean result = super.commitChanges();

    if (result) {
      this.downloadRemoteIndexEnabled = false;
    }

    return result;
  }

  @Override
  public boolean rollbackChanges() {
    this.downloadRemoteIndexEnabled = false;

    return super.rollbackChanges();
  }

  @Override
  protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent() {
    RepositoryConfigurationUpdatedEvent event = super.getRepositoryConfigurationUpdatedEvent();

    event.setDownloadRemoteIndexEnabled(this.downloadRemoteIndexEnabled);

    return event;
  }

  @Override
  public ArtifactStoreHelper getArtifactStoreHelper() {
    return artifactStoreHelper;
  }

  @Override
  public ArtifactPackagingMapper getArtifactPackagingMapper() {
    return artifactPackagingMapper;
  }

  protected ProxyRequestFilter getProxyRequestFilter() {
    return proxyRequestFilter;
  }

  /**
   * Override the "default" kind with Maven specifics.
   */
  @Override
  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  protected Collection<String> doEvictUnusedItems(final ResourceStoreRequest request, final long timestamp) {
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      Collection<String> result =
          doEvictUnusedItems(request, timestamp, new EvictUnusedMavenItemsWalkerProcessor(timestamp),
              new EvictUnusedMavenItemsWalkerFilter());
      eventBus().post(new RepositoryEventEvictUnusedItems(this));
      return result;
    }
    else {
      return super.doEvictUnusedItems(request, timestamp);
    }
  }

  @Override
  public boolean recreateMavenMetadata(final ResourceStoreRequest request) {
    if (!shouldServiceOperation(request, "recreateMavenMetadata")) {
      return false;
    }
    if (!getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
      log.debug("Not performing recreateMavenMetadata, {} is not hosted.", this);
      return false;
    }

    if (StringUtils.isEmpty(request.getRequestPath())) {
      request.setRequestPath(RepositoryItemUid.PATH_ROOT);
    }

    try {
      if (!getLocalStorage().containsItem(this, request)) {
        log.info(
            "Skip rebuilding Maven2 Metadata in repository {} because it does not contain path='{}'.", this,
            request.getRequestPath());
        return false;
      }
    }
    catch (LocalStorageException e) {
      log.warn("Skip rebuilding Maven2 Metadata in repository {}", this, e);
      return false;
    }
    return doRecreateMavenMetadata(request);
  }

  protected boolean doRecreateMavenMetadata(final ResourceStoreRequest request) {
    log.info("Recreating Maven2 metadata in hosted repository {} from path='{}'", this, request.getRequestPath());
    final RecreateMavenMetadataWalkerProcessor wp = new RecreateMavenMetadataWalkerProcessor(log);
    final DefaultWalkerContext ctx = new DefaultWalkerContext(this, request);
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
    eventBus().post(new RepositoryEventRecreateMavenMetadata(this));
    return !ctx.isStopped();
  }

  @Override
  public boolean isDownloadRemoteIndexes() {
    return getExternalConfiguration(false).isDownloadRemoteIndex();
  }

  @Override
  public void setDownloadRemoteIndexes(boolean downloadRemoteIndexes) {
    boolean oldValue = isDownloadRemoteIndexes();
    boolean newValue = downloadRemoteIndexes;

    getExternalConfiguration(true).setDownloadRemoteIndex(downloadRemoteIndexes);

    if (oldValue == false && newValue == true) {
      this.downloadRemoteIndexEnabled = true;
    }
  }

  @Override
  public RepositoryPolicy getRepositoryPolicy() {
    return getExternalConfiguration(false).getRepositoryPolicy();
  }

  @Override
  public void setRepositoryPolicy(RepositoryPolicy repositoryPolicy) {
    getExternalConfiguration(true).setRepositoryPolicy(repositoryPolicy);
  }

  @Override
  public boolean isCleanseRepositoryMetadata() {
    return getExternalConfiguration(false).isCleanseRepositoryMetadata();
  }

  @Override
  public void setCleanseRepositoryMetadata(boolean cleanseRepositoryMetadata) {
    getExternalConfiguration(true).setCleanseRepositoryMetadata(cleanseRepositoryMetadata);
  }

  public ChecksumPolicy getChecksumPolicy() {
    return getExternalConfiguration(false).getChecksumPolicy();
  }

  public void setChecksumPolicy(ChecksumPolicy checksumPolicy) {
    getExternalConfiguration(true).setChecksumPolicy(checksumPolicy);
  }

  @Override
  public boolean isMavenArtifact(StorageItem item) {
    return isMavenArtifactPath(item.getPath());
  }

  @Override
  public boolean isMavenMetadata(StorageItem item) {
    return isMavenMetadataPath(item.getPath());
  }

  @Override
  public boolean isMavenArtifactPath(String path) {
    return getGavCalculator().pathToGav(path) != null;
  }

  @Override
  public abstract boolean isMavenMetadataPath(String path);

  public abstract boolean isMavenArtifactChecksumPath(String path);

  public abstract boolean shouldServeByPolicies(ResourceStoreRequest request);

  @Override
  public void storeItemWithChecksums(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    if (log.isDebugEnabled()) {
      log.debug("storeItemWithChecksums() :: " + request.getRequestPath());
    }

    getArtifactStoreHelper().storeItemWithChecksums(request, is, userAttributes);
  }

  @Override
  public void deleteItemWithChecksums(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
             StorageException, AccessDeniedException
  {
    if (log.isDebugEnabled()) {
      log.debug("deleteItemWithChecksums() :: " + request.getRequestPath());
    }

    getArtifactStoreHelper().deleteItemWithChecksums(request);
  }

  @Override
  public void storeItemWithChecksums(boolean fromTask, AbstractStorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("storeItemWithChecksums() :: " + item.getRepositoryItemUid().toString());
    }

    getArtifactStoreHelper().storeItemWithChecksums(fromTask, item);
  }

  @Override
  public void deleteItemWithChecksums(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("deleteItemWithChecksums() :: " + request.toString());
    }

    getArtifactStoreHelper().deleteItemWithChecksums(fromTask, request);
  }

  @Override
  public MetadataManager getMetadataManager() {
    return metadataManager;
  }

  // =================================================================================
  // DefaultRepository customizations
  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (!shouldServeByPolicies(request)) {
      if (log.isDebugEnabled()) {
        log.debug(
            "The serving of item " + request.toString() + " is forbidden by Maven repository policy.");
      }

      throw new ItemNotFoundException(reasonFor(request, this,
          "Retrieval of %s from %s is forbidden by repository policy %s.", request.getRequestPath(),
          this, getRepositoryPolicy()));
    }

    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)
        && !request.getRequestPath().startsWith("/.")) {
      if (request.getRequestPath().endsWith(SUFFIX_SHA1)) {
        return doRetrieveSHA1(this, request, doRetrieveArtifactItem(request, SUFFIX_SHA1)).getHashItem();
      }

      if (request.getRequestPath().endsWith(SUFFIX_MD5)) {
        return doRetrieveMD5(this, request, doRetrieveArtifactItem(request, SUFFIX_MD5)).getHashItem();
      }
    }

    return super.doRetrieveItem(request);
  }

  /**
   * Retrieves artifact corresponding to .sha1/.md5 request (or any request suffix).
   */
  private StorageItem doRetrieveArtifactItem(ResourceStoreRequest hashRequest, String suffix)
      throws ItemNotFoundException, StorageException, IllegalOperationException
  {
    final String hashPath = hashRequest.getRequestPath();
    final String itemPath = hashPath.substring(0, hashPath.length() - suffix.length());
    hashRequest.pushRequestPath(itemPath);
    try {
      return super.doRetrieveItem(hashRequest);
    }
    finally {
      hashRequest.popRequestPath();
    }
  }

  @Override
  protected void shouldTryRemote(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException
  {
    // do super first
    super.shouldTryRemote(request);
    // if here, super did not throw any exception, so let's continue
    // apply autorouting filter to "normal" requests only, not hidden (which is meta or plain hidden)
    final RepositoryItemUid uid = createUid(request.getRequestPath());
    if (!uid.getBooleanAttributeValue(IsHiddenAttribute.class)) {
      // but filter it only if request is not marked as NFS
      if (!request.getRequestContext().containsKey(Manager.ROUTING_REQUEST_NFS_FLAG_KEY)) {
        final boolean proxyFilterAllowed = getProxyRequestFilter().allowed(this, request);
        if (!proxyFilterAllowed) {
          throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
              "Automatic routing filter rejected remote request for path %s from %s", request.getRequestPath(),
              this));
        }
      }
    }
  }

  @Override
  public void storeItem(boolean fromTask, StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(item); // this is local only request
    if (shouldServeByPolicies(request)) {
      if (getRepositoryKind().isFacetAvailable(ProxyRepository.class) && item instanceof StorageFileItem
          && !item.getPath().startsWith("/.")) {
        try {
          if (item.getPath().endsWith(SUFFIX_SHA1)) {
            doStoreSHA1(this, doRetrieveArtifactItem(request, SUFFIX_SHA1), (StorageFileItem) item);
          }
          else if (item.getPath().endsWith(SUFFIX_MD5)) {
            doStoreMD5(this, doRetrieveArtifactItem(request, SUFFIX_MD5), (StorageFileItem) item);
          }
          else {
            super.storeItem(fromTask, item);
          }
        }
        catch (ItemNotFoundException e) {
          // ignore storeItem request
          // this is a maven2 proxy repository, it is requested to store .sha1/.md5 file
          // and not there is not corresponding artifact
        }
      }
      else {
        super.storeItem(fromTask, item);
      }
    }
    else {
      String msg =
          "Storing of item " + item.getRepositoryItemUid().toString()
              + " is forbidden by Maven Repository policy. Because " + getId() + " is a "
              + getRepositoryPolicy().name() + " repository";

      log.info(msg);

      throw new UnsupportedStorageOperationException(msg);
    }
  }

  @Override
  public AbstractStorageItem doCacheItem(AbstractStorageItem item)
      throws LocalStorageException
  {
    final AbstractStorageItem result = super.doCacheItem(item);
    result.getRepositoryItemAttributes().remove(ATTR_REMOTE_SHA1);
    result.getRepositoryItemAttributes().remove(ATTR_REMOTE_MD5);
    return result;
  }

  @Override
  protected boolean doExpireProxyCaches(ResourceStoreRequest request, WalkerFilter filter) {

    // special handling if this is a remote checksum request
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)
        && request.getRequestPath() != null && !request.getRequestPath().startsWith("/.")) {

      if (request.getRequestPath().endsWith(SUFFIX_SHA1)) {
        expireRemoteHash(request, SUFFIX_SHA1);
      }
      else if (request.getRequestPath().endsWith(SUFFIX_MD5)) {
        expireRemoteHash(request, SUFFIX_MD5);
      }
    }

    return super.doExpireProxyCaches(request, filter);
  }

  private void expireRemoteHash(ResourceStoreRequest hashRequest, String suffix) {
    final String hashPath = hashRequest.getRequestPath();
    final String itemPath = hashPath.substring(0, hashPath.length() - suffix.length());
    hashRequest.pushRequestPath(itemPath);
    try {
      StorageItem artifact = getLocalStorage().retrieveItem(this, hashRequest);
      Attributes attributes = artifact.getRepositoryItemAttributes();
      if (attributes.containsKey(ATTR_REMOTE_SHA1) || attributes.containsKey(ATTR_REMOTE_MD5)) {
        attributes.put(ATTR_REMOTE_HASH_EXPIRED, "true");
        getAttributesHandler().storeAttributes(artifact);
      }
    }
    catch (Exception e) {
      log.debug("Skip expiring remote hash in repository {} because it does not contain path='{}'.", this, itemPath);
    }
    finally {
      hashRequest.popRequestPath();
    }
  }

  @Override
  public boolean isCompatible(Repository repository) {
    if (super.isCompatible(repository) && MavenRepository.class.isAssignableFrom(repository.getClass())
        && getRepositoryPolicy().equals(((MavenRepository) repository).getRepositoryPolicy())) {
      return true;
    }

    return false;
  }

  // =================================================================================
  // DefaultRepository customizations

  @Override
  protected Collection<StorageItem> doListItems(ResourceStoreRequest request)
      throws ItemNotFoundException, StorageException
  {
    Collection<StorageItem> items = super.doListItems(request);
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      Map<String, StorageItem> result = new TreeMap<String, StorageItem>();
      for (StorageItem item : items) {
        putChecksumItem(result, request, item, ATTR_REMOTE_SHA1, SUFFIX_SHA1);
        putChecksumItem(result, request, item, ATTR_REMOTE_MD5, SUFFIX_MD5);
      }

      for (StorageItem item : items) {
        if (!result.containsKey(item.getPath())) {
          result.put(item.getPath(), item);
        }
      }

      items = result.values();
    }
    return items;
  }

  private void putChecksumItem(Map<String, StorageItem> checksums, ResourceStoreRequest request,
                               StorageItem artifact, String attrname, String suffix)
  {
    String hash = artifact.getRepositoryItemAttributes().get(attrname);
    if (hash != null) {
      String hashPath = artifact.getPath() + suffix;
      request.pushRequestPath(hashPath);
      try {
        checksums.put(hashPath, newHashItem(this, request, artifact, hash));
      }
      finally {
        request.popRequestPath();
      }
    }
  }

  /**
   * Beside original behavior, only add to NFC when remote access is not rejected by autorouting.
   *
   * @since 2.4
   */
  @Override
  protected boolean shouldAddToNotFoundCache(final ResourceStoreRequest request) {
    boolean shouldAddToNFC = super.shouldAddToNotFoundCache(request);
    if (shouldAddToNFC && request.getRequestContext().containsKey(Manager.ROUTING_REQUEST_REJECTED_FLAG_KEY)) {
      request.getRequestContext().remove(Manager.ROUTING_REQUEST_REJECTED_FLAG_KEY);
      shouldAddToNFC = false;
      log.debug("Maven proxy repository {} autorouting rejected this request, not adding path {} to NFC.",
          RepositoryStringUtils.getHumanizedNameString(this), request.getRequestPath());
    }
    return shouldAddToNFC;
  }

  @Override
  protected boolean shouldNeglectItemNotFoundExOnDelete(ResourceStoreRequest request, ItemNotFoundException ex) {
    return isMavenArtifactChecksumPath(request.getRequestPath());
  }

  /**
   * Deletes item and regenerates Maven metadata, if repository is a hosted repository and maven-metadata.xml file is
   * present.
   *
   * @since 2.5
   */
  @Override
  protected void doDeleteItem(final ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, ItemNotFoundException, StorageException
  {
    try {
      super.doDeleteItem(request);
    }
    catch (ItemNotFoundException ex) {
      // NEXUS-5773, NEXUS-5418
      // Since Nx 2.5, checksum are not stored on disk
      // but are stored as attributes. Still, upgraded systems
      // might have them on disk, so delete is attempted
      // but INFex on Checksum file in general can be neglected here.
      if (!shouldNeglectItemNotFoundExOnDelete(request, ex)) {
        throw ex;
      }
    }
    // regenerate maven metadata for parent of this item if is a hosted maven repo and it contains maven-metadata.xml
    if (request.isExternal() && getRepositoryKind().isFacetAvailable(MavenHostedRepository.class)) {
      String parentPath = request.getRequestPath();
      parentPath = parentPath.substring(0, parentPath.lastIndexOf(RepositoryItemUid.PATH_SEPARATOR));
      final String parentMetadataPath = parentPath + "/maven-metadata.xml";
      try {
        if (getLocalStorage().containsItem(this, new ResourceStoreRequest(parentMetadataPath))) {
          recreateMavenMetadata(new ResourceStoreRequest(parentPath));
        }
      }
      catch (Exception e) {
        log.warn("Could not maintain Maven metadata '{}'", parentMetadataPath, e);
      }
    }
  }

}
