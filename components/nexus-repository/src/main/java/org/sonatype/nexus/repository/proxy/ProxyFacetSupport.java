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
import java.net.URI;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.io.Cooperation;
import org.sonatype.nexus.common.io.CooperationFactory;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.RemoteBlockedIOException;
import org.sonatype.nexus.repository.storage.MissingBlobException;
import org.sonatype.nexus.repository.storage.RetryDeniedException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;
import org.sonatype.nexus.validation.constraint.Url;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
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

/**
 * A support class which implements basic payload logic; subclasses provide format-specific operations.
 *
 * @since 3.0
 */
public abstract class ProxyFacetSupport
    extends FacetSupport
    implements ProxyFacet
{
  @VisibleForTesting
  static final String CONFIG_KEY = "proxy";

  @VisibleForTesting
  public static class Config
  {
    @Url
    @NotNull
    public URI remoteUrl;

    /**
     * Content max-age minutes.
     */
    @NotNull
    public Integer contentMaxAge = Time.hours(24).toMinutesI();

    /**
     * Metadata max-age minutes.
     */
    @NotNull
    public Integer metadataMaxAge = Time.hours(24).toMinutesI();

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "remoteUrl=" + remoteUrl +
          ", contentMaxAge=" + contentMaxAge +
          '}';
    }
  }

  private static final ThreadLocal<Boolean> downloading = new ThreadLocal<>();

  private Config config;

  private HttpClientFacet httpClient;

  private boolean remoteUrlChanged;

  protected CacheControllerHolder cacheControllerHolder;

  @Nullable
  private CooperationFactory.Builder cooperationBuilder;

  @Nullable
  private Cooperation proxyCooperation;

  /**
   * Configures content {@link Cooperation} for this proxy; a timeout of 0 means wait indefinitely.
   *
   * @param enabled should threads attempt to cooperate when downloading resources
   * @param majorTimeout when waiting for the main I/O request
   * @param minorTimeout when waiting for any I/O dependencies
   * @param threadsPerKey limits the threads waiting under each key
   *
   * @since 3.4
   */
  @Inject
  protected void configureCooperation(
      final CooperationFactory cooperationFactory,
      @Named("${nexus.proxy.cooperation.enabled:-true}") final boolean cooperationEnabled,
      @Named("${nexus.proxy.cooperation.majorTimeout:-0s}") final Time majorTimeout,
      @Named("${nexus.proxy.cooperation.minorTimeout:-30s}") final Time minorTimeout,
      @Named("${nexus.proxy.cooperation.threadsPerKey:-100}") final int threadsPerKey)
  {
    if (cooperationEnabled) {
      this.cooperationBuilder = cooperationFactory.configure()
          .majorTimeout(majorTimeout)
          .minorTimeout(minorTimeout)
          .threadsPerKey(threadsPerKey);
    }
  }

  @VisibleForTesting
  void buildCooperation() {
    if (cooperationBuilder != null) {
      this.proxyCooperation = cooperationBuilder.build(getRepository().getName() + ":proxy");
    }
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    buildCooperation();
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);

    cacheControllerHolder = new CacheControllerHolder(
        new CacheController(Time.minutes(config.contentMaxAge).toSecondsI(), null),
        new CacheController(Time.minutes(config.metadataMaxAge).toSecondsI(), null)
    );

    // normalize URL path to contain trailing slash
    if (!config.remoteUrl.getPath().endsWith("/")) {
      config.remoteUrl = config.remoteUrl.resolve(config.remoteUrl.getPath() + "/");
    }

    log.debug("Config: {}", config);
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

  public URI getRemoteUrl() {
    return config.remoteUrl;
  }

  @Override
  public Content get(final Context context) throws IOException {
    checkNotNull(context);

    Content content = maybeGetCachedContent(context);
    if (!isStale(context, content)) {
      return content;
    }
    if (proxyCooperation == null) {
      return doGet(context, content);
    }
    return proxyCooperation.cooperate(getRequestKey(context), failover -> {
      Content latestContent = content;
      if (failover) {
        // re-check cache when failing over to new thread
        latestContent = proxyCooperation.join(() -> maybeGetCachedContent(context));
        if (!isStale(context, latestContent)) {
          return latestContent;
        }
      }
      return doGet(context, latestContent);
    });
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
        if (proxyCooperation != null && remote.equals(content)) {
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
      logContentOrThrow(content, context, null, e.getCause()); // "special" path (for now) for npm and similar peculiar formats
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
   * Path + query parameters provide a unique enough request key for known formats.
   * If a format needs to add more context then they should customize this method.
   *
   * @return key that uniquely identifies this upstream request from other contexts
   *
   * @since 3.4
   */
  protected String getRequestKey(final Context context) {
    return context.getRequest().getPath() + '?' + context.getRequest().getParameters();
  }

  private <X extends Throwable> void logContentOrThrow(@Nullable final Content content,
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
  <X extends Throwable> String buildLogContentMessage(@Nullable final Content content,
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

  private Content maybeGetCachedContent(Context context) throws IOException {
    try {
      return getCachedContent(context);
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
   *
   * The Context indicates which component was being requested.
   *
   * @throws IOException
   * @throws InvalidContentException
   */
  protected abstract Content store(final Context context, final Content content) throws IOException;

  @Nullable
  protected Content fetch(final Context context, Content stale) throws IOException {
    return fetch(getUrl(context), context, stale);
  }

  protected Content fetch(String url, Context context, @Nullable Content stale) throws IOException {
    HttpClient client = httpClient.getHttpClient();

    checkState(config.remoteUrl.isAbsolute(),
        "Invalid remote URL '%s' for proxy repository %s, please fix your configuration", config.remoteUrl,
        getRepository().getName());
    URI uri;
    try {
      uri = config.remoteUrl.resolve(url);
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
        request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + etag + "\"");
      }
    }
    log.debug("Fetching: {}", request);

    HttpResponse response = execute(context, client, request);
    log.debug("Response: {}", response);

    StatusLine status = response.getStatusLine();
    log.debug("Status: {}", status);

    final CacheInfo cacheInfo = getCacheController(context).current();

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      log.debug("Entity: {}", entity);

      final Content result = createContent(context, response);
      result.getAttributes().set(Content.CONTENT_LAST_MODIFIED, extractLastModified(request, response));
      result.getAttributes().set(Content.CONTENT_ETAG, extractETag(response));
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

  /**
   * Create {@link Content} out of HTTP response.
   */
  protected Content createContent(final Context context, final HttpResponse response)
  {
    return new Content(new HttpEntityPayload(response, response.getEntity()));
  }

  /**
   * May throw {@link ProxyServiceException} based on response statuses.
   */
  private void mayThrowProxyServiceException(final HttpResponse httpResponse) {
    final StatusLine status = httpResponse.getStatusLine();
    if (HttpStatus.SC_UNAUTHORIZED == status.getStatusCode()
        || HttpStatus.SC_PAYMENT_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED == status.getStatusCode()
        || HttpStatus.SC_INTERNAL_SERVER_ERROR <= status.getStatusCode()) {
      throw new ProxyServiceException(httpResponse);
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
  protected HttpRequestBase buildFetchHttpRequest(URI uri, Context context) {
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
   * Extract ETag from response if possible, or {@code null}.
   */
  @Nullable
  private String extractETag(final HttpResponse response) {
    final Header etagHeader = response.getLastHeader(HttpHeaders.ETAG);
    if (etagHeader != null) {
      final String etag = etagHeader.getValue();
      if (!Strings.isNullOrEmpty(etag)) {
        if (etag.startsWith("\"") && etag.endsWith("\"")) {
          return etag.substring(1, etag.length() - 1);
        }
        else {
          return etag;
        }
      }
    }
    return null;
  }

  /**
   * For whatever component/asset
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
    return cacheInfo == null || getCacheController(context).isStale(cacheInfo);
  }

  /**
   * @return number of threads cooperating per request-key.
   */
  @VisibleForTesting
  Map<String, Integer> getThreadCooperationPerRequest() {
    return proxyCooperation.getThreadCountPerKey();
  }
}
