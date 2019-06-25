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
package org.sonatype.nexus.repository.apt.internal.proxy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptRestoreFacet;
import org.sonatype.nexus.repository.apt.internal.AptFacetImpl;
import org.sonatype.nexus.repository.apt.internal.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptRecipeSupport;
import org.sonatype.nexus.repository.apt.internal.AptSecurityFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.NegativeCacheHandler;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.proxy.ProxyHandler;
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.search.SearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.FormatHighAvailabilitySupportHandler;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * @since 3.17
 */
@Named(AptProxyRecipe.NAME)
@Singleton
public class AptProxyRecipe
    extends AptRecipeSupport
{

  public static final String NAME = "apt-proxy";

  @Inject
  Provider<AptSecurityFacet> securityFacet;

  @Inject
  FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  @Inject
  Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  Provider<HttpClientFacet> httpClientFacet;

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet;

  @Inject
  Provider<AptProxyFacet> proxyFacet;

  @Inject
  Provider<AptProxySnapshotFacet> proxySnapshotFacet;

  @Inject
  Provider<AptFacetImpl> aptFacet;

  @Inject
  Provider<AptRestoreFacet> aptRestoreFacet;

  @Inject
  Provider<StorageFacet> storageFacet;

  @Inject
  Provider<AttributesFacet> attributesFacet;

  @Inject
  Provider<SingleAssetComponentMaintenance> componentMaintenance;

  @Inject
  Provider<SearchFacet> searchFacet;

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet;

  @Inject
  ExceptionHandler exceptionHandler;

  @Inject
  TimingHandler timingHandler;

  @Inject
  SecurityHandler securityHandler;

  @Inject
  NegativeCacheHandler negativeCacheHandler;

  @Inject
  PartialFetchHandler partialFetchHandler;

  @Inject
  UnitOfWorkHandler unitOfWorkHandler;

  @Inject
  ProxyHandler proxyHandler;

  @Inject
  ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  ContentHeadersHandler contentHeadersHandler;

  @Inject
  AptSnapshotHandler snapshotHandler;

  @Inject
  LastDownloadedHandler lastDownloadedHandler;

  @Inject
  RoutingRuleHandler routingRuleHandler;

  @Inject
  public AptProxyRecipe(final HighAvailabilitySupportChecker highAvailabilitySupportChecker,
                        @Named(ProxyType.NAME) final Type type, @Named(AptFormat.NAME) final Format format)
  {
    super(highAvailabilitySupportChecker, type, format);
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(httpClientFacet.get());
    repository.attach(negativeCacheFacet.get());
    repository.attach(proxyFacet.get());
    repository.attach(proxySnapshotFacet.get());
    repository.attach(aptFacet.get());
    repository.attach(aptRestoreFacet.get());
    repository.attach(storageFacet.get());
    repository.attach(attributesFacet.get());
    repository.attach(componentMaintenance.get());
    repository.attach(searchFacet.get());
    repository.attach(purgeUnusedFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    builder.route(new Route.Builder().matcher(new AlwaysMatcher())
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(routingRuleHandler)
        .handler(exceptionHandler)
        .handler(negativeCacheHandler)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(snapshotHandler)
        .handler(lastDownloadedHandler)
        .handler(proxyHandler).create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
