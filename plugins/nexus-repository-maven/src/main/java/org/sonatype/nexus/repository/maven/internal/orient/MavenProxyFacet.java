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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.maven.internal.orient.MavenFacetUtils.findAsset;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * Maven specific implementation of {@link ProxyFacetSupport}.
 *
 * @since 3.0
 */
@Named
public class MavenProxyFacet
    extends ProxyFacetSupport
{
  private final ConstraintViolationFactory constraintViolationFactory;

  private OrientMavenFacet mavenFacet;

  @Inject
  public MavenProxyFacet(final ConstraintViolationFactory constraintViolationFactory) {
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    this.mavenFacet = facet(OrientMavenFacet.class);
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
    return mavenFacet.get(mavenPath(context));
  }

  @Nullable
  @Override
  protected Content fetch(final Context context, final Content stale) throws IOException {
    // do not go remote for a non maven 2 artifact or metadata if is not already present in cache or allowed by config
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
    if (mavenFacet.getMavenPathParser().isRepositoryMetadata(mavenPath(context))) {
      return cacheControllerHolder.getMetadataCacheController();
    }
    else {
      return cacheControllerHolder.getContentCacheController();
    }
  }

  @Override
  protected Content store(final Context context, final Content payload) throws IOException {
    return mavenFacet.put(mavenPath(context), payload);
  }

  @Override
  @TransactionalTouchMetadata
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());
    final MavenPath path = mavenPath(context);

    // by EntityId
    Asset asset = Content.findAsset(tx, bucket, content);
    if (asset == null) {
      // by format coordinates
      asset = findAsset(tx, bucket, path);
    }
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent maven asset {}", path.getPath());
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", path.getPath(), cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1); // omit leading slash
  }

  @Nonnull
  private MavenPath mavenPath(@Nonnull final Context context) {
    return context.getAttributes().require(MavenPath.class);
  }
}
