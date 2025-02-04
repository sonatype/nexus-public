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
package org.sonatype.nexus.content.raw.internal.recipe;

import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.maintenance.SingleAssetMaintenanceFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.raw.ContentDispositionHandler;
import org.sonatype.nexus.repository.raw.internal.RawIndexHtmlForwardHandler;
import org.sonatype.nexus.repository.raw.internal.RawSecurityFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.24
 */
public abstract class RawRecipeSupport
    extends RecipeSupport
{
  public static final String PATH_NAME = "path";

  protected static final TokenMatcher PATH_MATCHER = new TokenMatcher("{" + PATH_NAME + ":/.+}");

  protected Provider<RawSecurityFacet> securityFacet;

  protected Provider<ConfigurableViewFacet> viewFacet;

  protected Provider<RawContentFacet> contentFacet;

  protected Provider<SingleAssetMaintenanceFacet> maintenanceFacet;

  protected Provider<SearchFacet> searchFacet;

  protected Provider<BrowseFacet> browseFacet;

  protected ExceptionHandler exceptionHandler;

  protected TimingHandler timingHandler;

  protected RawIndexHtmlForwardHandler indexHtmlForwardHandler;

  protected SecurityHandler securityHandler;

  protected PartialFetchHandler partialFetchHandler;

  protected RawContentHandler contentHandler;

  protected ConditionalRequestHandler conditionalRequestHandler;

  protected ContentHeadersHandler contentHeadersHandler;

  protected LastDownloadedHandler lastDownloadedHandler;

  protected HandlerContributor handlerContributor;

  protected ContentDispositionHandler contentDispositionHandler;

  protected RawRecipeSupport(final Type type, final Format format) {
    super(type, format);
  }

  @Inject
  public final void setDependencies(
      final Provider<RawSecurityFacet> securityFacet,
      final Provider<ConfigurableViewFacet> viewFacet,
      final Provider<RawContentFacet> contentFacet,
      final Provider<SingleAssetMaintenanceFacet> maintenanceFacet,
      final Provider<SearchFacet> searchFacet,
      final Provider<BrowseFacet> browseFacet,
      final ExceptionHandler exceptionHandler,
      final TimingHandler timingHandler,
      final RawIndexHtmlForwardHandler indexHtmlForwardHandler,
      final SecurityHandler securityHandler,
      final PartialFetchHandler partialFetchHandler,
      final RawContentHandler contentHandler,
      final ConditionalRequestHandler conditionalRequestHandler,
      final ContentHeadersHandler contentHeadersHandler,
      final LastDownloadedHandler lastDownloadedHandler,
      final HandlerContributor handlerContributor,
      final ContentDispositionHandler contentDispositionHandler)
  {
    this.securityFacet = checkNotNull(securityFacet);
    this.viewFacet = checkNotNull(viewFacet);
    this.contentFacet = checkNotNull(contentFacet);
    this.maintenanceFacet = checkNotNull(maintenanceFacet);
    this.searchFacet = checkNotNull(searchFacet);
    this.browseFacet = checkNotNull(browseFacet);
    this.exceptionHandler = checkNotNull(exceptionHandler);
    this.timingHandler = checkNotNull(timingHandler);
    this.indexHtmlForwardHandler = checkNotNull(indexHtmlForwardHandler);
    this.securityHandler = checkNotNull(securityHandler);
    this.partialFetchHandler = checkNotNull(partialFetchHandler);
    this.contentHandler = checkNotNull(contentHandler);
    this.conditionalRequestHandler = checkNotNull(conditionalRequestHandler);
    this.contentHeadersHandler = checkNotNull(contentHeadersHandler);
    this.lastDownloadedHandler = checkNotNull(lastDownloadedHandler);
    this.handlerContributor = checkNotNull(handlerContributor);
    this.contentDispositionHandler = checkNotNull(contentDispositionHandler);
  }
}
