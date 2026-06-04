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
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.ValidationException;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryCacheSyncTokenEvent;
import org.sonatype.nexus.outbound.context.OutboundRequestContext;
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
import org.sonatype.nexus.repository.firewall.FirewallHeaderProvider;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.OutboundRequestMetricRecorder;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.replication.PullReplicationSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.payloads.HeaderOnlyPayload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;
import org.sonatype.nexus.transaction.RetryDeniedException;
import org.sonatype.nexus.validation.constraint.Url;
import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Closeables;
import com.google.common.net.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static org.sonatype.nexus.logging.task.TaskLoggingMarkers.OUTBOUND_REQUESTS_LOG_ONLY;

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

  public static final String PROXY_REMOTE_FETCH_SKIP_MARKER =
      "proxy.remote-fetch.skip";

  public static final String MISSING_BLOB_SKIP_NEGATIVE_CACHE =
      "proxy.missing-blob.skip-negative-cache";

  public static final String ALTERNATIVE_URLS_PATTERN = "<(.*?)>";

  public static final String PROXY_THROTTLED_ANALYTICS_MARKED = "nexus.analytics.proxy_throttled_requests.marked";

  @VisibleForTesting
  static final String CONFIG_KEY = "proxy";

  public static final String X_FIREWALL_USER_AGENT = "X-Firewall-User-Agent";

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
     * Preserve encoded characters in URLs when proxying to the remote repository.
     * When true, preserves encoded characters like %2B (plus), %23 (hash), and %20 (space).
     * When false (default), uses standard encoding that preserves literal + characters.
     * Only used when the feature flag nexus.proxy.urlEncodingMode.enabled is true.
     */
    @NotNull
    public boolean preserveEncodedCharacters = false;

    /**
     * Enable Yellowfin proxy routing for Firewall Pro.
     * When true, requests are routed through the Yellowfin proxy service.
     */
    public boolean yellowfinEnabled = false;

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

  private EscapeHelper escapeHelper;

  private EncodingHelper encodingHelper;

  private boolean urlEncodingModeEnabled;

  private static final String CTX_REQ_STOPWATCH = "request.stopwatch";

  private static final String CTX_REQ_URI = "request.uri";

  protected static final String HTTP_RESPONSE = "request.http_response";

  protected static final String HTTP_CONTEXT = "request.http_context";

  static final String HTTPCLIENT_OUTBOUND_REQ_LOGGER_NAME = "outboundRequests";

  static final String HTTPCLIENT_OUTBOUND_LOGGER_NAME = "org.sonatype.nexus.httpclient.outbound";

  private final Logger outboundReqLog = LoggerFactory.getLogger(HTTPCLIENT_OUTBOUND_REQ_LOGGER_NAME);

  private final Logger outboundLog = LoggerFactory.getLogger(HTTPCLIENT_OUTBOUND_LOGGER_NAME);

  @Override
  public ProxyRepositoryConfiguration getConfiguration() {
    return config;
  }

  /**
   * Configures content {@link Cooperation} for this proxy; a timeout of 0 means wait indefinitely.
   *
   * @param cooperationEnabled should threads attempt to cooperate when downloading resources
   * @param majorTimeout when waiting for the main I/O request
   * @param minorTimeout when waiting for any I/O dependencies
   * @param threadsPerKey limits the threads waiting under each key
   * @since 3.4
   */
  @Autowired
  protected void configureCooperation(
      final Cooperation2Factory cooperationFactory,
      @Value("${nexus.proxy.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.proxy.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.proxy.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.proxy.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    this.cooperationBuilder = cooperationFactory.configure()
        .enabled(cooperationEnabled)
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey);
  }

  @Autowired
  protected void configureUrlEscapeRules(
      @Nullable @Value("${nexus.proxy.url.escape.rules:#{null}}") final String urlEscapeRulesConfig)
  {
    this.escapeHelper = new EscapeHelper(urlEscapeRulesConfig);
  }

  @Autowired
  protected void configureUrlEncodingMode(
      @Value("${nexus.proxy.urlEncodingMode.enabled:false}") final boolean enabled)
  {
    this.urlEncodingModeEnabled = enabled;
  }

  @Autowired
  @Nullable
  private ThrottlerInterceptor throttlerInterceptor;

  @Autowired
  @Nullable
  private GracePeriodInterceptor gracePeriodInterceptor;

  @Autowired
  @Nullable
  private FirewallHeaderProvider firewallHeaderProvider;

  private AntiSsrfService antiSsrfService;

  @Autowired
  protected void configureAntiSsrfService(final AntiSsrfService antiSsrfService) {
    this.antiSsrfService = checkNotNull(antiSsrfService);
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

    String storedToken =
        repositoryAttributeService().getRepositoryAttribute(getRepository(), CACHE_TOKEN_ATTRIBUTE, null);

    cacheControllerHolder = new CacheControllerHolder(
        new CacheController((int) config.getContentMaxAge().getSeconds(), null),
        new CacheController((int) config.getMetadataMaxAge().getSeconds(), storedToken));

    // normalize URL path to contain trailing slash
    config.remoteUrl = normalizeURLPath(config.remoteUrl);

    // Initialize EncodingHelper only if feature is enabled
    if (urlEncodingModeEnabled) {
      this.encodingHelper = new EncodingHelper(escapeHelper, config.preserveEncodedCharacters);
      log.debug("URL encoding mode enabled. Preserve encoded characters: {}", config.preserveEncodedCharacters);
    }
    else {
      this.encodingHelper = null;
      log.debug("URL encoding mode feature disabled, using legacy behavior");
    }

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
    if (gracePeriodInterceptor != null &&
        gracePeriodInterceptor.isInGracePeriod() &&
        throttlerInterceptor != null &&
        throttlerInterceptor.shouldBlock()) {
      sendProxyThrottledRequestEventIfNeeded(context, false);
    }
    if (gracePeriodInterceptor != null &&
        !gracePeriodInterceptor.isInGracePeriod() &&
        throttlerInterceptor != null &&
        throttlerInterceptor.shouldBlock()) {
      context.getAttributes().set(PROXY_REMOTE_FETCH_SKIP_MARKER, TRUE);
      sendProxyThrottledRequestEventIfNeeded(context, true);
      return content;
    }
    return get(context, content);
  }

  private void sendProxyThrottledRequestEventIfNeeded(final Context context, boolean isBlocked) {
    if (!context.getAttributes().contains(PROXY_THROTTLED_ANALYTICS_MARKED)) {
      getEventManager().post(new ProxyThrottledRequestEvent(isBlocked));
    }
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

    try {
      if (isHeadRequest(context) && content != null && !isStale(context, content)) {
        log.debug("HEAD request - returning cached metadata");
        return content;
      }
    }
    catch (Exception e) {
      log.warn("Error checking stale content for HEAD request, proceeding with normal flow", e);
    }
    boolean nested = isDownloading();
    try {
      if (!nested) {
        downloading.set(TRUE);
      }
      context.setAttribute(CTX_REQ_STOPWATCH, Stopwatch.createStarted());
      remote = fetch(context, content);
      if (remote != null) {
        // HEAD requests return metadata immediately without storing/caching
        if (isHeadRequest(context) && shouldSkipStoreForHead()) {
          log.debug("HEAD request - returning remote metadata without caching");
          content = remote;
        }
        else {
          content = store(context, remote);
          if (remote.equals(content)) {
            // remote wasn't stored; make reusable copy for cooperation
            content = new TempContent(remote);
          }
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
      printOutboundLogging(context);
    }
    return content;
  }

  private void printOutboundLogging(final Context context) {
    try {
      Stopwatch stopwatch = context.getAttribute(CTX_REQ_STOPWATCH, Stopwatch.class);
      if (stopwatch == null) {
        return;
      }

      HttpContext httpContext = context.getAttribute(HTTP_CONTEXT, HttpContext.class);

      String requestUri = context.getRequest().getPath();
      if (httpContext != null) {
        URI uri = (URI) httpContext.getAttribute(CTX_REQ_URI);
        if (uri != null) {
          requestUri = uri.toString();
        }
        outboundLog.debug("Request for {} took {} milliseconds", requestUri,
            stopwatch.elapsed(TimeUnit.MILLISECONDS));
      }

      if (OutboundRequestContext.getFormattedString() == null) {
        return;
      }

      SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z");
      String formattedString = OutboundRequestContext.getFormattedString();
      String newFormattedString =
          formattedString.replace(OutboundRequestContext.ELAPSED_TIME_PLACEHOLDER,
              String.valueOf(stopwatch.elapsed(TimeUnit.MILLISECONDS)))
              .replace(OutboundRequestContext.TIMESTAMP_PLACEHOLDER, dateFormat.format(new Date()));
      outboundReqLog.info(OUTBOUND_REQUESTS_LOG_ONLY, "{}", newFormattedString);
    }
    finally {
      OutboundRequestContext.remove(); // clear thread local
    }
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

  /**
   * Check if the current request is a HEAD request.
   *
   * @param context the request context
   * @return true if this is a HEAD request, false for GET or other methods
   */
  private boolean isHeadRequest(final Context context) {
    return context.getRequest() != null &&
        HttpMethods.HEAD.equalsIgnoreCase(context.getRequest().getAction());
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
      else if (exception instanceof BypassHttpErrorException) {
        // debug because these are expected exceptions (e.g., 401 Unauthorized from Docker Hub)
        if (log.isDebugEnabled()) {
          log.debug(logMessage, exception, repositoryName, contextUrl, statusLine, exception);
        }
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

    // Post event to synchronize cache token across nodes
    postCacheTokenEvent(getRepository(), cacheControllerHolder.getContentCacheController().current().getCacheToken());
  }

  private Content maybeGetCachedContent(final Context context) throws IOException {
    try {
      return getCachedContent(context);
    }
    catch (MissingBlobException e) {
      log.warn("Unable to find blob {} for {}, will check remote", e.getBlobRef(),
          getUrl(context));

      context.getAttributes().set(MISSING_BLOB_SKIP_NEGATIVE_CACHE, TRUE);
      return null;
    }
    catch (RetryDeniedException e) {
      if (e.getCause() instanceof MissingBlobException) {
        log.warn("Unable to find blob {} for {}, will check remote", ((MissingBlobException) e.getCause()).getBlobRef(),
            getUrl(context));

        context.getAttributes().set(MISSING_BLOB_SKIP_NEGATIVE_CACHE, TRUE);
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
    if (url == null) {
      log.debug(
          "Unable to determine remote URL for request path: {}. The format-specific getUrl() method returned null, indicating the request path is not valid or requires metadata that has not been fetched yet.",
          context.getRequest().getPath());
      return null;
    }

    HttpClient client = httpClient.getHttpClient();

    checkState(config.remoteUrl.isAbsolute(),
        "Invalid remote URL '%s' for proxy repository %s, please fix your configuration", config.remoteUrl,
        getRepository().getName());

    URI uri;
    try {
      // Handle absolute URLs (e.g., NPM tarball URLs from package metadata)
      // These already contain full URL and should not be encoded or resolved
      if (url.contains("://")) {
        uri = new URI(url);
      }
      // Check if feature is enabled before using EncodingHelper
      else if (urlEncodingModeEnabled && encodingHelper != null) {
        // New two-stage encoding (feature enabled)
        String baseEncoded = encodingHelper.encodeUrlSegments(url);
        String finalEncoded = encodeUrl(baseEncoded);
        uri = config.remoteUrl.resolve(finalEncoded);
      }
      else {
        // Legacy behavior (feature disabled or not configured)
        uri = config.remoteUrl.resolve(encodeUrl(url));
      }
    }
    catch (IllegalArgumentException e) { // NOSONAR
      log.warn("Unable to resolve url. Reason: {}", e.getMessage());
      throw new BadRequestException("Invalid repository path");
    }
    catch (URISyntaxException e) { // NOSONAR
      log.warn("Invalid absolute URL: {}. Reason: {}", url, e.getMessage());
      throw new BadRequestException("Invalid repository path");
    }
    validateNotPrivateNetwork(uri);

    HttpRequestBase request = buildFetchHttpRequest(uri, context, stale);

    // DEBUG-level logging for troubleshooting
    log.debug("ProxyFacet: Fetching from upstream: {} - Repository: {}",
        uri, getRepository().getName());

    log.debug("Fetching: {}", request);
    log.debug("Fetching Request Headers: {}", Arrays.toString(request.getAllHeaders()));

    HttpResponse response = execute(context, client, request);
    context.setAttribute(HTTP_RESPONSE, response);
    log.debug("Response: {}", response);

    StatusLine status = response.getStatusLine();
    log.debug("Status: {}", status);

    // DEBUG-level response logging
    log.debug("ProxyFacet: Response from upstream: {} - Status: {} - Repository: {}",
        uri, status.getStatusCode(), getRepository().getName());

    mayThrowBypassHttpErrorException(context, response);

    final CacheInfo cacheInfo;

    try {
      cacheInfo = getCacheController(context).current();
    }
    catch (Exception e) {
      log.trace("Exception getting cache controller for context", e);
      HttpClientUtils.closeQuietly(response);
      throw e;
    }

    boolean isUnmodified = isNotModified(response, stale);

    if (status.getStatusCode() == HttpStatus.SC_OK && !isUnmodified) {
      return buildOkResponseContent(context, request, response, cacheInfo);
    }

    try {
      return build3xxResponseContent(context, uri, stale, response, cacheInfo, isUnmodified);
    }
    finally {
      HttpClientUtils.closeQuietly(response);
    }
  }

  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context, final Content stale) {
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

    if (firewallHeaderProvider != null) {
      request.addHeader(X_FIREWALL_USER_AGENT, firewallHeaderProvider.originatingUserAgent(context.getRequest()));
    }
    return request;
  }

  protected Content buildOkResponseContent(
      final Context context,
      final HttpRequestBase request,
      final HttpResponse response,
      final CacheInfo cacheInfo)
  {
    HttpEntity entity = response.getEntity();
    log.debug("Entity: {}", entity);

    // INFO-level: Log key response headers for production visibility
    StringBuilder headerSummary = new StringBuilder();
    Header[] allHeaders = response.getAllHeaders();
    for (Header header : allHeaders) {
      String name = header.getName().toLowerCase();
      // Log important headers: etag, ETag, content-type, content-length, cache-control
      if (name.equals("etag") || name.equals("content-type") || name.equals("content-length") ||
          name.equals("cache-control") || name.equals("last-modified")) {
        if (headerSummary.length() > 0) {
          headerSummary.append(", ");
        }
        headerSummary.append(header.getName()).append(": ").append(header.getValue());
      }
    }
    log.debug("ProxyFacet: Key response headers from upstream - {} - Repository: {}",
        headerSummary.toString(), getRepository().getName());

    // Diagnostic logging to debug ETag extraction issues (DEBUG level - all headers)
    if (log.isDebugEnabled()) {
      log.debug("ProxyFacet - ALL Response Headers:");
      for (Header header : allHeaders) {
        log.debug("  Header: {} = {}", header.getName(), header.getValue());
      }
    }

    // Azure Blob Storage (used by NuGet.org) may return "etag" (lowercase) or "ETag" (standard)
    // HTTP header names are case-insensitive per RFC 7230, but Apache HttpClient lookup is case-sensitive
    // Try standard case first, then lowercase
    Header etagHeader = response.getLastHeader(HttpHeaders.ETAG);
    boolean foundLowercase = false;
    if (etagHeader == null) {
      etagHeader = response.getLastHeader("etag");
      if (etagHeader != null) {
        foundLowercase = true;
        log.debug("Found lowercase 'etag' header instead of standard 'ETag'");
      }
    }

    final String etag = etagHeader != null ? ETagHeaderUtils.extract(etagHeader.getValue()) : null;

    // DEBUG-level logging for ETag extraction result
    if (etag != null) {
      log.debug("ProxyFacet: ETag extracted from upstream - Value: {} - Lowercase: {} - Repository: {}",
          etag, foundLowercase, getRepository().getName());
    }
    else {
      log.debug("ProxyFacet: NO ETag found in upstream response - Repository: {}",
          getRepository().getName());
    }

    if (log.isDebugEnabled()) {
      log.debug("ProxyFacet - ETag Header Object: {}", etagHeader);
      log.debug("ProxyFacet - Extracted ETag value: {}", etag);
    }

    final Content content = createContent(context, response);
    content.getAttributes().set(Content.CONTENT_LAST_MODIFIED, extractLastModified(request, response));
    content.getAttributes().set(Content.CONTENT_ETAG, etag);
    content.getAttributes().set(CacheInfo.class, cacheInfo);

    if (log.isDebugEnabled()) {
      log.debug("ProxyFacet - Content ETAG attribute set to: {}",
          content.getAttributes().get(Content.CONTENT_ETAG, String.class));
    }

    return content;
  }

  protected Content build3xxResponseContent(
      final Context context,
      final URI uri,
      final Content stale,
      final HttpResponse response,
      final CacheInfo cacheInfo,
      final boolean isUnmodified) throws ProxyServiceException, IOException
  {
    StatusLine status = response.getStatusLine();
    if (status.getStatusCode() == HttpStatus.SC_MULTIPLE_CHOICES) {
      return handle300MultipleChoicesError(context, stale, uri, response);
    }
    if (isUnmodified) {
      checkState(stale != null, "Received 304 without conditional GET (bad server?) from %s", uri);
      indicateVerified(context, stale, cacheInfo);
    }
    mayThrowProxyServiceException(response);
    return null;
  }

  protected boolean isNotModified(final HttpResponse response, final Content stale) {
    return response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED;
  }

  protected Content handle300MultipleChoicesError(
      final Context context,
      final Content stale,
      final URI uri,
      final HttpResponse response) throws IOException
  {
    log.debug("Received 300 (multiple locations) from {}", uri);
    List<String> alternativeUris = extractUrls(response);
    for (String alternativeUri : alternativeUris) {
      log.debug("Processing alternative link: {}", alternativeUri);
      Content alternativeContent = fetch(alternativeUri, context, stale);
      if (alternativeContent != null) {
        return alternativeContent;
      }
    }
    return null;
  }

  /**
   * Gets URLs from header to handle 300 (multiple locations) responses.
   */
  protected List<String> extractUrls(HttpResponse response) {
    Header[] locationHeaders = response.getHeaders(HttpHeaders.LINK);
    List<String> urls = new ArrayList<>();
    Pattern pattern = Pattern.compile(ALTERNATIVE_URLS_PATTERN);
    for (Header locationHeader : locationHeaders) {
      Matcher matcher = pattern.matcher(locationHeader.getValue());
      while (matcher.find()) {
        urls.add(matcher.group(1));
      }
    }
    return urls;
  }

  protected String encodeUrl(final String url) throws UnsupportedEncodingException { // NOSONAR
    // some formats can use special characters in url
    // override this method if necessary
    return url;
  }

  /**
   * Create {@link Content} out of HTTP response.
   */
  protected Content createContent(final Context context, final HttpResponse response) {
    HttpEntity entity = response.getEntity();
    if (isHeadRequest(context) && entity == null) {
      // HEAD responses don't have entity - using HeaderOnlyPayload as entity is mandatory in HttpEntityPayload
      log.debug("Creating HeaderOnlyPayload for HEAD response");
      return new Content(new HeaderOnlyPayload(response));
    }
    return new Content(new HttpEntityPayload(response, entity));
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

  private void mayThrowBypassHttpErrorException(final Context context, final HttpResponse httpResponse) {
    final StatusLine status = httpResponse.getStatusLine();
    if (httpResponse.containsHeader(BYPASS_HTTP_ERRORS_HEADER_NAME)) {
      log.debug("Bypass http error: {}", status);
      ListMultimap<String, String> headers = buildHttpHeaders(context, httpResponse);
      String body = getBodyFromResponse(httpResponse);
      String contentType = httpResponse.containsHeader(HttpHeaders.CONTENT_TYPE)
          ? httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()
          : null;
      HttpClientUtils.closeQuietly(httpResponse);
      throw new BypassHttpErrorException(status.getStatusCode(), status.getReasonPhrase(), headers, body, contentType);
    }
  }

  private ListMultimap<String, String> buildHttpHeaders(final Context context, final HttpResponse httpResponse) {
    ListMultimap<String, String> headers = ArrayListMultimap.create();
    headers.put(BYPASS_HTTP_ERRORS_HEADER_NAME, BYPASS_HTTP_ERRORS_HEADER_VALUE);

    // If a firewall header provider is available, use it to determine which headers to copy
    if (firewallHeaderProvider != null) {
      firewallHeaderProvider.addHeaders(getRepository().getFormat().getValue(), context.getRequest(), httpResponse,
          headers);
    }

    return headers;
  }

  private String getBodyFromResponse(final HttpResponse httpResponse) {
    try {
      HttpEntity entity = httpResponse.getEntity();
      if (entity != null) {
        return EntityUtils.toString(entity);
      }
    }
    catch (IOException e) {
      log.warn("Failed to read response body", e);
    }
    return null;
  }

  /**
   * Execute http client request.
   */
  protected HttpResponse execute(
      final Context context,
      final HttpClient client,
      final HttpRequestBase request) throws IOException
  {
    HttpContext httpContext = new BasicHttpContext();

    // Only set encoding mode in context if feature is enabled
    if (urlEncodingModeEnabled && encodingHelper != null) {
      httpContext.setAttribute("preserveEncodedCharacters", encodingHelper.shouldPreserveEncodedCharacters());
    }

    // Set repository info for outbound request telemetry
    Repository repository = getRepository();
    if (repository != null && repository.getFormat() != null && repository.getType() != null) {
      httpContext.setAttribute(OutboundRequestMetricRecorder.CONTEXT_FORMAT, repository.getFormat().getValue());
      httpContext.setAttribute(OutboundRequestMetricRecorder.CONTEXT_REPOSITORY_TYPE, repository.getType().getValue());
    }

    HttpResponse response = client.execute(request, httpContext);
    context.setAttribute(HTTP_CONTEXT, httpContext);
    return response;
  }

  /**
   * Builds the {@link HttpRequestBase} for a particular set of parameters.
   * Returns HttpHead for HEAD requests, HttpGet for GET requests.
   *
   * @param uri the target URI for the request
   * @param context the request context (getRequest() may be null in cases during cleanup/error handling)
   * @return HttpHead for HEAD requests, HttpGet otherwise
   */
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context) {
    if (context.getRequest() != null && isHeadRequest(context)) {
      log.debug("Creating HttpHead request");
      return new HttpHead(uri);
    }
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
  protected abstract void indicateVerified(
      final Context context,
      final Content content,
      final CacheInfo cacheInfo) throws IOException;

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

  protected EscapeHelper getEscapeHelper() {
    return escapeHelper;
  }

  /**
   * Get the encoding helper for URL encoding based on configured mode.
   * Returns null if the feature is disabled or not configured.
   *
   * @return the encoding helper, or null if feature is disabled
   */
  protected EncodingHelper getEncodingHelper() {
    return encodingHelper;
  }

  /**
   * Encode a path for remote access using the two-stage encoding pipeline.
   *
   * Stage 1: Base URL encoding (via EncodingHelper) - applies when feature is enabled
   * Stage 2: Format-specific encoding (via encodeUrl()) - always applied
   *
   * @param path the path to encode
   * @return the encoded path
   * @throws UnsupportedEncodingException if encoding fails
   */
  protected String encodePathForRemote(final String path) throws UnsupportedEncodingException {
    if (urlEncodingModeEnabled && encodingHelper != null) {
      // New two-stage encoding (feature enabled)
      String baseEncoded = encodingHelper.encodeUrlSegments(path);
      return encodeUrl(baseEncoded);
    }
    // Legacy behavior (feature disabled or not configured)
    return encodeUrl(path);
  }

  private void validateNotPrivateNetwork(final URI uri) throws RemoteBlockedIOException {
    String host = uri.getHost();
    if (host == null) {
      return;
    }

    try {
      antiSsrfService.validateHost(host);
    }
    catch (ValidationException e) {
      String message = "Access to remote %s blocked: %s".formatted(uri, e.getMessage());
      log.debug("Blocked outbound request to local/private network URL: {} (repository: {}, reason: {})",
          uri, getRepository().getName(), e.getMessage());
      throw new RemoteBlockedIOException(message);
    }
  }

  @Subscribe
  public void on(final RepositoryCacheSyncTokenEvent event) {
    if (!event.isLocal() && getRepository().getName().equals(event.getRepositoryName())) {
      cacheControllerHolder.getMetadataCacheController().setCache(event.getToken());
    }
  }

  protected boolean shouldSkipStoreForHead() {
    return true;
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
