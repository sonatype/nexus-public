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
package org.sonatype.nexus.proxy.repository;

import java.util.Collection;
import java.util.Map;

import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.cache.PathCache;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.mirror.PublishedMirrors;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.targets.TargetSet;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

/**
 * Repository interface used by Proximity. It is an extension of ResourceStore iface, allowing to make direct
 * RepositoryItemUid based requests which bypasses AccessManager. Also, defines some properties.
 *
 * @author cstamas
 */
@RepositoryType(pathPrefix = "repositories")
public interface Repository
    extends ResourceStore
{
  /**
   * Disposes this repository, frees it's resources and unhooks it, allowing the instance to be GCed. Typically when a
   * repository is removed from Nx instance. "User" code should never invoke this method, as it's typically handled
   * by Nexus core.
   * 
   * @since 2.7.0
   */
  void dispose();

  /**
   * Returns the repository's "provider" role. These are getters only, and application is NOT able to change these
   * values runtime! Note: this is a FQN of a class, that is used to "register" the component with container, and the
   * class might reside in core but also in a plugin (separate child classloader!).
   */
  String getProviderRole();

  /**
   * Returns the repository's "provider" hint. These are getters only, and application is NOT able to change these
   * values runtime!
   */
  String getProviderHint();

  /**
   * Returns the ID of the resourceStore.
   *
   * @return the id
   */
  String getId();

  /**
   * Sets the ID of the resourceStore. It must be unique type-wide (Router vs Repository).
   *
   * @param id the ID of the repo.
   */
  void setId(String id);

  /**
   * Gets repository human name.
   */
  String getName();

  /**
   * Sets repository human name.
   */
  void setName(String name);

  /**
   * Used by router only, to specify a valid path prefix to a repository (previously was used getId() for this).
   */
  String getPathPrefix();

  /**
   * Used by router only, to specify a valid path prefix to a repository (previously was used getId() for this).
   */
  void setPathPrefix(String prefix);

  /**
   * This is the "type"/kind of the repository. It tells some minimal info about the repo working (not content,
   * neither implementation).
   */
  RepositoryKind getRepositoryKind();

  /**
   * This is the "class" of the repository content. It is used in grouping, only same content reposes may be grouped.
   */
  ContentClass getRepositoryContentClass();

  /**
   * Gets the target set for request.
   */
  TargetSet getTargetsForRequest(ResourceStoreRequest request);

  /**
   * Checks is there at all any target for the given request.
   */
  boolean hasAnyTargetsForRequest(ResourceStoreRequest request);

  /**
   * Creates an UID within this Repository.
   */
  RepositoryItemUid createUid(String path);

  /**
   * Returns the repository ItemUidAttributeManager.
   */
  RepositoryItemUidAttributeManager getRepositoryItemUidAttributeManager();

  /**
   * Will return the proper Action that will occur on "write" operation: create (if nothing exists on the given path)
   * or update (if overwrite will happen since the path already exists).
   *
   * @throws LocalStorageException when some storage (IO) problem happens.
   */
  Action getResultingActionOnWrite(ResourceStoreRequest rsr)
      throws LocalStorageException;

  /**
   * Is the target repository compatible to this one
   */
  boolean isCompatible(Repository repository);

  /**
   * Returns the facet of Repository, if available, otherwise it returns null.
   *
   * @return the facet requested, otherwise null.
   */
  <F> F adaptToFacet(Class<F> t);

  // ==================================================
  // NFC et al

  /**
   * Gets the not found cache time to live (in minutes).
   *
   * @return the not found cache time to live (in minutes)
   */
  int getNotFoundCacheTimeToLive();

  /**
   * Sets the not found cache time to live (in minutes).
   *
   * @param notFoundCacheTimeToLive the new not found cache time to live (in minutes).
   */
  void setNotFoundCacheTimeToLive(int notFoundCacheTimeToLive);

  /**
   * Gets the not found cache.
   *
   * @return the not found cache
   */
  PathCache getNotFoundCache();

  /**
   * Maintains NFC.
   */
  void maintainNotFoundCache(ResourceStoreRequest request)
      throws ItemNotFoundException;

  /**
   * Adds path to NFC.
   */
  void addToNotFoundCache(ResourceStoreRequest request);

  /**
   * Removes path from NFC.
   */
  void removeFromNotFoundCache(ResourceStoreRequest request);

  /**
   * Is NFC active? (true by default)
   */
  boolean isNotFoundCacheActive();

  /**
   * Sets is NFC active.
   */
  void setNotFoundCacheActive(boolean notFoundCacheActive);

  // ==================================================
  // LocalStorage et al

  /**
   * Returns the Repository specific MIME rules source.
   *
   * @since 2.0
   */
  MimeRulesSource getMimeRulesSource();

  /**
   * Returns the attribute handler used by repository.
   */
  AttributesHandler getAttributesHandler();

  /**
   * Returns the local URL of this repository, if any.
   *
   * @return local url of this repository, null otherwise.
   */
  String getLocalUrl();

  /**
   * Sets the local url.
   *
   * @param url the new local url
   */
  void setLocalUrl(String url)
      throws LocalStorageException;

  /**
   * Gets local status.
   */
  LocalStatus getLocalStatus();

  /**
   * Sets local status.
   *
   * @param val the val
   */
  void setLocalStatus(LocalStatus val);

  /**
   * Returns repository specific local storage context.
   */
  LocalStorageContext getLocalStorageContext();

  /**
   * Returns the local storage of the repository. Per repository instance may exists.
   */
  LocalRepositoryStorage getLocalStorage();

  /**
   * Sets the local storage of the repository. May be null if this is an aggregating repos without caching function.
   * Per repository instance may exists.
   *
   * @param storage the storage
   */
  void setLocalStorage(LocalRepositoryStorage storage);

  /**
   * Gets the published mirrors.
   */
  PublishedMirrors getPublishedMirrors();

  // ==================================================
  // Behaviour

  /**
   * Registers a {@link RequestStrategy} with this repository.
   *
   * @param key      must not be {@code null}.
   * @param strategy must not be {@code null}.
   * @return the strategy that was already registered under this key (might be same instance if method invoked with
   *         same instance!), or {@code null} if there was no strategy registered under this key. In short, the
   *         replaced strategy or {@code null}.
   * @since 2.5
   */
  RequestStrategy registerRequestStrategy(String key, RequestStrategy strategy);

  /**
   * Unregisters a {@link RequestStrategy} from this repository.
   *
   * @param key must not be {@code null}.
   * @return the strategy that was registered under this key, or {@code null} if there was no strategy registered
   *         under this key. In short, the unregistered strategy or {@code null}.
   * @since 2.5
   */
  RequestStrategy unregisterRequestStrategy(String key);

  /**
   * Returns a detached (a copy made in the moment of invocation of this method) map containing all the registered
   * {@link RequestStrategy}. Modifications to returned map are possible, but does not affect the originating
   * repository (this repository).
   *
   * @return map of registered {@link RequestStrategy}.
   * @since 2.5
   */
  Map<String, RequestStrategy> getRegisteredStrategies();

  /**
   * If is user managed, the nexus core and nexus core UI handles the store. Thus, for reposes, users are allowed to
   * edit/drop the repository.
   */
  boolean isUserManaged();

  /**
   * Sets is the store user managed.
   */
  void setUserManaged(boolean val);

  /**
   * Tells whether the resource store is exposed as Nexus content or not.
   */
  boolean isExposed();

  /**
   * Sets the exposed flag.
   */
  void setExposed(boolean val);

  /**
   * Is Repository listable?.
   *
   * @return true if is listable, otherwise false.
   */
  boolean isBrowseable();

  /**
   * Sets the listable property of repository. If true, its content will be returned by listItems method, otherwise
   * not. The retrieveItem will still function and return the requested item.
   *
   * @param val the val
   */
  void setBrowseable(boolean val);

  /**
   * Specifies if the repo is write, readonly, or single deploy.
   *
   * @return the write policy for this repository.
   */
  RepositoryWritePolicy getWritePolicy();

  /**
   * Sets the write policy for the repo. See {@link RepositoryWritePolicy}. This does not affect the in-repository
   * caching using LocalStorage. It just says, that from the "outer" perspective, this repo behaves like read-only
   * repository, and deployment is disabled for example.
   *
   * @param val the val
   */
  void setWritePolicy(RepositoryWritePolicy writePolicy);

  /**
   * Is Repository indexable? If {@code true}, indexing context (or whatever indexer technology is used if the
   * future)
   * will be created and maintained for this repository. This also implies, that using "targeted searches" (of maven
   * indexer or any "future" technology we'd use for indexing) should work against this repository.
   */
  boolean isIndexable();

  /**
   * Sets the indexable property of repository. If {@code true}, its content will be indexed by Indexer, otherwise
   * not.
   */
  void setIndexable(boolean val);

  /**
   * Is Repository searchable? If {@code true} (it implies {@link #isIndexable()} is {@code true} also, client code
   * should check both!), it means that in case of "non targeted searches" (aka "global searches" done against this
   * Nexus instance not specifying repository to search in) this repository will participate in search, will
   * contribute to search results: it will be searched too, and found hits will be added to resulting search results.
   * If {@code false}, then this repository might be searched only using "targeted searches", when this repository is
   * explicity stated as search target.
   */
  boolean isSearchable();

  /**
   * Sets the searchable property of repository. If {@code true}, it will be searched when doing
   * "non targeted searches", otherwise not.
   */
  void setSearchable(boolean val);

  // ==================================================
  // Maintenance

  /**
   * Expires all the caches used by this repository implementation from path and below. This methods delegates to
   * {@link #expireCaches(ResourceStoreRequest, WalkerFilter)} method using {@code null} for filter.
   *
   * @param request a path from to start descending. If null, it is taken as "root".
   */
  void expireCaches(ResourceStoreRequest request);

  /**
   * Expires all the caches used by this repository implementation from path and below. What kind of caches are
   * tackled depends on the actual implementation behind this interface (NFC, proxy cache or something third). To
   * gain
   * more control, you can call corresponding methods manually too. Currently, this method equals to a single call to
   * {@link #expireNotFoundCaches(ResourceStoreRequest)} on hosted repositories, and on a sequential calls of
   * {@link ProxyRepository#expireProxyCaches(ResourceStoreRequest)} and
   * {@link #expireNotFoundCaches(ResourceStoreRequest)} on proxy repositories. Moreover, on group repositories, this
   * call is propagated to it's member repositories!
   *
   * @param request a path from to start descending. If null, it is taken as "root".
   * @param filter  to apply or {@code null} for "all".
   * @return {@code true} if cache modified.
   * @since 2.1
   */
  boolean expireCaches(ResourceStoreRequest request, WalkerFilter filter);

  /**
   * Purges the NFC caches from path and below. This methods delegates to
   * {@link #expireNotFoundCaches(ResourceStoreRequest, WalkerFilter)} method using {@code null} for filter.
   */
  void expireNotFoundCaches(ResourceStoreRequest request);

  /**
   * Purges the NFC caches from path and below.
   *
   * @param filter to apply or {@code null} for "all".
   * @return {@code true} if cache modified.
   * @since 2.1
   */
  boolean expireNotFoundCaches(ResourceStoreRequest request, WalkerFilter filter);

  /**
   * Returns the meta data manager instance of this repository.
   *
   * @since 2.1
   */
  RepositoryMetadataManager getRepositoryMetadataManager();

  /**
   * Evicts items that were last used before timestamp.
   */
  Collection<String> evictUnusedItems(ResourceStoreRequest request, long timestamp);

  /**
   * Forces the recreation of attributes on this repository.
   *
   * @param initialData the initial data
   * @return true, if recreate attributes
   */
  boolean recreateAttributes(ResourceStoreRequest request, Map<String, String> initialData);

  /**
   * Returns the repository level AccessManager. Per repository instance may exists.
   *
   * @return the access manager
   */
  AccessManager getAccessManager();

  // ==================================================
  // Alternative (and unprotected) Content access
  // THESE ARE DEPRECATED! They used as circumvention for tasks running without valid JSec subject

  @Deprecated
  StorageItem retrieveItem(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException;

  @Deprecated
  void copyItem(boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

  @Deprecated
  void moveItem(boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

  @Deprecated
  void deleteItem(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException;

  @Deprecated
  Collection<StorageItem> list(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException;

  // Alternative content access
  // These will stay!

  void storeItem(boolean fromTask, StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException;

  Collection<StorageItem> list(boolean fromTask, StorageCollectionItem item)
      throws IllegalOperationException, ItemNotFoundException, StorageException;
}
