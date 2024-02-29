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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.data.AptKeyValueFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
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

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * Apt hosted repository recipe.
 *
 * @since 3.31
 */
@AvailabilityVersion(from = "1.0")
@Named(AptHostedRecipe.NAME)
@Singleton
public class AptHostedRecipe
    extends RecipeSupport
{
  public static final String NAME = "apt-hosted";

  @Inject
  Provider<AptSecurityFacet> securityFacet;

  @Inject
  Provider<AptSigningFacet> aptSigningFacet;

  @Inject
  Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  Provider<AptContentFacet> aptContentFacet;

  @Inject
  Provider<AptHostedFacet> aptHostedFacet;

  @Inject
  Provider<BrowseFacet> browseFacet;

  @Inject
  Provider<AptLastAssetMaintenanceFacet> maintenanceFacet;

  @Inject
  Provider<AptHostedSnapshotFacet> hostedSnapshotFacet;

  @Inject
  Provider<AptKeyValueFacet> aptKeyValueFacet;

  @Inject
  Provider<AptHostedMetadataFacet> aptHostedMetadataFacet;

  @Inject
  TimingHandler timingHandler;

  @Inject
  SecurityHandler securityHandler;

  @Inject
  ExceptionHandler exceptionHandler;

  @Inject
  HandlerContributor handlerContributor;

  @Inject
  ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  PartialFetchHandler partialFetchHandler;

  @Inject
  ContentHeadersHandler contentHeadersHandler;

  @Inject
  LastDownloadedHandler lastDownloadedHandler;

  @Inject
  AptSnapshotHandler snapshotHandler;

  @Inject
  AptHostedHandler hostedHandler;

  @Inject
  Provider<SearchFacet> searchFacet;

  @Inject
  public AptHostedRecipe(
      @Named(HostedType.NAME) final Type type,
      @Named(AptFormat.NAME) final Format format)
  {
    super(type, format);
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
        .handler(hostedHandler).create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
