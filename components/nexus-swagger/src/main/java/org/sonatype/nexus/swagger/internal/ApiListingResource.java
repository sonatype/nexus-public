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
package org.sonatype.nexus.swagger.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.sonatype.nexus.rest.Resource;

import io.swagger.models.Swagger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Siesta-managed {@link io.swagger.jaxrs.listing.ApiListingResource}.
 * 
 * @since 3.3
 */
@Named
@Singleton
@Path("/swagger.{type:json|yaml}")
public class ApiListingResource
    extends io.swagger.jaxrs.listing.ApiListingResource
    implements Resource
{
  private final SwaggerModel swaggerModel;

  @Inject
  public ApiListingResource(final SwaggerModel swaggerModel) { // NOSONAR
    this.swaggerModel = checkNotNull(swaggerModel);
  }

  @Override
  protected Swagger process(
      final Application app,
      final ServletContext servletContext,
      final ServletConfig servletConfig,
      final HttpHeaders httpHeaders,
      final UriInfo uriInfo)
  {
    // update cached model to use base path calculated from incoming request
    return swaggerModel.getSwagger().basePath(uriInfo.getBaseUri().getPath());
  }
}
