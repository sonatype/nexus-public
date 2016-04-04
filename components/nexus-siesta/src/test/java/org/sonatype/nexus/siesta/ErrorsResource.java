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
package org.sonatype.nexus.siesta;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.sonatype.nexus.rest.Resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

@Named
@Singleton
@Path("/errors")
public class ErrorsResource
    implements Resource
{
  @GET
  @Path("/NotFoundException")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public Object throwObjectNotFoundException() {
    throw new NotFoundException("NotFoundException");
  }

  @GET
  @Path("/BadRequestException")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public Object throwBadRequestException() {
    throw new BadRequestException("BadRequestException");
  }

  @GET
  @Path("/406")
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  public Object throw406() {
    throw new WebApplicationException(406);
  }
}
