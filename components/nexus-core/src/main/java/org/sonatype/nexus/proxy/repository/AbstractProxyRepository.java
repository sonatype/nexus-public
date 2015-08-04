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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageEOFException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.RemoteAccessDeniedException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.RemoteStorageTransportException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryEventExpireProxyCaches;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeSet;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheCreate;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCacheUpdate;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.EvictUnusedItemsWalkerProcessor.EvictUnusedItemsWalkerFilter;
import org.sonatype.nexus.proxy.repository.threads.ThreadPoolManager;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.remote.AbstractHTTPRemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;
import org.sonatype.nexus.proxy.walker.WalkerFilter;
import org.sonatype.nexus.util.ConstantNumberSequence;
import org.sonatype.nexus.util.FibonacciNumberSequence;
import org.sonatype.nexus.util.NumberSequence;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.util.ExceptionUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * Adds the proxying capability to a simple repository. The proxying will happen only if reposiory has remote storage!
 * So, this implementation is used in both "simple" repository cases: hosted and proxy, but in 1st case there is no
 * remote storage.
 *
 * @author cstamas
 */
public abstract class AbstractProxyRepository
    extends AbstractRepository
    implements ProxyRepository
{

  /**
   * Default time to do NOT check an already known remote status: 5 mins.
   */
  private static final long DEFAULT_REMOTE_STATUS_RETAIN_TIME = 5L * 60L * 1000L;

  /**
   * The time while we do NOT check an already known remote status
   */
  private static final long REMOTE_STATUS_RETAIN_TIME = SystemPropertiesHelper.getLong(
      "plexus.autoblock.remote.status.retain.time", DEFAULT_REMOTE_STATUS_RETAIN_TIME);

  /**
   * The maximum amount of time to have a repository in AUTOBlock status: 60 minutes (1hr). This value is system
   * default, is used only as limiting point. When repository steps here, it will be checked for remote status hourly
   * only (unless forced by user).
   */
  private static final long AUTO_BLOCK_STATUS_MAX_RETAIN_TIME = 60L * 60L * 1000L;

  // == injected

  private SystemStatus systemStatus;

  private ThreadPoolManager poolManager;

  // == set by this

  /**
   * The remote status checker thread, used in Proxies for handling autoBlocking. Not to go into Pool above, is
   * handled separately.
   */
  private RepositoryStatusCheckerThread repositoryStatusCheckerThread;

  /**
   * Remote storage context to store connection configs.
   */
  private RemoteStorageContext remoteStorageContext;

  // == set by configurators

  /**
   * The remote storage.
   */
  private RemoteRepositoryStorage remoteStorage;

  // == internals

  /**
   * Item content validators. Maintained by configurator, but map is final and created internally.
   */
  private final Map<String, ItemContentValidator> itemContentValidators = Maps.newHashMap();

  /**
   * The proxy remote status
   */
  private volatile RemoteStatus remoteStatus = RemoteStatus.UNKNOWN;

  /**
   * Last time remote status was updated
   */
  private volatile long remoteStatusUpdated = 0;

  /**
   * if remote url changed, need special handling after save
   */
  private volatile boolean remoteUrlChanged = false;

  /**
   * How much should be the last known remote status be retained.
   */
  private volatile NumberSequence remoteStatusRetainTimeSequence = new ConstantNumberSequence(
      REMOTE_STATUS_RETAIN_TIME);

  @Inject
  public void populateAbstractProxyRepository(SystemStatus systemStatus, ThreadPoolManager poolManager) {
    this.systemStatus = checkNotNull(systemStatus);
    this.poolManager = checkNotNull(poolManager);

    // we have been not configured yet! So, we have no ID and stuff coming from config!
    // set here
    remoteStorageContext =
        new DefaultRemoteStorageContext(getApplicationConfiguration().getGlobalRemoteStorageContext());
  }

  @Override
  protected AbstractProxyRepositoryConfiguration getExternalConfiguration(boolean forModification) {
    return (AbstractProxyRepositoryConfiguration) getCurrentCoreConfiguration().getExternalConfiguration()
        .getConfiguration(
            forModification);
  }

  @Subscribe
  public void on(final NexusStoppedEvent e) {
    disposeRepositoryStatusCheckerThread();
  }

  private void createRepositoryStatusCheckerThread() {
    // only for proxy kind
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      if (repositoryStatusCheckerThread == null) {
        repositoryStatusCheckerThread =
            new RepositoryStatusCheckerThread(LoggerFactory.getLogger(getClass().getName() + "-"
                + getId()), systemStatus, this);
        repositoryStatusCheckerThread.setRunning(true);
        repositoryStatusCheckerThread.setDaemon(true);
        repositoryStatusCheckerThread.start();
      }
    }
  }

  private void disposeRepositoryStatusCheckerThread() {
    // not depend on kind, as it might be "transformed" from proxy to hosted
    if (repositoryStatusCheckerThread != null) {
      repositoryStatusCheckerThread.setRunning(false);
      repositoryStatusCheckerThread.interrupt();
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    // kill our daemon thread too, if needed
    disposeRepositoryStatusCheckerThread();
  }

  @Override
  protected void doConfigure()
      throws ConfigurationException
  {
    super.doConfigure();
    createRepositoryStatusCheckerThread();
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean result = super.commitChanges();

    if (result) {
      this.remoteUrlChanged = false;
    }

    return result;
  }

  @Override
  public boolean rollbackChanges() {
    this.remoteUrlChanged = false;

    return super.rollbackChanges();
  }

  @Override
  protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent() {
    RepositoryConfigurationUpdatedEvent event = super.getRepositoryConfigurationUpdatedEvent();

    event.setRemoteUrlChanged(this.remoteUrlChanged);

    return event;
  }

  @Override
  public final void expireProxyCaches(final ResourceStoreRequest request) {
    expireProxyCaches(request, null);
  }

  @Override
  public final boolean expireProxyCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    if (!shouldServiceOperation(request, "expireProxyCaches")) {
      return false;
    }
    log.debug("Expiring proxy caches in repository {} from path='{}'", this, request.getRequestPath());
    return doExpireProxyCaches(request, filter);
  }

  /**
   * Allows proxy cache invalidation to be disabled by system property.
   *
   * @since 2.9
   */
  private static final boolean PROXY_CACHE_INVALIDATION_TOKEN_DISABLED = SystemPropertiesHelper.getBoolean(
      AbstractProxyRepository.class.getName() + ".proxyCacheInvalidationToken.disabled", false);

  static {
    if (PROXY_CACHE_INVALIDATION_TOKEN_DISABLED) {
      LoggerFactory.getLogger(AbstractProxyRepository.class).info("Proxy-cache invalidation-token support disabled");
    }
  }

  /**
   * Token set when expire proxy caches is done for entire collection.
   *
   * @see #doExpireProxyCaches(ResourceStoreRequest, WalkerFilter)
   * @since 2.9
   */
  protected volatile String proxyCacheInvalidationToken;

  /**
   * Key for storage-item invalidation token attribute.
   *
   * @see #isOld(int, StorageItem, boolean)
   * @since 2.9
   */
  public static final String PROXY_CACHE_INVALIDATION_TOKEN_KEY = "proxyRepository-invalidationToken";

  protected boolean doExpireProxyCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    // skip unless this is a proxy repository
    if (!getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      return false;
    }

    // normalize request path
    if (StringUtils.isEmpty(request.getRequestPath())) {
      request.setRequestPath(RepositoryItemUid.PATH_ROOT);
    }

    // flag to indicate if cache was altered or not
    boolean cacheAltered;

    // special handling for expiration for entire collection, unless disabled by system property
    if (!PROXY_CACHE_INVALIDATION_TOKEN_DISABLED && RepositoryItemUid.PATH_ROOT.equals(request.getRequestPath())) {
      // generate a unique token for this invalidation request
      proxyCacheInvalidationToken = String.valueOf(System.nanoTime());
      log.debug("Proxy cache marked as invalid for repository {}, token: {}", this, proxyCacheInvalidationToken);

      // assume the cache will alter, we can not know for sure w/o expensive walking operation
      cacheAltered = true;
    }
    else {
      // crawl the local storage (which is in this case proxy cache)
      // and flip the isExpired attribute bits to true
      request.setRequestLocalOnly(true);
      // 1st, expire all the files below path
      final DefaultWalkerContext ctx = new DefaultWalkerContext(this, request, filter);
      final ExpireCacheWalker expireCacheWalkerProcessor = new ExpireCacheWalker(this);
      ctx.getProcessors().add(expireCacheWalkerProcessor);
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
      cacheAltered = expireCacheWalkerProcessor.isCacheAltered();
    }

    if (log.isDebugEnabled()) {
      if (cacheAltered) {
        log.info("Proxy cache was expired for repository {} from path='{}'", this, request.getRequestPath());
      }
      else {
        log.debug("Proxy cache not altered for repository {} from path='{}'", this, request.getRequestPath());
      }
    }

    eventBus().post(new RepositoryEventExpireProxyCaches(
        this, request.getRequestPath(), request.getRequestContext().flatten(), cacheAltered));

    return cacheAltered;
  }

  protected boolean doExpireCaches(final ResourceStoreRequest request, final WalkerFilter filter) {
    // expire proxy cache
    boolean v1 = doExpireProxyCaches(request, filter);
    // do the stuff we inherited
    boolean v2 = super.doExpireCaches(request, filter);
    // return v1 OR v2
    return v1 || v2;
  }

  @Override
  protected Collection<String> doEvictUnusedItems(final ResourceStoreRequest request, final long timestamp) {
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      Collection<String> result =
          doEvictUnusedItems(request, timestamp, new EvictUnusedItemsWalkerProcessor(timestamp),
              new EvictUnusedItemsWalkerFilter());
      eventBus().post(new RepositoryEventEvictUnusedItems(this));
      return result;
    }
    else {
      return super.doEvictUnusedItems(request, timestamp);
    }
  }

  protected Collection<String> doEvictUnusedItems(ResourceStoreRequest request, final long timestamp,
                                                  EvictUnusedItemsWalkerProcessor processor, WalkerFilter filter)
  {
    request.setRequestLocalOnly(true);
    DefaultWalkerContext ctx = new DefaultWalkerContext(this, request, filter);
    ctx.getProcessors().add(processor);
    // and let it loose
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

    return processor.getFiles();
  }

  @Override
  public Map<String, ItemContentValidator> getItemContentValidators() {
    return itemContentValidators;
  }

  @Override
  public boolean isFileTypeValidation() {
    return getExternalConfiguration(false).isFileTypeValidation();
  }

  @Override
  public void setFileTypeValidation(boolean doValidate) {
    getExternalConfiguration(true).setFileTypeValidation(doValidate);
  }

  @Override
  public boolean isItemAgingActive() {
    return getExternalConfiguration(false).isItemAgingActive();
  }

  @Override
  public void setItemAgingActive(boolean value) {
    getExternalConfiguration(true).setItemAgingActive(value);
  }

  @Override
  public boolean isAutoBlockActive() {
    return getExternalConfiguration(false).isAutoBlockActive();
  }

  @Override
  public void setAutoBlockActive(boolean val) {
    // NEXUS-3516: if user disables autoblock, and repo is auto-blocked, unblock it
    if (!val && ProxyMode.BLOCKED_AUTO.equals(getProxyMode())) {
      log.warn(
          String.format(
              "Proxy Repository %s was auto-blocked, but user disabled this feature. Unblocking repository, but this MAY cause Nexus to leak connections (if remote repository is still down)!",
              RepositoryStringUtils.getHumanizedNameString(this)));

      setProxyMode(ProxyMode.ALLOW);
    }

    getExternalConfiguration(true).setAutoBlockActive(val);
  }

  @Override
  public long getCurrentRemoteStatusRetainTime() {
    return this.remoteStatusRetainTimeSequence.peek();
  }

  @Override
  public long getNextRemoteStatusRetainTime() {
    // step it up, but topped
    if (this.remoteStatusRetainTimeSequence.peek() <= AUTO_BLOCK_STATUS_MAX_RETAIN_TIME) {
      // step it up
      return this.remoteStatusRetainTimeSequence.next();
    }
    else {
      // it is topped, so just return current
      return getCurrentRemoteStatusRetainTime();
    }
  }

  @Override
  public ProxyMode getProxyMode() {
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      return getExternalConfiguration(false).getProxyMode();
    }
    else {
      return null;
    }
  }

  /**
   * ProxyMode is a persisted configuration property, hence it modifies configuration! It is the caller
   * responsibility
   * to save configuration.
   */
  protected void setProxyMode(ProxyMode proxyMode, boolean sendNotification, Throwable cause) {
    if (getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      ProxyMode oldProxyMode = getProxyMode();

      // NEXUS-4537: apply transition constraints: BLOCKED_MANUALLY cannot be transitioned into BLOCKED_AUTO
      if (!(ProxyMode.BLOCKED_AUTO.equals(proxyMode) && ProxyMode.BLOCKED_MANUAL.equals(oldProxyMode))) {
        // change configuration only if we have a transition
        if (!oldProxyMode.equals(proxyMode)) {
          // NEXUS-3552: Tricking the config framework, we are making this applied _without_ making
          // configuration
          // dirty
          if (ProxyMode.BLOCKED_AUTO.equals(proxyMode) || ProxyMode.BLOCKED_AUTO.equals(oldProxyMode)) {
            getExternalConfiguration(false).setProxyMode(proxyMode);

            if (isDirty()) {
              // we are dirty, then just set same value in the "changed" one too
              getExternalConfiguration(true).setProxyMode(proxyMode);
            }
          }
          else {
            // this makes it dirty if it was not dirty yet, but this is the intention too
            getExternalConfiguration(true).setProxyMode(proxyMode);
          }
        }

        // setting the time to retain remote status, depending on proxy mode
        // if not blocked_auto, just use default as it was the case before AutoBlock
        if (ProxyMode.BLOCKED_AUTO.equals(proxyMode)) {
          if (!(this.remoteStatusRetainTimeSequence instanceof FibonacciNumberSequence)) {
            // take the timeout * 2 as initial step
            long initialStep = getRemoteConnectionSettings().getConnectionTimeout() * 2L;

            // make it a fibonacci one
            this.remoteStatusRetainTimeSequence = new FibonacciNumberSequence(initialStep);

            // make it step one
            this.remoteStatusRetainTimeSequence.next();

            // ping the monitor thread
            if (this.repositoryStatusCheckerThread != null) {
              this.repositoryStatusCheckerThread.interrupt();
            }
          }
        }
        else {
          this.remoteStatusRetainTimeSequence = new ConstantNumberSequence(REMOTE_STATUS_RETAIN_TIME);
        }

        // if this is proxy
        // and was !shouldProxy() and the new is shouldProxy()
        if (proxyMode != null && proxyMode.shouldProxy() && !oldProxyMode.shouldProxy()) {
          // NEXUS-4410: do this only when we are going BLOCKED_MANUAL -> ALLOW transition
          // In case of Auto unblocking, do not perform purge!
          if (!oldProxyMode.shouldAutoUnblock()) {
            if (log.isDebugEnabled()) {
              log.debug("We have a BLOCKED_MANUAL -> ALLOW transition, purging NFC");
            }

            getNotFoundCache().purge();
          }

          resetRemoteStatus();
        }

        if (sendNotification) {
          // this one should be fired _always_
          eventBus().post(new RepositoryEventProxyModeSet(this, oldProxyMode, proxyMode, cause));

          if (proxyMode != null && !proxyMode.equals(oldProxyMode)) {
            // this one should be fired on _transition_ only
            eventBus().post(new RepositoryEventProxyModeChanged(this, oldProxyMode, proxyMode, cause));
          }
        }
      }
    }
  }

  @Override
  public void setProxyMode(ProxyMode proxyMode) {
    setProxyMode(proxyMode, true, null);
  }

  /**
   * This method should be called by AbstractProxyRepository and it's descendants only. Since this method modifies
   * the
   * ProxyMode property of this repository, and this property is part of configuration, this call will result in
   * configuration flush too (potentially saving any other unsaved changes)!
   */
  protected void autoBlockProxying(Throwable cause) {
    // depend of proxy mode
    ProxyMode oldProxyMode = getProxyMode();

    // Detect do we deal with S3 remote peer, those are not managed/autoblocked, since we have no
    // proper means using HTTP only to detect the issue.
    {
      RemoteRepositoryStorage remoteStorage = getRemoteStorage();

      /**
       * Special case here to handle Amazon S3 storage. Problem is that if we do a request against a folder, a 403
       * will always be returned, as S3 doesn't support that. So we simple check if its s3 and if so, we ignore
       * the fact that 403 was returned (only in regards to auto-blocking, rest of system will still handle 403
       * response as expected)
       */
      try {
        if (remoteStorage instanceof AbstractHTTPRemoteRepositoryStorage
            && ((AbstractHTTPRemoteRepositoryStorage) remoteStorage).isRemotePeerAmazonS3Storage(this)
            && cause instanceof RemoteAccessDeniedException) {
          log.debug(
              "Not autoblocking repository id " + getId() + "since this is Amazon S3 proxy repo");
          return;
        }
      }
      catch (StorageException e) {
        // This shouldn't occur, since we are just checking the context
        log.debug("Unable to validate if proxy repository id " + getId() + "is Amazon S3", e);
      }
    }

    // invalidate remote status
    final String unavailableReason = parseRemoteUnavailableReason(cause);
    if (unavailableReason == null) {
      setRemoteStatus(RemoteStatus.UNAVAILABLE, cause);
    }
    else {
      setRemoteStatus(new RemoteStatus(RemoteStatus.Type.UNAVAILABLE, unavailableReason), cause);
    }

    // do we need to do anything at all?
    boolean autoBlockActive = isAutoBlockActive();

    // nag only here
    if (!ProxyMode.BLOCKED_AUTO.equals(oldProxyMode)) {
      StringBuilder sb = new StringBuilder();

      sb.append("Remote peer of proxy repository " + RepositoryStringUtils.getHumanizedNameString(this)
          + " threw a " + cause.getClass().getName() + " exception.");

      if (cause instanceof RemoteAccessException) {
        sb.append(" Please set up authorization information for this repository.");
      }
      else if (cause instanceof StorageException) {
        sb.append(" Connection/transport problems occured while connecting to remote peer of the repository.");
      }

      // nag about autoblock if needed
      if (autoBlockActive) {
        sb.append(" Auto-blocking this repository to prevent further connection-leaks and known-to-fail outbound"
            + " connections until administrator fixes the problems, or Nexus detects remote repository as healthy.");
      }

      // log the event
      if (log.isDebugEnabled()) {
        log.warn(sb.toString(), cause);
      }
      else {
        sb.append(" - Cause(s): ").append(cause.getMessage());

        Throwable c = cause.getCause();

        while (c != null) {
          sb.append(" > ").append(c.getMessage());

          c = c.getCause();
        }

        log.warn(sb.toString());
      }
    }

    // autoblock if needed (above is all about nagging)
    if (autoBlockActive) {
      if (oldProxyMode != null) {
        setProxyMode(ProxyMode.BLOCKED_AUTO, true, cause);
      }

      // NEXUS-3552: Do NOT save configuration, just make it applied (see setProxyMode() how it is done)
      // save configuration only if we made a transition, otherwise no save is needed
      // if ( oldProxyMode != null && !oldProxyMode.equals( ProxyMode.BLOCKED_AUTO ) )
      // {
      // try
      // {
      // // NEXUS-3552: Do NOT save configuration, just make it applied
      // getApplicationConfiguration().saveConfiguration();
      // }
      // catch ( IOException e )
      // {
      // log.warn(
      // "Cannot save configuration after AutoBlocking repository \"" + getName() + "\" (id=" + getId()
      // + ")", e );
      // }
      // }
    }
  }

  /**
   * This method should be called by AbstractProxyRepository and it's descendants only. Since this method modifies
   * the
   * ProxyMode property of this repository, and this property is part of configuration, this call will result in
   * configuration flush too (potentially saving any other unsaved changes)!
   */
  protected void autoUnBlockProxying() {
    setRemoteStatus(RemoteStatus.AVAILABLE, null);

    ProxyMode oldProxyMode = getProxyMode();

    if (oldProxyMode.shouldAutoUnblock()) {
      // log the event
      log.warn(
          String.format(
              "Remote peer of proxy repository %s detected as healthy, un-blocking the proxy repository (it was AutoBlocked by Nexus).",
              RepositoryStringUtils.getHumanizedNameString(this)));

      setProxyMode(ProxyMode.ALLOW, true, null);
    }

    // NEXUS-3552: Do NOT save configuration, just make it applied (see setProxyMode() how it is done)
    // try
    // {
    // getApplicationConfiguration().saveConfiguration();
    // }
    // catch ( IOException e )
    // {
    // log.warn(
    // "Cannot save configuration after AutoBlocking repository \"" + getName() + "\" (id=" + getId() + ")", e );
    // }
  }

  /**
   * Best effort to extract reason why remote is not available.
   *
   * @param cause cause why the remote is not available (can be null)
   * @return parsed reason or null if reason could not be parsed
   */
  protected String parseRemoteUnavailableReason(final Throwable cause) {
    if (cause == null) {
      return null;
    }
    if (cause.getCause() != null) {
      if (cause.getCause() instanceof SSLPeerUnverifiedException) {
        return "Untrusted Remote";
      }
      if (cause.getCause() instanceof SSLException) {
        return cause.getCause().getMessage();
      }
    }

    return null;
  }

  @Override
  public RepositoryStatusCheckMode getRepositoryStatusCheckMode() {
    return getExternalConfiguration(false).getRepositoryStatusCheckMode();
  }

  @Override
  public void setRepositoryStatusCheckMode(RepositoryStatusCheckMode mode) {
    getExternalConfiguration(true).setRepositoryStatusCheckMode(mode);
  }

  @Override
  public String getRemoteUrl() {
    if (getCurrentConfiguration(false).getRemoteStorage() != null) {
      return getCurrentConfiguration(false).getRemoteStorage().getUrl();
    }
    else {
      return null;
    }
  }

  @Override
  public void setRemoteUrl(String remoteUrl)
      throws RemoteStorageException
  {
    if (getRemoteStorage() != null) {
      String newRemoteUrl = remoteUrl.trim();

      String oldRemoteUrl = getRemoteUrl();

      if (!newRemoteUrl.endsWith(RepositoryItemUid.PATH_SEPARATOR)) {
        newRemoteUrl = newRemoteUrl + RepositoryItemUid.PATH_SEPARATOR;
      }

      getRemoteStorage().validateStorageUrl(newRemoteUrl);

      getCurrentConfiguration(true).getRemoteStorage().setUrl(newRemoteUrl);

      if ((StringUtils.isEmpty(oldRemoteUrl) && StringUtils.isNotEmpty(newRemoteUrl))
          || (StringUtils.isNotEmpty(oldRemoteUrl) && !oldRemoteUrl.equals(newRemoteUrl))) {
        this.remoteUrlChanged = true;
      }
    }
    else {
      throw new RemoteStorageException("No remote storage set on repository \"" + getName() + "\" (ID=\""
          + getId() + "\"), cannot set remoteUrl!");
    }
  }

  /**
   * Gets the item max age in (in minutes).
   *
   * @return the item max age in (in minutes)
   */
  @Override
  public int getItemMaxAge() {
    return getExternalConfiguration(false).getItemMaxAge();
  }

  /**
   * Sets the item max age in (in minutes).
   *
   * @param itemMaxAge the new item max age in (in minutes).
   */
  @Override
  public void setItemMaxAge(int itemMaxAge) {
    getExternalConfiguration(true).setItemMaxAge(itemMaxAge);
  }

  protected void resetRemoteStatus() {
    remoteStatusUpdated = 0;
  }

  /**
   * Is checking in progress?
   */
  private volatile boolean _remoteStatusChecking = false;

  @Override
  public RemoteStatus getRemoteStatus(ResourceStoreRequest request, boolean forceCheck) {
    // if the last known status is old, simply reset it
    if (forceCheck || System.currentTimeMillis() - remoteStatusUpdated > REMOTE_STATUS_RETAIN_TIME) {
      remoteStatus = RemoteStatus.UNKNOWN;
    }

    if (getProxyMode() != null && RemoteStatus.UNKNOWN.equals(remoteStatus) && !_remoteStatusChecking) {
      // check for thread and go check it
      _remoteStatusChecking = true;

      poolManager.getRepositoryThreadPool(this).submit(new RemoteStatusUpdateCallable(request));
    }

    return remoteStatus;
  }

  private void setRemoteStatus(RemoteStatus remoteStatus, Throwable cause) {
    this.remoteStatus = remoteStatus;

    // UNKNOWN does not count
    if (RemoteStatus.AVAILABLE.equals(remoteStatus) || RemoteStatus.UNAVAILABLE.equals(remoteStatus)) {
      this.remoteStatusUpdated = System.currentTimeMillis();
    }
  }

  @Override
  public RemoteStorageContext getRemoteStorageContext() {
    return remoteStorageContext;
  }

  @Override
  public RemoteConnectionSettings getRemoteConnectionSettings() {
    return getRemoteStorageContext().getRemoteConnectionSettings();
  }

  @Override
  public void setRemoteConnectionSettings(RemoteConnectionSettings settings) {
    getRemoteStorageContext().setRemoteConnectionSettings(settings);
  }

  @Override
  public RemoteAuthenticationSettings getRemoteAuthenticationSettings() {
    return getRemoteStorageContext().getRemoteAuthenticationSettings();
  }

  @Override
  public void setRemoteAuthenticationSettings(RemoteAuthenticationSettings settings) {
    getRemoteStorageContext().setRemoteAuthenticationSettings(settings);

    if (getProxyMode() != null && getProxyMode().shouldAutoUnblock()) {
      // perm changes? retry if autoBlocked
      setProxyMode(ProxyMode.ALLOW);
    }
  }

  @Override
  public RemoteRepositoryStorage getRemoteStorage() {
    return remoteStorage;
  }

  @Override
  public void setRemoteStorage(RemoteRepositoryStorage remoteStorage) {
    this.remoteStorage = remoteStorage;

    if (remoteStorage == null) {
      getCurrentConfiguration(true).setRemoteStorage(null);
    }
    else {
      if (getCurrentConfiguration(true).getRemoteStorage() == null) {
        getCurrentConfiguration(true).setRemoteStorage(new CRemoteStorage());
      }

      getCurrentConfiguration(true).getRemoteStorage().setProvider(remoteStorage.getProviderId());

      setWritePolicy(RepositoryWritePolicy.READ_ONLY);
    }
  }

  @Override
  public AbstractStorageItem doCacheItem(AbstractStorageItem item)
      throws LocalStorageException
  {
    AbstractStorageItem result = null;

    try {
      if (log.isDebugEnabled()) {
        log.debug(
            "Caching item " + item.getRepositoryItemUid().toString() + " in local storage of repository.");
      }

      final RepositoryItemUidLock itemLock = item.getRepositoryItemUid().getLock();

      itemLock.lock(Action.create);

      final Action action;

      try {
        action = getResultingActionOnWrite(item.getResourceStoreRequest());

        getLocalStorage().storeItem(this, item);

        removeFromNotFoundCache(item.getResourceStoreRequest());

        // we swapped the remote item with the one from local storage
        // using this method below, we ensure that we get a "wrapped"
        // content locator that will keel shared-lock on the content
        // until being fully read
        result = doRetrieveLocalItem(item.getResourceStoreRequest());

      }
      finally {
        itemLock.unlock();
      }

      result.getItemContext().setParentContext(item.getItemContext());

      if (Action.create.equals(action)) {
        eventBus().post(new RepositoryItemEventCacheCreate(this, result));
      }
      else {
        eventBus().post(new RepositoryItemEventCacheUpdate(this, result));
      }
    }
    catch (ItemNotFoundException ex) {
      log.warn(
          "Nexus BUG in "
              + RepositoryStringUtils.getHumanizedNameString(this)
              + ", ItemNotFoundException during cache! Please report this issue along with the stack trace below!",
          ex);

      // this is a nonsense, we just stored it!
      result = item;
    }
    catch (UnsupportedStorageOperationException ex) {
      log.warn(
          "LocalStorage or repository " + RepositoryStringUtils.getHumanizedNameString(this)
              + " does not handle STORE operation, not caching remote fetched item.", ex);

      result = item;
    }

    return result;
  }

  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      StringBuilder db = new StringBuilder(request.toString());

      db.append(" :: localOnly=").append(request.isRequestLocalOnly());
      db.append(", remoteOnly=").append(request.isRequestRemoteOnly());
      db.append(", asExpired=").append(request.isRequestAsExpired());

      if (getProxyMode() != null) {
        db.append(", ProxyMode=" + getProxyMode().toString());
      }

      log.debug(db.toString());
    }

    // we have to re-set locking here explicitly, since we are going to
    // make a "salto-mortale" here, see below
    // we start with "usual" read lock, we still don't know is this hosted or proxy repo
    // if proxy, we still don't know do we have to go remote (local copy is old) or not
    // if proxy and need to go remote, we want to _protect_ ourselves from
    // serving up partial downloads...

    final RepositoryItemUid itemUid = createUid(request.getRequestPath());

    final RepositoryItemUidLock itemUidLock = itemUid.getLock();

    itemUidLock.lock(Action.read);

    try {
      if (!getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
        // we have no proxy facet, just get 'em!
        return super.doRetrieveItem(request);
      }
      else {
        // we have Proxy facet, so we want to check carefully local storage
        // Reason: a previous thread may still _downloading_ the stuff we want to
        // serve to another client, so we have to _wait_ for download, but for download
        // only.
        AbstractStorageItem localItem = null;

        if (!request.isRequestRemoteOnly()) {
          try {
            localItem = (AbstractStorageItem) super.doRetrieveItem(request);

            if (localItem != null && !request.isRequestAsExpired() && !isOld(localItem)) {
              // local copy is just fine, so, we are proxy but we have valid local copy in cache
              return localItem;
            }
          }
          catch (ItemNotFoundException e) {
            localItem = null;
          }
        }

        // we are a proxy, and we either don't have local copy or is stale, we need to
        // go remote and potentially check for new version of file, but we still don't know
        // will we actually fetch it (since aging != remote file changed!)
        // BUT, from this point on, we want to _serialize_ access, so upgrade to CREATE lock

        itemUidLock.lock(Action.create);

        try {
          // check local copy again, we were maybe blocked for a download, and we need to
          // recheck local copy after we acquired exclusive lock
          if (!request.isRequestRemoteOnly()) {
            try {
              localItem = (AbstractStorageItem) super.doRetrieveItem(request);

              if (localItem != null && !request.isRequestAsExpired() && !isOld(localItem)) {
                // local copy is just fine (downloaded by a thread holding us blocked on acquiring
                // exclusive lock)
                return localItem;
              }
            }
            catch (ItemNotFoundException e) {
              localItem = null;
            }
          }

          // this whole method happens with exclusive lock on UID
          return doRetrieveItem0(request, localItem);
        }
        finally {
          itemUidLock.unlock();
        }
      }
    }
    finally {
      itemUidLock.unlock();
    }
  }

  protected void shouldTryRemote(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException
  {
    if (request.isRequestLocalOnly()) {
      throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
          "Request is marked as local-only, remote access not allowed from %s", this));
    }
    if (getProxyMode() != null && !getProxyMode().shouldProxy()) {
      throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
          "Repository proxy-mode is %s, remote access not allowed from %s", getProxyMode(), this));
    }
  }

  protected StorageItem doRetrieveItem0(ResourceStoreRequest request, AbstractStorageItem localItem)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    AbstractStorageItem item = null;
    AbstractStorageItem remoteItem = null;

    // proxyMode and request.localOnly decides 1st
    ItemNotFoundException noRemoteAccessReason = null;
    try {
      shouldTryRemote(request);
    }
    catch (ItemNotFoundException e) {
      noRemoteAccessReason = e;
    }

    if (noRemoteAccessReason == null) {
      for (RequestStrategy strategy : getRegisteredStrategies().values()) {
        try {
          strategy.onRemoteAccess(this, request, localItem);
        }
        catch (ItemNotFoundException e) {
          noRemoteAccessReason = e;
          // escape
          break;
        }
      }
    }

    if (noRemoteAccessReason == null) {
      // we are able to go remote
      if (localItem == null || request.isRequestAsExpired() || isOld(localItem)) {
        // we should go remote coz we have no local copy or it is old
        try {
          boolean shouldGetRemote = false;

          if (localItem != null) {
            if (log.isDebugEnabled()) {
              log.debug(
                  "Item " + request.toString()
                      + " is old, checking for newer file on remote then local: "
                      + new Date(localItem.getModified()));
            }

            // check is the remote newer than the local one
            try {
              shouldGetRemote = doCheckRemoteItemExistence(localItem, request);

              if (!shouldGetRemote) {
                markItemRemotelyChecked(localItem);

                if (log.isDebugEnabled()) {
                  log.debug(
                      "No newer version of item " + request.toString() + " found on remote storage.");
                }
              }
              else {
                if (log.isDebugEnabled()) {
                  log.debug(
                      "Newer version of item " + request.toString() + " is found on remote storage.");
                }
              }

            }
            catch (RemoteStorageException ex) {
              // NEXUS-4593 HTTP status 403 should not lead to autoblock
              if (!(ex instanceof RemoteAccessDeniedException)
                  && !(ex instanceof RemoteStorageTransportException)) {
                autoBlockProxying(ex);
              }

              if (ex instanceof RemoteStorageTransportException) {
                throw ex;
              }

              // do not go remote, but we did not mark it as "remote checked" also.
              // let the user do proper setup and probably it will try again
              shouldGetRemote = false;
            }
            catch (IOException ex) {
              // do not go remote, but we did not mark it as "remote checked" also.
              // let the user do proper setup and probably it will try again
              shouldGetRemote = false;
            }
          }
          else {
            // we have no local copy of it, try to get it unconditionally
            shouldGetRemote = true;
          }

          if (shouldGetRemote) {
            // this will GET it unconditionally
            try {
              remoteItem = doRetrieveRemoteItem(request);

              if (log.isDebugEnabled()) {
                log.debug("Item " + request.toString() + " found in remote storage.");
              }
            }
            catch (StorageException ex) {
              if (ex instanceof RemoteStorageException
                  // NEXUS-4593 HTTP status 403 should not lead to autoblock
                  && !(ex instanceof RemoteAccessDeniedException)
                  && !(ex instanceof RemoteStorageTransportException)) {
                autoBlockProxying(ex);
              }

              if (ex instanceof RemoteStorageTransportException
                  || ex instanceof LocalStorageEOFException) {
                throw ex;
              }

              if (ex instanceof RemoteAccessDeniedException) {
                log.debug("Error code 403 {} obtaining {} from remote storage.", ex.getMessage(), request);
                request.getRequestContext().put("remote.accessDeniedException", ex);
              }

              remoteItem = null;

              // cleanup if any remnant is here
              try {
                if (localItem == null) {
                  deleteItem(false, request);
                }
              }
              catch (ItemNotFoundException ex1) {
                // ignore
              }
              catch (UnsupportedStorageOperationException ex2) {
                // will not happen
              }
            }
          }
          else {
            remoteItem = null;
          }
        }
        catch (ItemNotFoundException ex) {
          if (log.isDebugEnabled()) {
            log.debug("Item " + request.toString() + " not found in remote storage.");
          }

          remoteItem = null;
        }
      }

      if (localItem == null && remoteItem == null) {
        // we dont have neither one, NotFoundException
        if (log.isDebugEnabled()) {
          log.debug(
              "Item " + request.toString()
                  + " does not exist in local or remote storage, throwing ItemNotFoundException.");
        }

        throw new ItemNotFoundException(reasonFor(request, this,
            "Path %s not found in local nor in remote storage of %s", request.getRequestPath(),
            this));
      }
      else if (localItem != null && remoteItem == null) {
        // simple: we have local but not remote (coz we are offline or coz it is not newer)
        if (log.isDebugEnabled()) {
          log.debug(
              "Item " + request.toString()
                  + " does exist in local storage and is fresh, returning local one.");
        }

        item = localItem;
      }
      else {
        // the fact that remoteItem != null means we _have_ to return that one
        // OR: we had no local copy
        // OR: remoteItem is for sure newer (look above)
        item = remoteItem;
      }

    }
    else {
      // we cannot go remote
      if (localItem != null) {
        if (log.isDebugEnabled()) {
          log.debug(
              "Item " + request.toString() + " does exist locally and cannot go remote, returning local one.");
        }

        item = localItem;
      }
      else {
        if (log.isDebugEnabled()) {
          log.debug(
              "Item " + request.toString()
                  + " does not exist locally and cannot go remote, throwing ItemNotFoundException.");
        }

        throw new ItemNotFoundException(ItemNotFoundException.reasonFor(request, this,
            noRemoteAccessReason.getMessage()), noRemoteAccessReason);
      }
    }

    return item;
  }

  private void sendContentValidationEvents(ResourceStoreRequest request, List<RepositoryItemValidationEvent> events,
                                           boolean isContentValid)
  {
    if (log.isDebugEnabled() && !isContentValid) {
      log.debug("Item " + request.toString() + " failed content integrity validation.");
    }

    for (RepositoryItemValidationEvent event : events) {
      eventBus().post(event);
    }
  }

  protected void markItemRemotelyChecked(final StorageItem item)
      throws IOException, ItemNotFoundException
  {
    // remote file unchanged, touch the local one to renew it's Age
    getAttributesHandler().touchItemCheckedRemotely(System.currentTimeMillis(), item);
  }

  /**
   * Validates integrity of content of <code>item</code>. Retruns <code>true</code> if item content is valid and
   * <code>false</code> if item content is corrupted. Note that this method is called doRetrieveRemoteItem, so
   * implementation must retrieve checksum files directly from remote storage <code>
   * getRemoteStorage().retrieveItem( this, context, getRemoteUrl(), checksumUid.getPath() );
   * </code>
   */
  protected boolean doValidateRemoteItemContent(ResourceStoreRequest req, String baseUrl, AbstractStorageItem item,
                                                List<RepositoryItemValidationEvent> events)
  {
    boolean isValid = true;

    for (Map.Entry<String, ItemContentValidator> icventry : getItemContentValidators().entrySet()) {
      try {
        boolean isValidByCurrentItemContentValidator =
            icventry.getValue().isRemoteItemContentValid(this, req, baseUrl, item, events);

        if (!isValidByCurrentItemContentValidator) {
          log.info(
              String.format(
                  "Proxied item %s evaluated as INVALID during content validation (validator=%s, sourceUrl=%s)",
                  item.getRepositoryItemUid().toString(), icventry.getKey(), item.getRemoteUrl()));
        }

        isValid = isValid && isValidByCurrentItemContentValidator;
      }
      catch (StorageException e) {
        log.info(
            String.format(
                "Proxied item %s evaluated as INVALID during content validation (validator=%s, sourceUrl=%s)",
                item.getRepositoryItemUid().toString(), icventry.getKey(), item.getRemoteUrl()), e);

        isValid = false;
      }
    }

    return isValid;
  }

  /**
   * Checks for remote existence of local item.
   */
  protected boolean doCheckRemoteItemExistence(StorageItem localItem, ResourceStoreRequest request)
      throws RemoteAccessException, RemoteStorageException
  {
    if (localItem != null) {
      return getRemoteStorage().containsItem(localItem.getModified(), this, request);
    }
    else {
      return getRemoteStorage().containsItem(this, request);
    }
  }

  /**
   * Retrieves item with specified uid from remote storage according to the following retry-fallback-blacklist rules.
   * <li>Only retrieve item operation will use mirrors, other operations, like check availability and retrieve
   * checksum file, will always use repository canonical url.</li> <li>Only one mirror url will be considered before
   * retrieve item operation falls back to repository canonical url.</li> <li>Repository canonical url will never be
   * put on the blacklist.</li> <li>If retrieve item operation fails with ItemNotFound or AccessDenied error, the
   * operation will be retried with another url or original error will be reported if there are no more urls.</li>
   * <li>
   * If retrieve item operation fails with generic StorageException or item content is corrupt, the operation will be
   * retried one more time from the same url. After that, the operation will be retried with another url or original
   * error will be returned if there are no more urls.</li> <li>Mirror url will be put on the blacklist if retrieve
   * item operation from the url failed with StorageException, AccessDenied or InvalidItemContent error but the item
   * was successfully retrieve from another url.</li> <li>Mirror url will be removed from blacklist after 30
   * minutes.</li>
   * The following matrix summarises retry/blacklist behaviour
   * <p/>
   * <p/>
   *
   * <pre>
   * Error condition      Retry?        Blacklist?
   *
   * InetNotFound         no            no
   * AccessDedied         no            yes
   * InvalidContent       no            no
   * Other                yes           yes
   * </pre>
   */
  protected AbstractStorageItem doRetrieveRemoteItem(ResourceStoreRequest request)
      throws ItemNotFoundException, RemoteAccessException, StorageException
  {
    final RepositoryItemUid itemUid = createUid(request.getRequestPath());

    final RepositoryItemUidLock itemUidLock = itemUid.getLock();

    // all this remote download happens in exclusive lock
    itemUidLock.lock(Action.create);

    try {
      List<String> remoteUrls = getRemoteUrls(request);

      List<RepositoryItemValidationEvent> events = new ArrayList<>();

      Exception lastException = null;

      // flag that is true if we've seen content at all, so cleanup is needed
      boolean storageCleanupNeeded = false;
      all_urls:
      for (String remoteUrl : remoteUrls) {
        int retryCount = 1;

        if (getRemoteStorageContext() != null) {
          RemoteConnectionSettings settings = getRemoteStorageContext().getRemoteConnectionSettings();
          if (settings != null) {
            retryCount = settings.getRetrievalRetryCount();
          }
        }

        if (log.isDebugEnabled()) {
          log.debug("Using URL:" + remoteUrl + ", retryCount=" + retryCount);
        }

        // Validate the mirror URL
        try {
          getRemoteStorage().validateStorageUrl(remoteUrl);
        }
        catch (RemoteStorageException e) {
          lastException = e;

          logFailedUrl(remoteUrl, e);

          continue all_urls; // retry with next url
        }
        catch (Exception e) {
          lastException = e;

          // make it logged, this is RuntimeEx
          log.warn("Failed URL validation: {}", remoteUrl, e);

          continue all_urls; // retry with next url
        }

        for (int i = 0; i < retryCount; i++) {
          try {
            // events.clear();

            AbstractStorageItem remoteItem =
                getRemoteStorage().retrieveItem(this, request, remoteUrl);

            // we are here, RRS returned us content that we are about to cache
            storageCleanupNeeded = true;

            remoteItem = doCacheItem(remoteItem);

            if (doValidateRemoteItemContent(request, remoteUrl, remoteItem, events)) {
              sendContentValidationEvents(request, events, true);

              return remoteItem;
            }
            else {
              continue all_urls; // retry with next url
            }
          }
          catch (ItemNotFoundException e) {
            lastException = e;

            continue all_urls; // retry with next url
          }
          catch (RemoteAccessException e) {
            lastException = e;

            logFailedUrl(remoteUrl, e);

            continue all_urls; // retry with next url
          }
          catch (RemoteStorageException e) {
            // in case when we were unable to make outbound request
            // at all, do not retry
            if (e instanceof RemoteStorageTransportException) {
              throw e;
            }

            lastException = e;

            // debug, print all
            if (log.isDebugEnabled()) {
              logFailedUrl(remoteUrl, e);
            }
            // not debug, only print the message
            else {
              Throwable t = ExceptionUtils.getRootCause(e);

              if (t == null) {
                t = e;
              }

              log.error(
                  String.format(
                      "Got RemoteStorageException in proxy repository %s while retrieving remote artifact \"%s\" from URL %s, this is %s (re)try, cause: %s: %s",
                      RepositoryStringUtils.getHumanizedNameString(this), request.toString(),
                      remoteUrl, String.valueOf(i + 1), t.getClass().getName(),
                      t.getMessage()));
            }
            // do not switch url yet, obey the retries
          }
          catch (LocalStorageException e) {
            lastException = e;

            // debug, print all
            if (log.isDebugEnabled()) {
              logFailedUrl(remoteUrl, e);
            }
            // not debug, only print the message
            else {
              Throwable t = ExceptionUtils.getRootCause(e);

              if (t == null) {
                t = e;
              }

              log.error(
                  "Got LocalStorageException in proxy repository {} while caching retrieved artifact \"{}\" got from URL {}, will attempt next mirror",
                  RepositoryStringUtils.getHumanizedNameString(this),
                  request,
                  remoteUrl,
                  t
              );
            }
            // do not switch url yet, obey the retries
            // TODO: IOException _might_ be actually a fatal error (like Nx process have no perms to write to disk)
            // but also might come when caching, from inability to READ the HTTP response body (see NEXUS-5898)
            // Hence, we will retry here too, and in case of first type of IO problems no harm will be done
            // anyway, but will solve the second type of problems, where retry will be attempted
          }
          catch (RuntimeException e) {
            lastException = e;

            // make it logged, this is RuntimeEx
            log.warn("Failed URL retrieve/cache: {}", remoteUrl, e);

            continue all_urls; // retry with next url
          }

          // retry with same url
        }
      }

      // if we got here, requested item was not retrieved for some reason
      if (storageCleanupNeeded || request.isRequestRemoteOnly()) {
        sendContentValidationEvents(request, events, false);

        try {
          final StorageItem item = getLocalStorage().retrieveItem(this, request);
          if (!(item instanceof StorageCollectionItem)) {
            getLocalStorage().deleteItem(this, request);
          }
        }
        catch (ItemNotFoundException e) {
          // good, we want this item deleted
        }
        catch (UnsupportedStorageOperationException e) {
          log.warn("Unexpected Exception in " + RepositoryStringUtils.getHumanizedNameString(this), e);
        }
      }

      if (lastException instanceof StorageException) {
        throw (StorageException) lastException;
      }
      else if (lastException instanceof ItemNotFoundException) {
        throw (ItemNotFoundException) lastException;
      }

      if (storageCleanupNeeded) {
        // validation failed
        throw new ItemNotFoundException(reasonFor(request, this,
            "Path %s fetched from remote storage of %s but failed validation.", request.getRequestPath(), this));
      } else {
        // request for a directory or something else
        throw new ItemNotFoundException(reasonFor(request, this,
            "Path %s could not be fetched from remote storage of %s", request.getRequestPath(), this), lastException);
      }
    }
    finally {
      itemUidLock.unlock();
    }
  }

  protected List<String> getRemoteUrls(final ResourceStoreRequest request) {
    return Lists.newArrayList(getRemoteUrl());
  }

  private void logFailedUrl(String url, Exception e) {
    if (log.isDebugEnabled()) {
      log.debug("Failed URL: {}", url, e);
    }
  }

  /**
   * Checks if item is old with "default" maxAge.
   *
   * @param item the item
   * @return true, if it is old
   */
  protected boolean isOld(StorageItem item) {
    return isOld(getItemMaxAge(), item);
  }

  /**
   * Checks if item is old with given maxAge.
   */
  protected boolean isOld(int maxAge, StorageItem item) {
    return isOld(maxAge, item, isItemAgingActive());
  }

  protected boolean isOld(int maxAge, StorageItem item, boolean shouldCalculate) {
    if (!shouldCalculate) {
      // simply say "is old" always
      return true;
    }

    // If entire proxy cache has been invalidated, lazily expire item
    String itemInvalidationToken = item.getRepositoryItemAttributes().get(PROXY_CACHE_INVALIDATION_TOKEN_KEY);
    if (proxyCacheInvalidationToken != null && !proxyCacheInvalidationToken.equals(itemInvalidationToken)) {
      log.debug("Item treated as expired due to proxy-cache invalidation token mismatch: {} != {}; item: {}",
          proxyCacheInvalidationToken, itemInvalidationToken, item);
      // if item does not carry the current proxy cache invalidation token, then treat it as expired
      item.setExpired(true);
      item.getRepositoryItemAttributes().put(PROXY_CACHE_INVALIDATION_TOKEN_KEY, proxyCacheInvalidationToken);

      // save attributes, surrounding usage of item reloads attributes (over and over for some reason)
      try {
        getAttributesHandler().storeAttributes(item);
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
      return true;
    }

    // if item is manually expired, true
    if (item.isExpired()) {
      return true;
    }

    // a directory is not "aged"
    if (StorageCollectionItem.class.isAssignableFrom(item.getClass())) {
      return false;
    }

    // if repo is non-expirable, false
    if (maxAge < 0) {
      return false;
    }
    // else check age
    else {
      // include 0: if isOld check happens in same milli as file was checked, and maxAge is 0 => true
      return ((System.currentTimeMillis() - item.getRemoteChecked()) >= (maxAge * 60L * 1000L));
    }
  }

  private class RemoteStatusUpdateCallable
      implements Callable<Object>
  {

    private ResourceStoreRequest request;

    public RemoteStatusUpdateCallable(ResourceStoreRequest request) {
      this.request = request;
    }

    public Object call()
        throws Exception
    {
      try {
        try {
          if (!getProxyMode().shouldCheckRemoteStatus()) {
            setRemoteStatus(
                RemoteStatus.UNAVAILABLE,
                new ItemNotFoundException(reasonFor(request, AbstractProxyRepository.this,
                    "Proxy mode %s or repository %s forbids remote storage use.", getProxyMode(),
                    AbstractProxyRepository.this)));
          }
          else {
            if (isRemoteStorageReachable(request)) {
              autoUnBlockProxying();
            }
            else {
              autoBlockProxying(new ItemNotFoundException(reasonFor(request,
                  AbstractProxyRepository.this, "Remote peer of repository %s detected as unavailable.",
                  AbstractProxyRepository.this)));
            }
          }
        }
        catch (RemoteStorageException e) {
          // autoblock only when remote problems occur
          autoBlockProxying(e);
        }
      }
      finally {
        _remoteStatusChecking = false;
      }

      return null;
    }
  }

  protected boolean isRemoteStorageReachable(ResourceStoreRequest request)
      throws StorageException
  {
    return getRemoteStorage().isReachable(this, request);
  }

  // Need to allow delete for proxy repos
  @Override
  protected boolean isActionAllowedReadOnly(Action action) {
    return action.equals(Action.read) || action.equals(Action.delete);
  }

  /**
   * Beside original behavior, only add to NFC when we are not in BLOCKED mode.
   *
   * @since 2.0
   */
  @Override
  protected boolean shouldAddToNotFoundCache(final ResourceStoreRequest request) {
    boolean shouldAddToNFC = super.shouldAddToNotFoundCache(request);
    if (shouldAddToNFC) {
      shouldAddToNFC = getProxyMode() == null || getProxyMode().shouldProxy();
      if (!shouldAddToNFC && log.isDebugEnabled()) {
        log.debug(
            String.format(
                "Proxy repository '%s' is is not allowed to issue remote requests (%s), not adding path '%s' to NFC",
                getId(), getProxyMode(), request.getRequestPath()));
      }
    }
    return shouldAddToNFC;
  }

}
