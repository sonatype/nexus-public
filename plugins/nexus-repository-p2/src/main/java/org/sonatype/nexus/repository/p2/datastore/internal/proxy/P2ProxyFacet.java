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
package org.sonatype.nexus.repository.p2.datastore.internal.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.facet.ContentProxyFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.AssetDAO;
import org.sonatype.nexus.repository.mime.ContentValidator;
import org.sonatype.nexus.repository.p2.datastore.P2ContentFacet;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.metadata.CompositeRepositoryRewriter;
import org.sonatype.nexus.repository.p2.internal.metadata.RemoveMirrorTransformer;
import org.sonatype.nexus.repository.p2.internal.metadata.UriToSiteHashUtil;
import org.sonatype.nexus.repository.p2.internal.metadata.XmlTransformer;
import org.sonatype.nexus.repository.p2.internal.proxy.StreamCopier;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.matcherState;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.toP2Attributes;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.toP2AttributesBinary;

/**
 * P2 {@link ProxyFacet} implementation.
 *
 * @since 3.next
 */
@Named
public class P2ProxyFacet
    extends ContentProxyFacetSupport
{
  public static final String REMOTE_URL = "remote_url";

  public static final String REMOTE_HASH = "remote_site_hash";

  public static final String MIRRORS_URL = "mirrors_url";

  public static final String CHILD_URLS = "child_urls";

  private final ContentValidator contentValidator;

  @Inject
  public P2ProxyFacet(final ContentValidator contentValidator)
  {
    this.contentValidator = checkNotNull(contentValidator);
  }

  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case COMPOSITE_ARTIFACTS:
      case COMPOSITE_CONTENT:
      case P2_INDEX:
      case ARTIFACTS_METADATA:
      case CONTENT_METADATA:
      case BUNDLE:
      case BINARY_BUNDLE:
        return content()
            .get(context.getRequest().getPath())
            .orElse(null);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    String remoteUrl = context.getAttributes().require(REMOTE_URL, String.class);
    TokenMatcher.State matcherState = matcherState(context);
    String path = context.getRequest().getPath();
    switch (assetKind) {
      case COMPOSITE_ARTIFACTS:
      case COMPOSITE_CONTENT:
        return storeCompositeMetadata(path, content, assetKind, matcherState, remoteUrl);
      case P2_INDEX:
        return content().putP2Index(path, content, Collections.singletonMap(REMOTE_URL, remoteUrl));
      case CONTENT_METADATA:
        return content().putContentMetadata(path, content, Collections.singletonMap(REMOTE_URL, remoteUrl));
      case ARTIFACTS_METADATA:
        return storeArtifactsMetadata(path, content, assetKind, matcherState, remoteUrl);
      case BUNDLE:
        return content().putBundle(toP2Attributes(path, matcherState), content);
      case BINARY_BUNDLE:
        return content().putBinary(toP2AttributesBinary(path, matcherState), content);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  @Nullable
  protected Content fetch(final Context context, final Content stale) throws IOException {
    String url = getUrl(context);
    if (url == null) {
      return null;
    }
    return fetch(url, context, stale);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    TokenMatcher.State matcherState = matcherState(context);
    String site = matcherState.getTokens().get("site");

    String remoteUrl;
    if (site == null) {
      String repositoryUrl = getRemoteUrl().toString();
      remoteUrl = URI.create(repositoryUrl.endsWith("/") ? repositoryUrl : repositoryUrl + '/')
          .resolve(context.getRequest().getPath().substring(1)).toString();
    }
    else {
      String path = context.getRequest().getPath().substring(2 + site.length());
      Optional<URI> baseUri = findRepositoryUrl(site);
      remoteUrl = baseUri.map(b -> b.resolve(path)).map(Object::toString).orElse(null);
    }

    context.getAttributes().backing().put(REMOTE_URL, remoteUrl);
    return remoteUrl;
  }

  private Optional<URI> findRepositoryUrl(final String site) {
    Iterable<FluentAsset> assets = Continuations.iterableOf(content().assets()
        .byFilter("kind = #{" + AssetDAO.FILTER_PARAMS + ".artifacts} OR kind = #{" +
                  AssetDAO.FILTER_PARAMS + ".content}",
            ImmutableMap.of("artifacts", AssetKind.COMPOSITE_ARTIFACTS,
                "content", AssetKind.COMPOSITE_CONTENT))::browse);

    for (FluentAsset asset : assets) {
      List<String> urls = extractUris(asset);
      for (String url : urls) {
        if (UriToSiteHashUtil.map(url).equals(site)) {
          return Optional.of(URI.create(url));
        }
      }
    }
    log.debug("Unknown remote site: {}", site);
    return Optional.empty();
  }

  private List<String> extractUris(final FluentAsset asset) {
    return asset.attributes(P2Format.NAME).get(CHILD_URLS, new TypeToken<List<String>>() {});
  }

  private Content storeCompositeMetadata(
      final String assetPath,
      final Content content,
      final AssetKind assetKind,
      final TokenMatcher.State matcherState,
      final String remoteUrl) throws IOException
  {
    boolean isCompositeArtifacts = AssetKind.COMPOSITE_ARTIFACTS == assetKind;
    String site = getSiteHash(matcherState);
    String filename = isCompositeArtifacts ? "compositeArtifacts.xml" : "compositeContent.xml";
    URI baseUri = URI.create(remoteUrl);
    CompositeRepositoryRewriter rewriter = new CompositeRepositoryRewriter(baseUri, site == null);

    try (StreamPayload payload = rewriteMetadata(assetPath, content, filename, rewriter)) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(CHILD_URLS, rewriter.getUrls());
      attributes.put(REMOTE_HASH, site);
      attributes.put(REMOTE_URL, remoteUrl);

      Content streamContent = toContent(payload, content);

      return isCompositeArtifacts ? content().putCompositeArtifactsMetadata(assetPath, streamContent, attributes)
          : content().putCompositeContentMetadata(assetPath, streamContent, attributes);
    }
  }

  private Content storeArtifactsMetadata(
      final String assetPath,
      final Content content,
      final AssetKind assetKind,
      final TokenMatcher.State matcherState,
      final String remoteUrl) throws IOException
  {
    RemoveMirrorTransformer removeMirrorTransformer = new RemoveMirrorTransformer();

    try (StreamPayload payload = rewriteMetadata(assetPath, content, "artifacts.xml", removeMirrorTransformer)) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(REMOTE_HASH, getSiteHash(matcherState));
      attributes.put(REMOTE_URL, remoteUrl);
      removeMirrorTransformer.getMirrorsUrl().ifPresent(mirrorsUrl -> attributes.put(MIRRORS_URL, mirrorsUrl));

      return content().putArtifactsMetadata(assetPath, toContent(payload, content), attributes);
    }
  }

  private StreamPayload rewriteMetadata(
      final String assetPath,
      final Content content,
      final String internalFilename,
      final XmlTransformer transformer) throws IOException
  {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    try (InputStream in = content.openInputStream()) {
      IOUtils.copy(in, buffer);
    }

    ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray());
    buffer.reset();

    String mimeType = contentValidator.determineContentType(false, () -> in, null, assetPath, content.getContentType());
    in.reset();
    StreamCopier.copierFor(mimeType, internalFilename, in, buffer).process(transformer);

    return new StreamPayload(() -> new ByteArrayInputStream(buffer.toByteArray()), buffer.size(), mimeType);
  }

  private String getSiteHash(final TokenMatcher.State matcherState) {
    String site = matcherState.getTokens().get("site");
    if (site == null) {
      return null;
    }
    return UriToSiteHashUtil.map(getRemoteUrl());
  }

  private P2ContentFacet content() {
    return getRepository().facet(P2ContentFacet.class);
  }

  private static Content toContent(final Payload payload, final Content originalContent) {
    Content streamContent = new Content(payload);
    streamContent.getAttributes().backing().putAll(originalContent.getAttributes().backing());
    return streamContent;
  }
}
