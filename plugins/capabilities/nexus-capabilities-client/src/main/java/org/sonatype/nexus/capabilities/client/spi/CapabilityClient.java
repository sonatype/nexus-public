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
package org.sonatype.nexus.capabilities.client.spi;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.capabilities.model.CapabilityXO;
import org.sonatype.nexus.client.core.subsystem.SiestaClient;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 2.7
 */
@Path("/service/siesta/capabilities")
public interface CapabilityClient
    extends SiestaClient
{

  @GET
  @Consumes({APPLICATION_JSON})
  List<CapabilityStatusXO> get();

  @GET
  @Consumes({APPLICATION_JSON})
  List<CapabilityStatusXO> search(@QueryParam("filter") MultivaluedMap<String, String> filter);

  @GET
  @Consumes({APPLICATION_JSON})
  @Path("/{id}")
  CapabilityXO get(@PathParam("id") String id);

  @GET
  @Consumes({APPLICATION_JSON})
  @Path("/{id}/status")
  CapabilityStatusXO getStatus(@PathParam("id") String id);

  @POST
  @Produces({APPLICATION_JSON})
  @Consumes({APPLICATION_JSON})
  CapabilityStatusXO create(CapabilityXO capability);

  @DELETE
  @Path("/{id}")
  void delete(@PathParam("id") String id);

  @PUT
  @Produces({APPLICATION_JSON})
  @Consumes({APPLICATION_JSON})
  @Path("/{id}")
  CapabilityStatusXO update(@PathParam("id") String id, CapabilityXO capability);

  @PUT
  @Path("/{id}/enable")
  @Produces({APPLICATION_JSON})
  @Consumes({APPLICATION_JSON})
  CapabilityStatusXO enable(@PathParam("id") String id);

  @PUT
  @Path("/{id}/disable")
  @Produces({APPLICATION_JSON})
  @Consumes({APPLICATION_JSON})
  CapabilityStatusXO disable(@PathParam("id") String id);

}