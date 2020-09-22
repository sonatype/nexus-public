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
package org.sonatype.repository.conan.internal.orient.proxy.v1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.repository.conan.internal.orient.common.v1.ConanControllerV1;
import org.sonatype.repository.conan.internal.common.v1.ConanRoutes;
import org.sonatype.repository.conan.internal.proxy.v1.ConanProxyHandlers;

import static org.sonatype.repository.conan.internal.AssetKind.DIGEST;

/**
 * @since 3.next
 */
@Named
@Singleton
public class ConanProxyControllerV1
    extends ConanControllerV1
{
  @Inject
  private ConanProxyHandlers conanProxyHandlers;

  public void attach(final Router.Builder builder) {
    createRoute(builder, ConanRoutes.digest(), DIGEST, conanProxyHandlers.proxyHandler);
    createRoute(builder, ConanRoutes.searchQuery(), null, conanProxyHandlers.searchQueryHandler);
    createGetRoutes(builder, conanProxyHandlers.proxyHandler, conanProxyHandlers.proxyHandler, conanProxyHandlers.proxyHandler);
  }
}
