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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.ETagHeaderUtils;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.MissingBlobException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.replication.PullReplicationSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;
import org.sonatype.nexus.transaction.RetryDeniedException;
import org.sonatype.nexus.validation.constraint.Url;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.io.Closeables;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED;

/**
 * A support class which implements basic payload logic; subclasses provide format-specific operations.
 *
 * @since 3.0
 */
public abstract class ProxyFacetSupport
    extends FacetSupport
    implements ProxyFacet
{
  public static final String BYPASS_HTTP_ERRORS_HEADER_NAME = "BYPASS_HTTP_ERRORS";

  public static final String BYPASS_HTTP_ERRORS_HEADER_VALUE = "true";

  private static final String PROXY_REMOTE_FETCH_SKIP_MARKER =
      "proxy.remote-fetch.skip";

  @VisibleForTesting
  static final String CONFIG_KEY = "proxy";

  @VisibleForTesting
  public static class ProxyConfig
      implements ProxyRepositoryConfiguration
  {
    @Url
    @NotNull
    public URI remoteUrl;

    /**
     * Content max-age minutes.
     */
    @NotNull
    public Integer contentMaxAge = (int) Duration.ofHours(24).toMinutes();

    /**
     * Metadata max-age minutes.
     */
    @NotNull
    public Integer metadataMaxAge = (int) Duration.ofHours(24).toMinutes();

    /**
     * Content max-age.
     */
    @Override
    public Duration getContentMaxAge() {
      return Duration.ofMinutes(contentMaxAge);
    }

    /**
     * Metadata max-age.
     */
    @Override
    public Duration getMetadataMaxAge() {
      return Duration.ofMinutes(metadataMaxAge);
    }

    /**
     * The remote URI of the proxy repository.
     */
    @Override
    public URI getRemoteURL() {
      return remoteUrl;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "remoteUrl=" + remoteUrl +
          ", contentMaxAge=" + contentMaxAge +
          '}';
    }
  }

  private static final ThreadLocal<Boolean> downloading = new ThreadLocal<>();

  private ProxyConfig config;

  private HttpClientFacet httpClient;

  private boolean remoteUrlChanged;

  protected CacheControllerHolder cacheControllerHolder;

  private Cooperation2Factory.Builder cooperationBuilder;

  private Cooperation2 proxyCooperation;

  @Override
  public ProxyRepositoryConfiguration getConfiguration() {
    return config;
  }

  /**
   * Configures content {@link Cooperation} for this proxy; a timeout of 0 means wait indefinitely.
   *
   * @param cooperationEnabled should threads attempt to cooperate when downloading resources
   * @param majorTimeout       when waiting for the main I/O request
   * @param minorTimeout       when waiting for any I/O dependencies
   * @param threadsPerKey      limits the threads waiting under each key
   * @since 3.4
   */
  @Inject
  protected void configureCooperation(
      final Cooperation2Factory cooperationFactory,
      @Nullable @Named("local") Cooperation2Factory defaultCooperationFactory,
      @Named("${nexus.proxy.clustered.cooperation.enabled:-false}") final boolean proxyClusteredCooperationEnabled,
      @Named(DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean clustered,
      @Named("${nexus.proxy.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.proxy.cooperation.majorTimeout:-0s}") final Duration majorTimeout,
      @Named("${nexus.proxy.cooperation.minorTimeout:-30s}") final Duration minorTimeout,
      @Named("${nexus.proxy.cooperation.threadsPerKey:-100}") final int threadsPerKey)
  {
    Cooperation2Factory currentCooperationFactory;
    if (clustered && !proxyClusteredCooperationEnabled) {
      if (defaultCooperationFactory == null) // will be initialized on datastore mode only
      {
        log.error("Can't select cooperation factory, fallback to default");
        currentCooperationFactory = cooperationFactory;
      }
      else {
        // keep cooperation enabled but disable distributed implementation for proxy repositories in case of issue with
        // performance based on property nexus.proxy.clustered.cooperation.enabled
        log.debug("Disable distributed cooperation for proxy repositories");
        currentCooperationFactory = defaultCooperationFactory;
      }
    }
    else {
      // automatically injected, DistributedCooperation2Factory if clustered mode enabled
      currentCooperationFactory = cooperationFactory;
    }

    this.cooperationBuilder = currentCooperationFactory.configure()
        .enabled(cooperationEnabled)
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey);
  }

  @VisibleForTesting
  void buildCooperation() {
    buildCooperation(getRepository());
  }

  @VisibleForTesting
  public void buildCooperation(final Repository repository) {
    if (cooperationBuilder != null) {
      this.proxyCooperation = cooperationBuilder.build(repository.getName() + ":proxy");
    }
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    buildCooperation(getRepository());
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, ProxyConfig.class);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, ProxyConfig.class);

    cacheControllerHolder = new CacheControllerHolder(
        new CacheController((int) config.getContentMaxAge().getSeconds(), null),
        new CacheController((int) config.getMetadataMaxAge().getSeconds(), null)
    );

    // normalize URL path to contain trailing slash
    config.remoteUrl = normalizeURLPath(config.remoteUrl);

    log.debug("Config: {}", config);
  }

  protected URI normalizeURLPath(final URI remoteURI) {
    String path = remoteURI.getPath();
    if (!path.endsWith("/")) {
      return remoteURI.resolve(remoteURI.getRawPath() + "/");
    }
    return remoteURI;
  }

  @Override
  protected void doUpdate(final Configuration configuration) throws Exception {
    // detect URL changes
    URI previousUrl = config.remoteUrl;
    super.doUpdate(configuration);
    remoteUrlChanged = !config.remoteUrl.equals(previousUrl);
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  protected void doStart() throws Exception {
    httpClient = facet(HttpClientFacet.class);

    if (remoteUrlChanged) {
      remoteUrlChanged = false;

      optionalFacet(NegativeCacheFacet.class).ifPresent((nfc) -> nfc.invalidate());
    }
  }

  @Override
  protected void doStop() throws Exception {
    httpClient = null;
  }

  @Override
  public URI getRemoteUrl() {
    return config.remoteUrl;
  }

  @Override
  public Content get(final Context context) throws IOException {
    checkNotNull(context);
    Content content = maybeGetCachedContent(context);
    boolean isReplication = PullReplicationSupport.isReplicationRequest(context);
    String format = getRepository().getFormat().getValue();
    getEventManager().post(new ProxyRequestEvent(format, isReplication));
    if (!isStale(context, content)) {
      getEventManager().post(new ProxyCacheHitEvent(format, isReplication));
      return content;
    }
    boolean remoteFetchSkipMarker = isRemoteFetchSkipMarkerEnabled(context);
    if (remoteFetchSkipMarker) {
      return content;
    }
    return get(context, content);
  }

  private boolean isRemoteFetchSkipMarkerEnabled(final Context context) {
    Object marker = context.getAttributes()
        .get(PROXY_REMOTE_FETCH_SKIP_MARKER);
    return TRUE.equals(marker);
  }

  /**
   * Attempt to retrieve from the remote using proxy co-operation
   */
  protected Content get(final Context context, @Nullable final Content staleContent) throws IOException {
    return proxyCooperation.on(() -> doGet(context, staleContent))
        .checkFunction(() -> {
          Content latestContent = maybeGetCachedContent(context);
          if (!isStale(context, latestContent)) {
            boolean isReplication = PullReplicationSupport.isReplicationRequest(context);
            getEventManager().post(new ProxyCacheHitEvent(getRepository().getFormat().getValue(), isReplication));
            return Optional.of(latestContent);
          }
          return Optional.empty();
        })
        .cooperate(getRequestKey(context));
  }

  /**
   * Is the current thread actively downloading (ie. fetch + store) from the upstream proxy?
   *
   * @since 3.16
   */
  public static boolean isDownloading() {
    return TRUE.equals(downloading.get());
  }

  /**
   * @since 3.4
   */
  protected Content doGet(final Context context, @Nullable final Content staleContent) throws IOException {
    Content remote = null, content = staleContent;

    boolean nested = isDownloading();
    try {
      if (!nested) {
        downloading.set(TRUE);
      }
      remote = fetch(context, content);
      if (remote != null) {
        content = store(context, remote);
        if (remote.equals(content)) {
          // remote wasn't stored; make reusable copy for cooperation
          content = new TempContent(remote);
        }
      }
    }
    catch (ProxyServiceException e) {
      logContentOrThrow(content, context, e.getHttpResponse().getStatusLine(), e);
    }
    catch (IOException e) {
      logContentOrThrow(content, context, null, e); // note this also takes care of RemoteBlockedIOException
    }
    catch (UncheckedIOException e) {
      logContentOrThrow(content, context, null,
          e.getCause()); // "special" path (for now) for npm and similar peculiar formats
    }
    finally {
      if (!nested) {
        downloading.remove();
      }
      if (remote != null && !remote.equals(content)) {
        Closeables.close(remote, true);
      }
    }
    return content;
  }

  /**
   * Path + query parameters provide a unique enough request key for known formats. If a format needs to add more
   * context then they should customize this method.
   *
   * @return key that uniquely identifies this upstream request from other contexts
   * @since 3.4
   */
  protected String getRequestKey(final Context context) {
    return context.getRequest().getPath() + '?' + context.getRequest().getParameters();
  }

  protected <X extends Throwable> void logContentOrThrow(
      @Nullable final Content content,
      final Context context,
      @Nullable final StatusLine statusLine,
      final X exception) throws X
  {
    String logMessage = buildLogContentMessage(content, statusLine);
    String repositoryName = context.getRepository().getName();
    String contextUrl = getUrl(context);

    if (content != null) {
      log.debug(logMessage, exception, repositoryName, contextUrl, statusLine);
    }
    else {
      if (exception instanceof RemoteBlockedIOException) {
        // trace because the blocked status of a repo is typically discoverable in the UI and other log messages
        log.trace(logMessage, exception, repositoryName, contextUrl, statusLine, exception);
      }
      else if (log.isDebugEnabled()) {
        log.warn(logMessage, exception, repositoryName, contextUrl, statusLine, exception);
      }
      else {
        log.warn(logMessage, exception, repositoryName, contextUrl, statusLine);
      }
      throw exception;
    }
  }

  @VisibleForTesting
  <X extends Throwable> String buildLogContentMessage(
      @Nullable final Content content,
      @Nullable final StatusLine statusLine)
  {
    StringBuilder message = new StringBuilder("Exception {} checking remote for update");

    if (statusLine == null) {
      message.append(", proxy repo {} failed to fetch {}");
    }
    else {
      message.append(", proxy repo {} failed to fetch {} with status line {}");
    }

    if (content == null) {
      message.append(", content not in cache.");
    }
    else {
      message.append(", returning content from cache.");
    }

    return message.toString();
  }

  @Override
  public void invalidateProxyCaches() {
    log.info("Invalidating proxy caches of {}", getRepository().getName());
    cacheControllerHolder.invalidateCaches();
  }

  private Content maybeGetCachedContent(final Context context) throws IOException {
    try {
      return getCachedContent(context);
    }
    catch (MissingBlobException e) {
      log.warn("Unable to find blob {} for {}, will check remote", e.getBlobRef(),
          getUrl(context));
      return null;
    }
    catch (RetryDeniedException e) {
      if (e.getCause() instanceof MissingBlobException) {
        log.warn("Unable to find blob {} for {}, will check remote", ((MissingBlobException) e.getCause()).getBlobRef(),
            getUrl(context));
        return null;
      }
      else {
        throw e;
      }
    }
  }

  /**
   * If we have the content cached locally already, return that along with applicable cache controller - otherwise
   * {@code null}.
   */
  @Nullable
  protected abstract Content getCachedContent(final Context context) throws IOException;

  /**
   * Store a new Payload, freshly fetched from the remote URL.
   * <p>
   * The Context indicates which component was being requested.
   *
   * @throws IOException
   * @throws InvalidContentException
   */
  protected abstract Content store(final Context context, final Content content) throws IOException;

  @Nullable
  protected Content fetch(final Context context, final Content stale) throws IOException {
    return fetch(getUrl(context), context, stale);
  }

  protected Content fetch(final String url, final Context context, @Nullable final Content stale) throws IOException {
    HttpClient client = httpClient.getHttpClient();

    checkState(config.remoteUrl.isAbsolute(),
        "Invalid remote URL '%s' for proxy repository %s, please fix your configuration", config.remoteUrl,
        getRepository().getName());
    URI uri;
    try {
      uri = config.remoteUrl.resolve(encodeUrl(url));
    }
    catch (IllegalArgumentException e) { // NOSONAR
      log.warn("Unable to resolve url. Reason: {}", e.getMessage());
      throw new BadRequestException("Invalid repository path");
    }
    HttpRequestBase request = buildFetchHttpRequest(uri, context);
    if (stale != null) {
      final DateTime lastModified = stale.getAttributes().get(Content.CONTENT_LAST_MODIFIED, DateTime.class);
      if (lastModified != null) {
        request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(lastModified.toDate()));
      }
      final String etag = stale.getAttributes().get(Content.CONTENT_ETAG, String.class);
      if (etag != null) {
        request.addHeader(HttpHeaders.IF_NONE_MATCH, ETagHeaderUtils.quote(etag));
      }
    }
    log.debug("Fetching: {}", request);
    log.debug("Fetching Request Headers: {}", Arrays.toString(request.getAllHeaders()));

    HttpResponse response = execute(context, client, request);
    log.debug("Response: {}", response);

    StatusLine status = response.getStatusLine();
    log.debug("Status: {}", status);

    mayThrowBypassHttpErrorException(response);

    final CacheInfo cacheInfo;

    try {
      cacheInfo = getCacheController(context).current();
    }
    catch (Exception e) {
      log.trace("Exception getting cache controller for context", e);
      HttpClientUtils.closeQuietly(response);
      throw e;
    }

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      log.debug("Entity: {}", entity);

      final Content result = createContent(context, response);
      result.getAttributes().set(Content.CONTENT_LAST_MODIFIED, extractLastModified(request, response));
      final Header etagHeader = response.getLastHeader(HttpHeaders.ETAG);
      result.getAttributes()
          .set(Content.CONTENT_ETAG, etagHeader == null ? null : ETagHeaderUtils.extract(etagHeader.getValue()));

      result.getAttributes().set(CacheInfo.class, cacheInfo);
      return result;
    }

    try {
      if (status.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
        checkState(stale != null, "Received 304 without conditional GET (bad server?) from %s", uri);
        indicateVerified(context, stale, cacheInfo);
      }
      mayThrowProxyServiceException(response);
    }
    finally {
      HttpClientUtils.closeQuietly(response);
    }

    return null;
  }

  protected String encodeUrl(final String url) throws UnsupportedEncodingException { //NOSONAR
    // some formats can use special characters in url
    // override this method if necessary
    return url;
  }

  /**
   * Create {@link Content} out of HTTP response.
   */
  protected Content createContent(final Context context, final HttpResponse response) {
    return new Content(new HttpEntityPayload(response, response.getEntity()));
  }

  /**
   * May throw {@link ProxyServiceException} based on response statuses.
   */
  protected void mayThrowProxyServiceException(final HttpResponse httpResponse) {
    final StatusLine status = httpResponse.getStatusLine();
    if (HttpStatus.SC_UNAUTHORIZED == status.getStatusCode()
        || HttpStatus.SC_PAYMENT_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_INTERNAL_SERVER_ERROR <= status.getStatusCode()) {
      throw new ProxyServiceException(httpResponse);
    }
  }

  private void mayThrowBypassHttpErrorException(final HttpResponse httpResponse) {
    final StatusLine status = httpResponse.getStatusLine();
    if (httpResponse.containsHeader(BYPASS_HTTP_ERRORS_HEADER_NAME)) {
      log.debug("Bypass http error: {}", status);
      ListMultimap<String, String> headers = ArrayListMultimap.create();
      headers.put(BYPASS_HTTP_ERRORS_HEADER_NAME, BYPASS_HTTP_ERRORS_HEADER_VALUE);
      HttpClientUtils.closeQuietly(httpResponse);
      throw new BypassHttpErrorException(status.getStatusCode(), status.getReasonPhrase(), headers);
    }
  }

  /**
   * Execute http client request.
   */
  protected HttpResponse execute(final Context context, final HttpClient client, final HttpRequestBase request)
      throws IOException
  {
    return client.execute(request);
  }

  /**
   * Builds the {@link HttpRequestBase} for a particular set of parameters (mapping to GET by default).
   */
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context) {
    return new HttpGet(uri);
  }

  /**
   * Extract Last-Modified date from response if possible, or {@code null}.
   */
  @Nullable
  private DateTime extractLastModified(final HttpRequestBase request, final HttpResponse response) {
    final Header lastModifiedHeader = response.getLastHeader(HttpHeaders.LAST_MODIFIED);
    if (lastModifiedHeader != null) {
      try {
        return new DateTime(DateUtils.parseDate(lastModifiedHeader.getValue()).getTime());
      }
      catch (Exception ex) {
        log.warn("Could not parse date '{}' received from {}; using system current time as item creation time",
            lastModifiedHeader, request.getURI());
      }
    }
    return null;
  }

  /**
   * Refresh the asset's cache status because the upstream server has indicated that the content has not changed.
   */
  protected abstract void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException;

  /**
   * Provide the URL of the content relative to the repository root.
   */
  protected abstract String getUrl(@Nonnull final Context context);

  /**
   * Get the appropriate cache controller for the type of content being requested. Must never return {@code null}.
   */
  @Nonnull
  protected CacheController getCacheController(@Nonnull final Context context) {
    return cacheControllerHolder.getContentCacheController();
  }

  private boolean isStale(final Context context, final Content content) {
    if (content == null) {
      // not in cache, consider it stale
      return true;
    }

    final CacheInfo cacheInfo = content.getAttributes().get(CacheInfo.class);

    if (isNull(cacheInfo)) {
      log.warn("CacheInfo missing for {}, assuming stale content.", content);
      return true;
    }
    return getCacheController(context).isStale(cacheInfo);
  }

  /**
   * @return number of threads cooperating per request-key.
   */
  @VisibleForTesting
  Map<String, Integer> getThreadCooperationPerRequest() {
    return proxyCooperation.getThreadCountPerKey();
  }

  /**
   * Internal exception thrown when resolving of tarball name to package version using package metadata fails.
   *
   * @see #getUrl(Context)
   * @see #fetch(Context, Content)
   */
  protected static class NonResolvablePackageException
      extends RuntimeException
  {
    private static final long serialVersionUID = 4744330472156130441L;

    public NonResolvablePackageException(final String message) {
      super(message);
    }
  }
}
