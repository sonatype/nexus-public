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
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.content.maven.MavenArchetypeCatalogFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * Fetches or rebuilds the maven archetype catalog for a given repository.
 *
 * @since 3.25.0
 */
@Named
@Singleton
public class MavenArchetypeCatalogHandler
    extends ComponentSupport
    implements Handler
{
  @Nonnull
  @Override
  public Response handle(
      @Nonnull final Context context) throws Exception
  {
    String method = context.getRequest().getAction();
    switch (method) {
      case GET:
      case HEAD:
        return fetchOrGenerateArchetypeCatalog(context);
      default:
        return HttpResponses.methodNotAllowed(context.getRequest().getAction(), GET, HEAD);
    }
  }

  private Response fetchOrGenerateArchetypeCatalog(final Context context) throws Exception {
    Response response = context.proceed();
    if (!response.getStatus().isSuccessful()) {
      return generateArchetypeCatalog(context);
    }
    else {
      return response;
    }
  }

  private Response generateArchetypeCatalog(final Context context) throws Exception {
    Repository repository = context.getRepository();
    MavenArchetypeCatalogFacet archetypeCatalogFacet = repository.facet(MavenArchetypeCatalogFacet.class);
    archetypeCatalogFacet.rebuildArchetypeCatalog();
    return context.proceed();
  }
}
