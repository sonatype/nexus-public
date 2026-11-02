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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.inject.Provider;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataRebuildSchedulerFacet;
import org.sonatype.nexus.repository.apt.internal.AptSecurityFacet;
import org.sonatype.nexus.repository.apt.internal.gpg.AptSigningFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.HostedType;
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

import org.springframework.beans.factory.annotation.Qualifier;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;
import org.springframework.stereotype.Component;

/**
 * Apt hosted repository recipe.
 *
 * @since 3.31
 */
@AvailabilityVersion(from = "1.0")
@Component
@Qualifier(AptHostedRecipe.NAME)
public class AptHostedRecipe
    extends RecipeSupport
{
  public static final String NAME = "apt-hosted";

  private final Provider<AptSecurityFacet> securityFacet;

  private final Provider<AptSigningFacet> aptSigningFacet;

  private final Provider<ConfigurableViewFacet> viewFacet;

  private final Provider<AptContentFacet> aptContentFacet;

  private final Provider<AptHostedFacet> aptHostedFacet;

  private final Provider<BrowseFacet> browseFacet;

  private final Provider<AptLastAssetMaintenanceFacet> maintenanceFacet;

  private final Provider<AptHostedSnapshotFacet> hostedSnapshotFacet;

  private final Provider<AptKeyValueFacet> aptKeyValueFacet;

  private final Provider<AptHostedMetadataFacet> aptHostedMetadataFacet;

  private final Provider<AptMetadataRebuildSchedulerFacet> aptMetadataRebuildSchedulerFacet;

  private final TimingHandler timingHandler;

  private final SecurityHandler securityHandler;

  private final ExceptionHandler exceptionHandler;

  private final HandlerContributor handlerContributor;

  private final ConditionalRequestHandler conditionalRequestHandler;

  private final PartialFetchHandler partialFetchHandler;

  private final ContentHeadersHandler contentHeadersHandler;

  private final LastDownloadedHandler lastDownloadedHandler;

  private final AptSnapshotHandler snapshotHandler;

  private final AptHostedHandler hostedHandler;

  private final Provider<SearchFacet> searchFacet;

  @Autowired
  public AptHostedRecipe(
      @Qualifier(HostedType.NAME) final Type type,
      @Qualifier(AptFormat.NAME) final Format format,
      final Provider<AptSecurityFacet> securityFacet,
      final Provider<AptSigningFacet> aptSigningFacet,
      final Provider<ConfigurableViewFacet> viewFacet,
      final Provider<AptContentFacet> aptContentFacet,
      final Provider<AptHostedFacet> aptHostedFacet,
      final Provider<BrowseFacet> browseFacet,
      final Provider<AptLastAssetMaintenanceFacet> maintenanceFacet,
      final Provider<AptHostedSnapshotFacet> hostedSnapshotFacet,
      final Provider<AptKeyValueFacet> aptKeyValueFacet,
      final Provider<AptHostedMetadataFacet> aptHostedMetadataFacet,
      final Provider<AptMetadataRebuildSchedulerFacet> aptMetadataRebuildSchedulerFacet,
      final TimingHandler timingHandler,
      final SecurityHandler securityHandler,
      final ExceptionHandler exceptionHandler,
      final HandlerContributor handlerContributor,
      final ConditionalRequestHandler conditionalRequestHandler,
      final PartialFetchHandler partialFetchHandler,
      final ContentHeadersHandler contentHeadersHandler,
      final LastDownloadedHandler lastDownloadedHandler,
      final AptSnapshotHandler snapshotHandler,
      final AptHostedHandler hostedHandler,
      final Provider<SearchFacet> searchFacet)
  {
    super(type, format);
    this.securityFacet = securityFacet;
    this.aptSigningFacet = aptSigningFacet;
    this.viewFacet = viewFacet;
    this.aptContentFacet = aptContentFacet;
    this.aptHostedFacet = aptHostedFacet;
    this.browseFacet = browseFacet;
    this.maintenanceFacet = maintenanceFacet;
    this.hostedSnapshotFacet = hostedSnapshotFacet;
    this.aptKeyValueFacet = aptKeyValueFacet;
    this.aptHostedMetadataFacet = aptHostedMetadataFacet;
    this.aptMetadataRebuildSchedulerFacet = aptMetadataRebuildSchedulerFacet;
    this.timingHandler = timingHandler;
    this.securityHandler = securityHandler;
    this.exceptionHandler = exceptionHandler;
    this.handlerContributor = handlerContributor;
    this.conditionalRequestHandler = conditionalRequestHandler;
    this.partialFetchHandler = partialFetchHandler;
    this.contentHeadersHandler = contentHeadersHandler;
    this.lastDownloadedHandler = lastDownloadedHandler;
    this.snapshotHandler = snapshotHandler;
    this.hostedHandler = hostedHandler;
    this.searchFacet = searchFacet;
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(aptSigningFacet.get());
    repository.attach(aptContentFacet.get());
    repository.attach(aptHostedFacet.get());
    repository.attach(maintenanceFacet.get());
    repository.attach(browseFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(hostedSnapshotFacet.get());
    repository.attach(aptKeyValueFacet.get());
    repository.attach(aptHostedMetadataFacet.get());
    repository.attach(aptMetadataRebuildSchedulerFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    builder.route(new Route.Builder().matcher(new AlwaysMatcher())
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(snapshotHandler)
        .handler(hostedHandler)
        .create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
