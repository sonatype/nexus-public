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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.ExternalConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.cache.CacheManager;
import org.sonatype.nexus.proxy.cache.PathCache;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventExpireNotFoundCaches;
import org.sonatype.nexus.proxy.events.RepositoryEventLocalStatusChanged;
import org.sonatype.nexus.proxy.events.RepositoryEventRecreateAttributes;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDeleteRoot;
import org.sonatype.nexus.proxy.events.RepositoryItemEventRetrieve;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStoreCreate;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStoreUpdate;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ContentGenerator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageNotFoundItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.ReadLockingContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsRemotelyAccessibleAttribute;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.DefaultLocalStorageContext;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.proxy.targets.TargetSet;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.ParentOMatic;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.walker.WalkerFilter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * <p>
 * A common base for Proximity repository. It defines all the needed properties and main methods as in
 * ProximityRepository interface.
 * <p>
 * This abstract class handles the following functionalities:
 * <ul>
 * <li>Holds base properties like repo ID, group ID, rank</li>
 * <li>Manages AccessManager</li>
 * <li>Manages notFoundCache to speed up responses</li>
 * <li>Manages event listeners</li>
 * </ul>
 * <p>
 * The subclasses only needs to implement the abstract method focusing on item retrieaval and other "basic" functions.
 *
 * @author cstamas
 */
public abstract class AbstractRepository
    extends ConfigurableRepository
    implements Repository
{

  // == injected

  private CacheManager cacheManager;

  private TargetRegistry targetRegistry;

  private RepositoryItemUidFactory repositoryItemUidFactory;

  private RepositoryItemUidAttributeManager repositoryItemUidAttributeManager;

  private Walker walker;

  private MimeSupport mimeSupport;

  private Map<String, ContentGenerator> contentGenerators;

  private AttributesHandler attributesHandler;

  private AccessManager accessManager;

  // == set by this

  /**
   * Local storage context to store storage-wide configs.
   */
  private LocalStorageContext localStorageContext;

  /**
   * The not found cache.
   */
  private PathCache notFoundCache;

  /**
   * Request strategies map. Supersedes RequestProcessors.
   *
   * @since 2.5
   */
  private final Map<String, RequestStrategy> requestStrategies = Maps.newHashMap();

  // ==

  /**
   * The local storage, set by configurator.
   */
  private LocalRepositoryStorage localStorage;

  /**
   * if local url changed, need special handling after save
   */
  private boolean localUrlChanged = false;

  /**
   * if non-indexable -> indexable change occured, need special handling after save
   */
  private boolean madeSearchable = false;

  /**
   * if local status changed, need special handling after save
   */
  private boolean localStatusChanged = false;

  // --

  @Inject
  public void populateAbstractRepository(
      CacheManager cacheManager, TargetRegistry targetRegistry, RepositoryItemUidFactory repositoryItemUidFactory,
      RepositoryItemUidAttributeManager repositoryItemUidAttributeManager, AccessManager accessManager, Walker walker,
      MimeSupport mimeSupport, Map<String, ContentGenerator> contentGenerators, AttributesHandler attributesHandler)
  {
    this.cacheManager = checkNotNull(cacheManager);
    this.targetRegistry = checkNotNull(targetRegistry);
    this.repositoryItemUidFactory = checkNotNull(repositoryItemUidFactory);
    this.repositoryItemUidAttributeManager = checkNotNull(repositoryItemUidAttributeManager);
    this.walker = checkNotNull(walker);
    this.mimeSupport = checkNotNull(mimeSupport);
    this.contentGenerators = checkNotNull(contentGenerators);
    this.attributesHandler = checkNotNull(attributesHandler);
    this.accessManager = checkNotNull(accessManager);

    // we have been not configured yet! So, we have no ID and stuff coming from config!
    // post inject stuff
    this.localStorageContext = new DefaultLocalStorageContext(
        getApplicationConfiguration().getGlobalLocalStorageContext());
  }

  protected MimeSupport getMimeSupport() {
    return mimeSupport;
  }

  @Override
  public MimeRulesSource getMimeRulesSource() {
    return MimeRulesSource.NOOP;
  }

  // ==

  @Override
  protected abstract Configurator getConfigurator();

  @Override
  protected abstract CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory();

  @Override
  protected void doConfigure()
      throws ConfigurationException
  {
    super.doConfigure();
    if (notFoundCache == null) {
      this.notFoundCache = cacheManager.getPathCache(getId());
    }
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean wasDirty = super.commitChanges();

    if (wasDirty) {
      eventBus().post(getRepositoryConfigurationUpdatedEvent());
    }

    this.localUrlChanged = false;

    this.madeSearchable = false;

    this.localStatusChanged = false;

    return wasDirty;
  }

  @Override
  public boolean rollbackChanges() {
    this.localUrlChanged = false;

    this.madeSearchable = false;

    this.localStatusChanged = false;

    return super.rollbackChanges();
  }

  protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent() {
    RepositoryConfigurationUpdatedEvent event = new RepositoryConfigurationUpdatedEvent(this);

    event.setLocalUrlChanged(this.localUrlChanged);
    event.setMadeSearchable(this.madeSearchable);
    event.setLocalStatusChanged(localStatusChanged);

    return event;
  }

  protected AbstractRepositoryConfiguration getExternalConfiguration(boolean forModification) {
    final CRepositoryCoreConfiguration cc = getCurrentCoreConfiguration();
    if (cc != null) {
      ExternalConfiguration<?> ec = cc.getExternalConfiguration();
      if (ec != null) {
        return (AbstractRepositoryConfiguration) ec.getConfiguration(forModification);
      }
    }
    return null;
  }

  // ==

  @Override
  public RequestStrategy registerRequestStrategy(final String key, final RequestStrategy strategy) {
    checkNotNull(key);
    checkNotNull(strategy);
    synchronized (requestStrategies) {
      return requestStrategies.put(key, strategy);
    }
  }

  @Override
  public RequestStrategy unregisterRequestStrategy(final String key) {
    checkNotNull(key);
    synchronized (requestStrategies) {
      return requestStrategies.remove(key);
    }
  }

  @Override
  public Map<String, RequestStrategy> getRegisteredStrategies() {
    synchronized (requestStrategies) {
      return Maps.newHashMap(requestStrategies);
    }
  }

  /**
   * Returns the repository Item Uid Factory.
   */
  protected RepositoryItemUidFactory getRepositoryItemUidFactory() {
    return repositoryItemUidFactory;
  }

  /**
   * Gets the not found cache.
   *
   * @return the not found cache
   */
  @Override
  public PathCache getNotFoundCache() {
    return notFoundCache;
  }

  @Override
  public void setIndexable(boolean indexable) {
    if (!isIndexable() && indexable) {
      // we have a non-indexable -> indexable transition
      madeSearchable = true;
    }

    super.setIndexable(indexable);
  }

  @Override
  public void setLocalUrl(String localUrl)
      throws LocalStorageException
  {
    String newLocalUrl = null;

    if (localUrl != null) {
      newLocalUrl = localUrl.trim();
    }

    if (newLocalUrl != null) {
      if (newLocalUrl.endsWith(RepositoryItemUid.PATH_SEPARATOR)) {
        newLocalUrl = newLocalUrl.substring(0, newLocalUrl.length() - 1);
      }

      getLocalStorage().validateStorageUrl(newLocalUrl);
    }

    // Dont use getLocalUrl since that applies default
    if (getCurrentConfiguration(false).getLocalStorage() != null
        && !StringUtils.equals(newLocalUrl, getCurrentConfiguration(false).getLocalStorage().getUrl())) {
      this.localUrlChanged = true;
    }

    super.setLocalUrl(localUrl);
  }

  @Override
  public void setLocalStatus(LocalStatus localStatus) {
    if (!localStatus.equals(getLocalStatus())) {
      LocalStatus oldLocalStatus = getLocalStatus();

      super.setLocalStatus(localStatus);

      localStatusChanged = true;

      eventBus().post(new RepositoryEventLocalStatusChanged(this, oldLocalStatus, localStatus));
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <F> F adaptToFacet(Class<F> t) {
    if (getRepositoryKind().isFacetAvailable(t)) {
      return (F) this;
    }
    else {
      return null;
    }
  }

  protected Walker getWalker() {
    return walker;
  }

  protected Map<String, ContentGenerator> getContentGenerators() {
    return contentGenerators;
  }

  // ===================================================================================
  // Repository iface

  @Override
  public AccessManager getAccessManager() {
    return accessManager;
  }

  /**
   * DefaultReleaseRemoverIT
   */
  @VisibleForTesting
  public void setAccessManager(AccessManager accessManager) {
    this.accessManager = accessManager;
  }

  /**
   * Returns {@code true} if action should be performed against this repository. Should guard against repeated
   * processing, like in "cascade" case when a group does action against it's members.
   */
  protected boolean shouldServiceOperation(final ResourceStoreRequest request, final String action) {
    if (!getLocalStatus().shouldServiceRequest()) {
      log.debug("Not performing {}, {} is not in service.", action, this);
      return false;
    }
    if (request.getProcessedRepositories().contains(getId())) {
      log.debug("Not performing {}, {} was already processed in this request.", action, this);
      return false;
    }
    request.addProcessedRepository(getId());
    return true;
  }

  @Override
  public final void expireCaches(final ResourceStoreRequest request) {
    expireCaches(request, null);
  }

  @Override
  public final boolean expireCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    if (!shouldServiceOperation(request, "expireCaches")) {
      return false;
    }
    log.debug("Expiring caches in repository {} from path='{}'", this, request.getRequestPath());
    return doExpireCaches(request, filter);
  }

  protected boolean doExpireCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    // at this level (we are not proxy) expireCaches() actually boils down to "expire NFC" only
    // we are NOT crawling local storage content to flip the isExpired flags to true on a hosted
    // repo, since those attributes in case of hosted (or any other non-proxy) repositories does not have any
    // meaning
    return doExpireNotFoundCaches(request, filter);
  }

  @Override
  public final void expireNotFoundCaches(final ResourceStoreRequest request) {
    expireNotFoundCaches(request, null);
  }

  @Override
  public final boolean expireNotFoundCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    if (!shouldServiceOperation(request, "expireNotFoundCaches")) {
      return false;
    }
    log.debug("Expiring NFC caches in repository {} from path='{}'", this, request.getRequestPath());
    return doExpireNotFoundCaches(request, filter);
  }

  protected boolean doExpireNotFoundCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    if (StringUtils.isBlank(request.getRequestPath())) {
      request.setRequestPath(RepositoryItemUid.PATH_ROOT);
    }
    boolean cacheAltered = false;
    // remove the items from NFC
    if (filter == null) {
      if (RepositoryItemUid.PATH_ROOT.equals(request.getRequestPath())) {
        // purge all
        if (getNotFoundCache() != null) {
          cacheAltered = getNotFoundCache().purge();
        }
      }
      else {
        // purge below and above path only
        if (getNotFoundCache() != null) {
          boolean altered1 = getNotFoundCache().removeWithParents(request.getRequestPath());
          boolean altered2 = getNotFoundCache().removeWithChildren(request.getRequestPath());
          cacheAltered = altered1 || altered2;
        }
      }
    }
    else {
      final ParentOMatic parentOMatic = new ParentOMatic(false);
      final DefaultWalkerContext context = new DefaultWalkerContext(this, request);
      final Collection<String> nfcPaths = getNotFoundCache().listKeysInCache();

      for (String nfcPath : nfcPaths) {
        final DefaultStorageNotFoundItem nfcItem =
            new DefaultStorageNotFoundItem(this, new ResourceStoreRequest(nfcPath));

        if (filter.shouldProcess(context, nfcItem)) {
          parentOMatic.addAndMarkPath(nfcPath);
        }
      }

      for (String path : parentOMatic.getMarkedPaths()) {
        boolean removed = getNotFoundCache().remove(path);
        cacheAltered = cacheAltered || removed;
      }
    }

    if (log.isDebugEnabled()) {
      if (cacheAltered) {
        log.info("NFC for repository {} from path='{}' was cleared.", this, request.getRequestPath());
      }
      else {
        log.debug("Clear NFC for repository {} from path='{}' did not alter cache.", this, request.getRequestPath());
      }
    }

    eventBus().post(
        new RepositoryEventExpireNotFoundCaches(this, request.getRequestPath(),
            request.getRequestContext().flatten(), cacheAltered));

    return cacheAltered;
  }

  @Override
  public RepositoryMetadataManager getRepositoryMetadataManager() {
    return new NoopRepositoryMetadataManager();
  }

  @Override
  public final Collection<String> evictUnusedItems(final ResourceStoreRequest request, final long timestamp) {
    if (!shouldServiceOperation(request, "evictUnusedItems")) {
      return Collections.emptyList();
    }
    log.debug("Evicting unused items in repository {}, from path='{}'", this, request.getRequestPath());
    return doEvictUnusedItems(request, timestamp);
  }

  protected Collection<String> doEvictUnusedItems(final ResourceStoreRequest request, final long timestamp) {
    // this is noop at hosted level
    return Collections.emptyList();
  }

  // TODO: this is strictly Nexus internal thing, not exposed in any way, nothing should override this
  // Also, AttributeStorage should never be "rebuilt", thats := data loss
  @Override
  public final boolean recreateAttributes(final ResourceStoreRequest request, final Map<String, String> initialData) {
    if (!shouldServiceOperation(request, "recreateAttributes")) {
      return false;
    }
    if (StringUtils.isEmpty(request.getRequestPath())) {
      request.setRequestPath(RepositoryItemUid.PATH_ROOT);
    }
    log.info("Rebuilding item attributes in repository {} from path='{}'", this, request.getRequestPath());
    final RecreateAttributesWalker walkerProcessor = new RecreateAttributesWalker(this, initialData);
    final DefaultWalkerContext ctx = new DefaultWalkerContext(this, request);
    ctx.getProcessors().add(walkerProcessor);
    // let it loose
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
    eventBus().post(new RepositoryEventRecreateAttributes(this));
    return true;
  }

  @Override
  public AttributesHandler getAttributesHandler() {
    return attributesHandler;
  }

  @Override
  public LocalStorageContext getLocalStorageContext() {
    return localStorageContext;
  }

  @Override
  public LocalRepositoryStorage getLocalStorage() {
    return localStorage;
  }

  @Override
  public void setLocalStorage(LocalRepositoryStorage localStorage) {
    getCurrentConfiguration(true).getLocalStorage().setProvider(localStorage.getProviderId());

    this.localStorage = localStorage;
  }

  // ===================================================================================
  // Store iface

  @Override
  public StorageItem retrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException, AccessDeniedException
  {
    checkConditions(request, Action.read);

    StorageItem item = retrieveItem(false, request);

    if (StorageCollectionItem.class.isAssignableFrom(item.getClass()) && !isBrowseable()) {
      log.debug(
          getId() + " retrieveItem() :: FOUND a collection on " + request.toString()
              + " but repository is not Browseable.");

      throw new ItemNotFoundException(reasonFor(request, this, "Repository %s is not browsable",
          this));
    }

    checkPostConditions(request, item);

    return item;
  }

  @Override
  public void copyItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
             StorageException, AccessDeniedException
  {
    checkConditions(from, Action.read);
    checkConditions(to, getResultingActionOnWrite(to));

    copyItem(false, from, to);
  }

  @Override
  public void moveItem(ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
             StorageException, AccessDeniedException
  {
    checkConditions(from, Action.read);
    checkConditions(from, Action.delete);
    checkConditions(to, getResultingActionOnWrite(to));

    moveItem(false, from, to);
  }

  @Override
  public void deleteItem(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
             StorageException, AccessDeniedException
  {
    checkConditions(request, Action.delete);

    deleteItem(false, request);
  }

  @Override
  public void storeItem(ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    try {
      checkConditions(request, getResultingActionOnWrite(request));
    }
    catch (ItemNotFoundException e) {
      throw new AccessDeniedException(request, e.getMessage());
    }

    DefaultStorageFileItem fItem =
        new DefaultStorageFileItem(this, request, true, true, new PreparedContentLocator(is,
            getMimeSupport().guessMimeTypeFromPath(getMimeRulesSource(), request.getRequestPath()),
            ContentLocator.UNKNOWN_LENGTH));

    if (userAttributes != null) {
      fItem.getRepositoryItemAttributes().putAll(userAttributes);
    }

    storeItem(false, fItem);
  }

  @Override
  public void createCollection(ResourceStoreRequest request, Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    try {
      checkConditions(request, getResultingActionOnWrite(request));
    }
    catch (ItemNotFoundException e) {
      throw new AccessDeniedException(request, e.getMessage());
    }

    DefaultStorageCollectionItem coll = new DefaultStorageCollectionItem(this, request, true, true);

    if (userAttributes != null) {
      coll.getRepositoryItemAttributes().putAll(userAttributes);
    }

    storeItem(false, coll);
  }

  @Override
  public Collection<StorageItem> list(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException, AccessDeniedException
  {
    checkConditions(request, Action.read);

    Collection<StorageItem> items = null;

    if (isBrowseable()) {
      items = list(false, request);
    }
    else {
      throw new ItemNotFoundException(reasonFor(request, this, "Repository %s is not browsable", this));
    }

    return items;
  }

  @Override
  public TargetSet getTargetsForRequest(ResourceStoreRequest request) {
    if (log.isDebugEnabled()) {
      log.debug("getTargetsForRequest() :: " + this.getId() + ":" + request.getRequestPath());
    }

    return targetRegistry.getTargetsForRepositoryPath(this, request.getRequestPath());
  }

  @Override
  public boolean hasAnyTargetsForRequest(ResourceStoreRequest request) {
    if (log.isDebugEnabled()) {
      log.debug("hasAnyTargetsForRequest() :: " + this.getId());
    }

    return targetRegistry.hasAnyApplicableTarget(this);
  }

  @Override
  public Action getResultingActionOnWrite(final ResourceStoreRequest rsr)
      throws LocalStorageException
  {
    final boolean isInLocalStorage = getLocalStorage().containsItem(this, rsr);

    if (isInLocalStorage) {
      return Action.update;
    }
    else {
      return Action.create;
    }
  }

  // ===================================================================================
  // Repositry store-like

  @Override
  public StorageItem retrieveItem(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".retrieveItem() :: " + request.toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    request.addProcessedRepository(getId());

    maintainNotFoundCache(request);

    final RepositoryItemUid uid = createUid(request.getRequestPath());

    final RepositoryItemUidLock uidLock = uid.getLock();

    uidLock.lock(Action.read);

    try {
      StorageItem item = doRetrieveItem(request);

      // file with generated content?
      if (item instanceof StorageFileItem && ((StorageFileItem) item).isContentGenerated()) {
        StorageFileItem file = (StorageFileItem) item;

        String key = file.getContentGeneratorId();

        if (getContentGenerators().containsKey(key)) {
          ContentGenerator generator = getContentGenerators().get(key);

          try {
            file.setContentLocator(generator.generateContent(this, uid.getPath(), file));
          }
          catch (Exception e) {
            throw new LocalStorageException("Could not generate content:", e);
          }
        }
        else {
          log.info(
              String.format(
                  "The file in repository %s on path=\"%s\" should be generated by ContentGeneratorId=%s, but component does not exists!",
                  RepositoryStringUtils.getHumanizedNameString(this), uid.getPath(), key));

          throw new ItemNotFoundException(reasonFor(request, this,
              "The generator for generated path %s with key %s not found in %s", request.getRequestPath(),
              key, this));
        }
      }

      eventBus().post(new RepositoryItemEventRetrieve(this, item));

      if (log.isDebugEnabled()) {
        log.debug(getId() + " retrieveItem() :: FOUND " + uid.toString());
      }

      return item;
    }
    catch (ItemNotFoundException ex) {
      if (log.isDebugEnabled()) {
        log.debug(getId() + " retrieveItem() :: NOT FOUND " + uid.toString());
      }

      if (shouldAddToNotFoundCache(request)) {
        addToNotFoundCache(request);
      }

      throw ex;
    }
    finally {
      uidLock.unlock();
    }
  }

  @Override
  public void copyItem(boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".copyItem() :: " + from.toString() + " --> " + to.toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    maintainNotFoundCache(from);

    final RepositoryItemUid fromUid = createUid(from.getRequestPath());

    final RepositoryItemUid toUid = createUid(to.getRequestPath());

    final RepositoryItemUidLock fromUidLock = fromUid.getLock();

    final RepositoryItemUidLock toUidLock = toUid.getLock();

    fromUidLock.lock(Action.read);
    toUidLock.lock(Action.create);

    try {
      StorageItem item = retrieveItem(fromTask, from);

      if (StorageFileItem.class.isAssignableFrom(item.getClass())) {
        try {
          DefaultStorageFileItem target =
              new DefaultStorageFileItem(this, to, true, true, ((StorageFileItem) item).getContentLocator());

          storeItem(fromTask, target);

          // remove the "to" item from n-cache if there
          removeFromNotFoundCache(to);
        }
        catch (IOException e) {
          throw new LocalStorageException("Could not get the content of source file (is it file?)!", e);
        }
      }
    }
    finally {
      toUidLock.unlock();

      fromUidLock.unlock();
    }
  }

  @Override
  public void moveItem(boolean fromTask, ResourceStoreRequest from, ResourceStoreRequest to)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".moveItem() :: " + from.toString() + " --> " + to.toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    copyItem(fromTask, from, to);

    deleteItem(fromTask, from);
  }

  @Override
  public void deleteItem(boolean fromTask, ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".deleteItem() :: " + request.toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    maintainNotFoundCache(request);

    final RepositoryItemUid uid = createUid(request.getRequestPath());

    final RepositoryItemUidLock uidLock = uid.getLock();

    uidLock.lock(Action.delete);

    try {
      StorageItem item = null;
      try {
        // determine is the thing to be deleted a collection or not
        item = getLocalStorage().retrieveItem(this, request);
      }
      catch (ItemNotFoundException ex) {
        if (shouldNeglectItemNotFoundExOnDelete(request, ex)) {
          item = null;
        }
        else {
          throw ex;
        }
      }

      if (item != null) {
        // fire the event for file being deleted
        eventBus().post(new RepositoryItemEventDeleteRoot(this, item));

        // if we are deleting a collection, perform recursive notification about this too
        if (item instanceof StorageCollectionItem) {
          log.debug("deleting a collection '{}'", item.getPath());

          // NEXUS-7628: If collection is being deleted, purge all of it's children from NFC
          if (isNotFoundCacheActive()) {
            getNotFoundCache().removeWithChildren(request.getRequestPath());
          }

          // it is collection, walk it and below and fire events for all files
          DeletionNotifierWalker dnw = new DeletionNotifierWalker(eventBus(), request);

          DefaultWalkerContext ctx = new DefaultWalkerContext(this, request);

          ctx.getProcessors().add(dnw);

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

        doDeleteItem(request);
      }
    }
    finally {
      uidLock.unlock();
    }
  }

  /**
   * Decides should a {@link ItemNotFoundException} be neglected on
   * {@link #deleteItem(boolean, org.sonatype.nexus.proxy.ResourceStoreRequest)} method invocation or not. Nexus
   * was always throwing this exception when deletion of non existent item was tried, but since 2.4 in Maven support
   * the Maven checksum files are not existing anymore as standalone items (in local storage), hence default
   * implementation of this method simply returns {@code false} to retain this behaviour, but still, to make
   * Repository implementations able to override this.
   *
   * @since 2.6
   */
  protected boolean shouldNeglectItemNotFoundExOnDelete(ResourceStoreRequest request, ItemNotFoundException ex) {
    return false;
  }

  @Override
  public void storeItem(boolean fromTask, StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".storeItem() :: " + item.getRepositoryItemUid().toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    final RepositoryItemUid uid = createUid(item.getPath());

    // replace UID to own one
    item.setRepositoryItemUid(uid);

    // NEXUS-4550: This "fake" UID/lock here is introduced only to serialize uploaders
    // This will catch immediately an uploader if an upload already happens
    // and prevent deadlocks, since uploader still does not have
    // shared lock
    final RepositoryItemUid uploaderUid = createUid(item.getPath() + ".storeItem()");

    final RepositoryItemUidLock uidUploaderLock = uploaderUid.getLock();

    uidUploaderLock.lock(Action.create);

    final Action action = getResultingActionOnWrite(item.getResourceStoreRequest());

    try {
      if (item.getResourceStoreRequest().isExternal()) {
        enforceWritePolicy(item.getResourceStoreRequest(), action); // 2nd check within exclusive lock
      }

      // NEXUS-4550: we are shared-locking the actual UID (to not prevent downloaders while
      // we save to temporary location. But this depends on actual LS backend actually...)
      // but we exclusive lock uploaders to serialize them!
      // And the LS has to take care of whatever stricter locking it has to use or not
      // Think: RDBMS LS or some trickier LS implementations for example
      final RepositoryItemUidLock uidLock = uid.getLock();

      uidLock.lock(Action.read);

      try {
        // store it
        getLocalStorage().storeItem(this, item);
      }
      finally {
        uidLock.unlock();
      }
    }
    finally {
      uidUploaderLock.unlock();
    }

    // remove the "request" item from n-cache if there
    removeFromNotFoundCache(item.getResourceStoreRequest());

    if (Action.create.equals(action)) {
      eventBus().post(new RepositoryItemEventStoreCreate(this, item));
    }
    else {
      eventBus().post(new RepositoryItemEventStoreUpdate(this, item));
    }
  }

  @Override
  public Collection<StorageItem> list(boolean fromTask, ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".list() :: " + request.toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    request.addProcessedRepository(getId());

    StorageItem item = retrieveItem(fromTask, request);

    if (item instanceof StorageCollectionItem) {
      return list(fromTask, (StorageCollectionItem) item);
    }
    else {
      throw new ItemNotFoundException(reasonFor(request, this, "Path %s in repository %s is not a collection",
          request.getRequestPath(), this));
    }
  }

  @Override
  public Collection<StorageItem> list(boolean fromTask, StorageCollectionItem coll)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug(getId() + ".list() :: " + coll.getRepositoryItemUid().toString());
    }

    if (!getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    maintainNotFoundCache(coll.getResourceStoreRequest());

    Collection<StorageItem> items = doListItems(new ResourceStoreRequest(coll));

    return items;
  }

  @Override
  public RepositoryItemUid createUid(final String path) {
    return getRepositoryItemUidFactory().createUid(this, path);
  }

  @Override
  public RepositoryItemUidAttributeManager getRepositoryItemUidAttributeManager() {
    return repositoryItemUidAttributeManager;
  }

  // ===================================================================================
  // Inner stuff

  /**
   * Maintains not found cache.
   *
   * @throws ItemNotFoundException the item not found exception
   */
  @Override
  public void maintainNotFoundCache(ResourceStoreRequest request)
      throws ItemNotFoundException
  {
    // NEXUS-6177: skip NFC if request is "asExpired"
    // On outcome, if remotely found, will invalidate NFC by caching it
    if (isNotFoundCacheActive() && !request.isRequestAsExpired()) {
      if (getNotFoundCache().contains(request.getRequestPath())) {
        if (getNotFoundCache().isExpired(request.getRequestPath())) {
          if (log.isDebugEnabled()) {
            log.debug("The path " + request.getRequestPath() + " is in NFC but expired.");
          }

          removeFromNotFoundCache(request);
        }
        else {
          StringBuilder sb = new StringBuilder("The path ").append(request.getRequestPath()).append(" is cached ");
          long expirationTime = getNotFoundCache().getExpirationTime(request.getRequestPath());
          if (expirationTime > 0) {
            sb.append("until ").append(new DateTime(expirationTime)).append(" ");
          }
          final String message = sb.append("as not found in repository ").append(this).toString();

          log.debug(message);
          throw new ItemNotFoundException(reasonFor(request, this, message));
        }
      }
    }
  }

  /**
   * Adds the uid to not found cache.
   */
  @Override
  public void addToNotFoundCache(ResourceStoreRequest request) {
    if (isNotFoundCacheActive()) {
      if (log.isDebugEnabled()) {
        log.debug("Adding path " + request.getRequestPath() + " to NFC.");
      }

      getNotFoundCache().put(request.getRequestPath(), Boolean.TRUE, getNotFoundCacheTimeToLive() * 60);
    }
  }

  /**
   * Removes the uid from not found cache.
   */
  @Override
  public void removeFromNotFoundCache(ResourceStoreRequest request) {
    if (isNotFoundCacheActive()) {
      if (log.isDebugEnabled()) {
        log.debug("Removing path " + request.getRequestPath() + " from NFC.");
      }

      getNotFoundCache().removeWithParents(request.getRequestPath());
    }
  }

  /**
   * Check conditions, such as availability, permissions, etc.
   *
   * @param request the request
   * @throws RepositoryNotAvailableException
   *                               the repository not available exception
   * @throws AccessDeniedException the access denied exception
   */
  protected void checkConditions(ResourceStoreRequest request, Action action)
      throws ItemNotFoundException, IllegalOperationException, AccessDeniedException
  {
    if (!this.getLocalStatus().shouldServiceRequest()) {
      throw new RepositoryNotAvailableException(this);
    }

    // for external requests, ensure item is remotely accessible
    if (request.isExternal()) {
      final RepositoryItemUid uid = createUid(request.getRequestPath());
      if (!uid.getBooleanAttributeValue(IsRemotelyAccessibleAttribute.class)) {
        log.debug("Request for remotely non-accessible UID {} is forbidden", uid);
        throw new ItemNotFoundException(
            ItemNotFoundException.reasonFor(request, this, "External access to UID %s not allowed!", uid));
      }
    }

    // check for writing to read only repo
    // Readonly is ALWAYS read only
    if (RepositoryWritePolicy.READ_ONLY.equals(getWritePolicy()) && !isActionAllowedReadOnly(action)) {
      throw new IllegalRequestException(request, "Repository with ID='" + getId()
          + "' is Read Only, but action was '" + action.toString() + "'!");
    }
    // but Write/write once may need to allow updating metadata
    // check the write policy
    enforceWritePolicy(request, action);

    // NXCM-3600: this if was an old remnant, is not needed
    // if ( isExposed() )
    // {
    getAccessManager().decide(this, request, action);
    // }

    checkRequestStrategies(request, action);
  }

  protected void checkRequestStrategies(final ResourceStoreRequest request, final Action action)
      throws ItemNotFoundException, IllegalOperationException
  {
    final Map<String, RequestStrategy> effectiveRequestStrategies = getRegisteredStrategies();
    for (RequestStrategy strategy : effectiveRequestStrategies.values()) {
      strategy.onHandle(this, request, action);
    }
  }

  protected void checkPostConditions(final ResourceStoreRequest request, final StorageItem item)
      throws IllegalOperationException, ItemNotFoundException
  {
    final Map<String, RequestStrategy> effectiveRequestStrategies = getRegisteredStrategies();
    for (RequestStrategy strategy : effectiveRequestStrategies.values()) {
      strategy.onServing(this, request, item);
    }
  }

  protected void enforceWritePolicy(ResourceStoreRequest request, Action action)
      throws IllegalRequestException
  {
    // check for write once (no redeploy)
    if (Action.update.equals(action) && !RepositoryWritePolicy.ALLOW_WRITE.equals(this.getWritePolicy())) {
      throw new IllegalRequestException(request, "Repository with ID='" + getId()
          + "' does not allow updating artifacts.");
    }
  }

  @Override
  public boolean isCompatible(Repository repository) {
    return getRepositoryContentClass().isCompatible(repository.getRepositoryContentClass());
  }

  protected void doDeleteItem(ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, ItemNotFoundException, StorageException
  {
    getLocalStorage().deleteItem(this, request);
  }

  protected Collection<StorageItem> doListItems(ResourceStoreRequest request)
      throws ItemNotFoundException, StorageException
  {
    return getLocalStorage().listItems(this, request);
  }

  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    return doRetrieveLocalItem(request);
  }

  protected AbstractStorageItem doRetrieveLocalItem(final ResourceStoreRequest request)
      throws ItemNotFoundException, LocalStorageException
  {
    AbstractStorageItem localItem = null;
    try {
      localItem = getLocalStorage().retrieveItem(this, request);
      if (localItem instanceof StorageFileItem) {
        StorageFileItem file = (StorageFileItem) localItem;
        // wrap the content locator if needed
        if (!(file.getContentLocator() instanceof ReadLockingContentLocator)) {
          final RepositoryItemUid uid = createUid(request.getRequestPath());
          file.setContentLocator(new ReadLockingContentLocator(uid, file.getContentLocator()));
        }
      }
      if (log.isDebugEnabled()) {
        log.debug("Item " + request.toString() + " found in local storage.");
      }
    }
    catch (ItemNotFoundException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Item " + request.toString() + " not found in local storage.");
      }
      throw ex;
    }

    return localItem;
  }

  protected boolean isActionAllowedReadOnly(Action action) {
    return action.isReadAction();
  }

  /**
   * Whether or not the requested path should be added to NFC. Item will be added to NFC if is not local/remote only.
   *
   * @param request resource store request
   * @return true if requested path should be added to NFC
   * @since 2.0
   */
  protected boolean shouldAddToNotFoundCache(final ResourceStoreRequest request) {
    // if not local/remote only, add it to NFC
    return !request.isRequestLocalOnly() && !request.isRequestRemoteOnly();
  }
}
