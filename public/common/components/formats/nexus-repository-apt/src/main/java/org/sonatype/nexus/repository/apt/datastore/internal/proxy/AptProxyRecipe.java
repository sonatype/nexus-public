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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataRebuildSchedulerFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.proxy.metadata.AptProxyMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.AptSecurityFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * Apt proxy repository recipe.
 *
 * @since 3.31
 */
@AvailabilityVersion(from = "1.0")
@Component
@Qualifier(AptProxyRecipe.NAME)
public class AptProxyRecipe
    extends RecipeSupport
{
  public static final String NAME = "apt-proxy";

  @Autowired
  Provider<AptSecurityFacet> securityFacet;

  @Autowired
  Provider<ConfigurableViewFacet> viewFacet;

  @Autowired
  Provider<HttpClientFacet> httpClientFacet;

  @Autowired
  Provider<NegativeCacheFacet> negativeCacheFacet;

  @Autowired
  Provider<AptProxyFacet> proxyFacet;

  @Autowired
  Provider<AptProxySnapshotFacet> proxySnapshotFacet;

  @Autowired
  Provider<PurgeUnusedFacet> purgeUnusedFacet;

  @Qualifier(AptFormat.NAME)
  @Autowired
  Provider<LastAssetMaintenanceFacet> lastAssetMaintenanceFacet;

  @Autowired
  Provider<AptContentFacet> aptContentFacet;

  @Autowired
  Provider<SearchFacet> searchFacet;

  @Autowired
  Provider<BrowseFacet> browseFacet;

  @Qualifier(AptFormat.NAME)
  @Autowired
  Provider<AptKeyValueFacet> keyValueFacet;

  @Qualifier(AptFormat.NAME + "-proxy")
  @Autowired
  Provider<AptProxyMetadataFacet> proxyMetadataFacet;

  @Autowired
  Provider<AptSigningFacet> signingFacet;

  @Autowired
  Provider<AptMetadataRebuildSchedulerFacet> aptMetadataRebuildSchedulerFacet;

  @Autowired
  ExceptionHandler exceptionHandler;

  @Autowired
  TimingHandler timingHandler;

  @Autowired
  SecurityHandler securityHandler;

  @Autowired
  NegativeCacheHandler negativeCacheHandler;

  @Autowired
  PartialFetchHandler partialFetchHandler;

  @Autowired
  ProxyHandler proxyHandler;

  @Autowired
  ConditionalRequestHandler conditionalRequestHandler;

  @Autowired
  ContentHeadersHandler contentHeadersHandler;

  @Autowired
  AptSnapshotHandler snapshotHandler;

  @Autowired
  AptDistributionValidationHandler distributionValidationHandler;

  @Autowired
  AptProxyMetadataHandler proxyMetadataHandler;

  @Autowired
  LastDownloadedHandler lastDownloadedHandler;

  @Autowired
  RoutingRuleHandler routingRuleHandler;

  @Autowired
  HandlerContributor handlerContributor;

  @Autowired
  public AptProxyRecipe(@Qualifier(ProxyType.NAME) final Type type, @Qualifier(AptFormat.NAME) final Format format) {
    super(type, format);
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(httpClientFacet.get());
    repository.attach(negativeCacheFacet.get());
    repository.attach(proxyFacet.get());
    repository.attach(proxySnapshotFacet.get());
    repository.attach(aptContentFacet.get());
    repository.attach(browseFacet.get());
    repository.attach(lastAssetMaintenanceFacet.get());
    repository.attach(purgeUnusedFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(keyValueFacet.get());
    repository.attach(signingFacet.get());
    repository.attach(proxyMetadataFacet.get());
    repository.attach(aptMetadataRebuildSchedulerFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    builder.route(new Route.Builder().matcher(new AlwaysMatcher())
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(distributionValidationHandler)
        .handler(negativeCacheHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(snapshotHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyMetadataHandler)
        .handler(proxyHandler)
        .create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
