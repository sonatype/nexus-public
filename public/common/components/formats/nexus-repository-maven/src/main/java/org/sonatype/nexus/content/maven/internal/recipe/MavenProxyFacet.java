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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.ETagHeaderUtils;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenProxyRequestHeaderSupport;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Maven specific implementation of {@link ProxyFacetSupport}.
 *
 * @since 3.26
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MavenProxyFacet
    extends ContentProxyFacetSupport
{
  private static final String MAVEN_CENTRAL_HOST = "repo1.maven.org";

  public static final String P_ATTRIBUTES = "attributes";

  final ConstraintViolationFactory constraintViolationFactory;

  private final MavenProxyRequestHeaderSupport mavenProxyRequestHeaderSupport;

  @Inject
  public MavenProxyFacet(
      final ConstraintViolationFactory constraintViolationFactory,
      final MavenProxyRequestHeaderSupport mavenProxyRequestHeaderSupport)
  {
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.mavenProxyRequestHeaderSupport = checkNotNull(mavenProxyRequestHeaderSupport);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
    validatePreemptiveAuth(configuration);
  }

  /**
   * Pre-emptive authentication is limited only to Maven Proxy using HTTPS protocol.
   */
  private void validatePreemptiveAuth(final Configuration configuration) {
    NestedAttributesMap httpclient = configuration.attributes("httpclient");
    if (Objects.nonNull(httpclient.get("authentication"))) {
      boolean isPreemptive =
          configuration.attributes("httpclient").child("authentication").get("preemptive", Boolean.class, false);
      if (isPreemptive) {
        String remoteUrl = configuration.attributes("proxy").get("remoteUrl", String.class, "");
        if (!remoteUrl.startsWith("https://")) {
          ConstraintViolation<?> violation = constraintViolationFactory
              .createViolation(P_ATTRIBUTES + ".httpclient.authentication.preemptive",
                  "Pre-emptive authentication is allowed only when using HTTPS remote URL");
          throw new ConstraintViolationException(ImmutableSet.of(violation));
        }
      }
    }
  }

  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    return content()
        .get(mavenPath(context))
        .orElse(null);
  }

  @Nullable
  @Override
  protected Content fetch(final Context context, final Content stale) throws IOException {
    // do not go remote for a non maven 2 artifact or metadata if is not already present in cache or allowed by config
    MavenContentFacet mavenFacet = content();
    if (stale == null && mavenFacet.layoutPolicy() == LayoutPolicy.STRICT) {
      MavenPath mavenPath = mavenPath(context);
      if (mavenPath.getCoordinates() == null
          && !mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath)
          && !mavenFacet.getMavenPathParser().isRepositoryIndex(mavenPath)
          && !mavenPath.getFileName().equals(Constants.ARCHETYPE_CATALOG_FILENAME)) {
        return null;
      }
    }
    return super.fetch(context, stale);
  }

  @Override
  protected CacheController getCacheController(@Nonnull final Context context) {
    if (content().getMavenPathParser().isRepositoryMetadata(mavenPath(context))) {
      return cacheControllerHolder.getMetadataCacheController();
    }
    else {
      return cacheControllerHolder.getContentCacheController();
    }
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    return content().put(mavenPath(context), payload);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return removePrefixingSlash(context.getRequest().getPath());
  }

  /**
   * Override to support ETag-based blob reuse for 200 OK responses.
   * When a remote server returns 200 OK but the ETag matches the cached content,
   * we treat it as "not modified" to avoid recreating blobs unnecessarily.
   * This is especially important for cloud deployments using S3 blob stores.
   */
  @Override
  protected boolean isNotModified(final HttpResponse response, @Nullable final Content stale) {
    boolean parent = super.isNotModified(response, stale);

    // If already not modified (304), or no stale content, use default behavior
    if (parent) {
      return true;
    }

    if (stale == null) {
      return false;
    }

    // Extract status code only when needed (after early returns)
    int statusCode = response.getStatusLine().getStatusCode();

    if (statusCode != 200) {
      return false;
    }

    // Check for ETag header only after confirming 200 OK
    // Azure Blob Storage, CDNs may return "etag" (lowercase) or "ETag" (standard)
    // HTTP header names are case-insensitive per RFC 7230, but Apache HttpClient lookup is case-sensitive
    boolean hasETagHeader = response.containsHeader(HttpHeaders.ETAG) || response.containsHeader("etag");

    if (!hasETagHeader) {
      return false;
    }

    // For 200 OK responses, compare ETags
    // Try standard case first, then lowercase
    Header etagHeader = response.getFirstHeader(HttpHeaders.ETAG);
    if (etagHeader == null) {
      etagHeader = response.getFirstHeader("etag");
    }
    String etag = etagHeader != null ? ETagHeaderUtils.extract(etagHeader.getValue()) : null;
    String staleEtag = stale.getAttributes().get(Content.CONTENT_ETAG, String.class);
    boolean etagsMatch = staleEtag != null && Objects.equals(etag, staleEtag);

    // Single comprehensive log statement
    log.debug(
        "Maven: isNotModified - status={}, hasStale={}, hasETag={}, responseETag={}, cachedETag={}, match={}, result={}",
        statusCode,
        true,
        hasETagHeader,
        etag,
        staleEtag,
        etagsMatch,
        (etagsMatch ? "REUSE_BLOB" : "DOWNLOAD_AND_CHECK_CHECKSUM"));

    return etagsMatch;
  }

  @Override
  protected void indicateVerified(
      final Context context,
      final Content content,
      final CacheInfo cacheInfo)
  {
    MavenPath mavenPath = mavenPath(context);
    FluentAsset fluentAsset = facet(MavenContentFacet.class)
        .createComponentAndAsset(mavenPath);

    if (fluentAsset == null) {
      log.debug("Attempting to set cache info for non-existent maven asset: {} on the repository: {}",
          mavenPath, getRepository().getName());
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("Updating cacheInfo of {} to {} on the repository {}",
          fluentAsset.path(), cacheInfo, getRepository().getName());
    }
    fluentAsset.markAsCached(cacheInfo);
  }

  @Override
  protected HttpRequestBase buildFetchHttpRequest(final URI uri, final Context context) {
    HttpRequestBase request = super.buildFetchHttpRequest(uri, context);
    String augmentedUserAgent;
    if (MAVEN_CENTRAL_HOST.equals(uri.getHost())) {
      augmentedUserAgent = mavenProxyRequestHeaderSupport.getUserAgentForAnalytics();
      request.setHeader(HttpHeaders.USER_AGENT, augmentedUserAgent);
    }
    return request;
  }

  @Nonnull
  private MavenPath mavenPath(@Nonnull final Context context) {
    return context.getAttributes().require(MavenPath.class);
  }

  private MavenContentFacet content() {
    return getRepository().facet(MavenContentFacet.class);
  }

  private String removePrefixingSlash(final String url) {
    if (url != null && url.startsWith("/")) {
      return url.replaceFirst("/", "");
    }
    return url;
  }
}
