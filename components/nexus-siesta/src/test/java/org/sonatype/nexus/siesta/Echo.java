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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Echo Resource client.
 */
@Path("/echo")
@Produces(MediaType.APPLICATION_JSON)
public interface Echo
{
  @GET
  List<String> get(@QueryParam("foo") String foo);

  @GET
  List<String> get(@QueryParam("foo") String foo, @QueryParam("bar") int bar);

  @GET
  List<String> get(@QueryParam("bar") int bar);

  @GET
  @Path("/multiple")
  List<String> get(@QueryParam("foo") String[] foo);

  @GET
  @Path("/multiple")
  List<String> get(@QueryParam("foo") Object[] foo);

  @GET
  @Path("/multiple")
  List<String> get(@QueryParam("foo") Collection<?> foo);

  @GET
  @Path("/multiple")
  List<String> get(@QueryParam("foo") Iterator<?> foo);

  @GET
  List<String> get(@QueryParam("params") MultivaluedMap<String, String> queryParams);
}
