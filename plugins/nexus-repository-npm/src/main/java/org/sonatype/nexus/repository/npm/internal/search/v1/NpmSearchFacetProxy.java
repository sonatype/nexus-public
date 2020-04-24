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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import java.io.IOException;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmProxyFacet.ProxyTarget;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Parameters;
import org.sonatype.nexus.repository.view.Request;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * Implementation of {@code NpmSearchFacet} for proxy repositories.
 *
 * @since 3.7
 */
@Named
public class NpmSearchFacetProxy
    extends FacetSupport
    implements NpmSearchFacet
{
  @Override
  public Content searchV1(final Parameters parameters) throws IOException {
    try {
      final Request getRequest = new Request.Builder()
          .action(GET)
          .path("/" + NpmFacetUtils.REPOSITORY_SEARCH_ASSET)
          .parameters(parameters)
          .build();
      Context context = new Context(getRepository(), getRequest);
      context.getAttributes().set(ProxyTarget.class, ProxyTarget.SEARCH_V1_RESULTS);
      Content searchResults = getRepository().facet(ProxyFacet.class).get(context);
      if (searchResults == null) {
        throw new IOException("Could not retrieve registry search");
      }
      return searchResults;
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }
}
