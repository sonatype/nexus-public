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
package org.sonatype.repository.conan.internal.orient.proxy.v1

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker
import org.sonatype.repository.conan.internal.ConanFormat
import org.sonatype.repository.conan.internal.ConanRecipeSupport
import org.sonatype.repository.conan.internal.security.token.ConanTokenFacet

import com.google.inject.Provider

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound

/**
 * @since 3.next
 */
@Named(ConanProxyRecipe.NAME)
@Singleton
class ConanProxyRecipe
  extends ConanRecipeSupport
{
  public static final String NAME = 'conan-proxy'

  @Inject
  Provider<ConanProxyFacet> proxyFacet

  @Inject
  HighAvailabilitySupportChecker highAvailabilitySupportChecker

  @Inject
  ProxyHandler proxyHandler

  @Inject
  Provider<ConanTokenFacet> tokenFacet

  private ConanProxyApiV1 conanApiV1

  @Inject
  ConanProxyRecipe(@Named(ProxyType.NAME) final Type type,
                   @Named(ConanFormat.NAME) final Format format,
                   final ConanProxyApiV1 conanApiV1) {
    super(type, format)
    this.conanApiV1 = conanApiV1
  }

  @Override
  boolean isFeatureEnabled() {
    return highAvailabilitySupportChecker.isSupported(getFormat().getValue())
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(tokenFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(componentMaintenanceFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(purgeUnusedFacet.get())
  }

  ViewFacet configure(ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    addBrowseUnsupportedRoute(builder)

    conanApiV1.create(builder);

    builder.defaultHandlers(notFound())
    facet.configure(builder.create())
    return facet
  }
}
