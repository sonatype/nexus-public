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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.content.maven.internal.index.MavenContentProxyIndexFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.matcher.MavenNx2MetaFilesMatcher;
import org.sonatype.nexus.repository.maven.internal.recipes.Maven2ProxyRecipe;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route.Builder;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * @since 3.26
 */
@AvailabilityVersion(from = "1.0")
@Named(Maven2ProxyRecipe.NAME)
@Singleton
public class MavenProxyRecipe
    extends MavenRecipeSupport
    implements Maven2ProxyRecipe
{
  private final Provider<HttpClientFacet> httpClientFacet;

  private final Provider<NegativeCacheFacet> negativeCacheFacet;

  private final Provider<MavenProxyFacet> proxyFacet;

  private final Provider<PurgeUnusedFacet> purgeUnusedFacet;

  private final NegativeCacheHandler negativeCacheHandler;

  private final ProxyHandler proxyHandler;

  private final Provider<MavenContentProxyIndexFacet> mavenProxyIndexFacet;

  @Inject
  public MavenProxyRecipe(
      @Named(ProxyType.NAME) final Type type,
      @Named(Maven2Format.NAME) final Format format,
      final Provider<HttpClientFacet> httpClientFacet,
      final Provider<NegativeCacheFacet> negativeCacheFacet,
      final Provider<MavenProxyFacet> proxyFacet,
      final Provider<PurgeUnusedFacet> purgeUnusedFacet,
      final NegativeCacheHandler negativeCacheHandler,
      final ProxyHandler proxyHandler,
      final Provider<MavenContentProxyIndexFacet> mavenProxyIndexFacet)
  {
    super(type, format);
    this.httpClientFacet = checkNotNull(httpClientFacet);
    this.negativeCacheFacet = checkNotNull(negativeCacheFacet);
    this.proxyFacet = checkNotNull(proxyFacet);
    this.purgeUnusedFacet = checkNotNull(purgeUnusedFacet);
    this.negativeCacheHandler = checkNotNull(negativeCacheHandler);
    this.proxyHandler = checkNotNull(proxyHandler);
    this.mavenProxyIndexFacet = checkNotNull(mavenProxyIndexFacet);
  }

  @Override
  public void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(httpClientFacet.get());
    repository.attach(negativeCacheFacet.get());
    repository.attach(proxyFacet.get());
    repository.attach(mavenContentFacet.get());
    repository.attach(purgeUnusedFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(browseFacet.get());
    repository.attach(mavenProxyIndexFacet.get());
    repository.attach(mavenMaintenanceFacet.get());
    repository.attach(removeSnapshotsFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    addBrowseUnsupportedRoute(builder);

    // Note: partialFetchHandler() NOT added for Maven metadata;
    builder.route(newMetadataRouteBuilder()
        .handler(negativeCacheHandler)
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    builder.route(newIndexRouteBuilder()
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    builder.route(newArchetypeCatalogRouteBuilder()
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    builder.route(newNx2MetaFilesRouteBuilder()
        .handler(notFound())
        .create());

    builder.route(newMavenPathRouteBuilder()
        .handler(negativeCacheHandler)
        .handler(partialFetchHandler)
        .handler(versionPolicyHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler)
        .create());

    builder.defaultHandlers(notFound());

    facet.configure(builder.create());

    return facet;
  }

  private Builder newNx2MetaFilesRouteBuilder() {
    return new Builder().matcher(new MavenNx2MetaFilesMatcher(mavenPathParser));
  }
}
