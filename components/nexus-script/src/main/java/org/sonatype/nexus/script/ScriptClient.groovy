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
package org.sonatype.nexus.script

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.TEXT_PLAIN

/**
 * Public API for managing Scripts. Provides BREAD capabilities.
 * 
 * @since 3.0
 */
@Path(ScriptClient.RESOURCE_URI)
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
interface ScriptClient
{
  public static final String RESOURCE_URI = '/v1/script'
  
  public static final String RUN_ACTION = 'run'

  /**
   * Browse all {@link Script}.
   */
  @GET
  List<ScriptXO> browse()

  /**
   * Get a specific {@link Script} by name.
   */
  @GET
  @Path('{name}')
  ScriptXO read(@PathParam('name') String name)

  /**
   * Edit an existing {@link Script}.
   */
  @PUT
  @Path('{name}')
  void edit(@PathParam('name') String name, @NotNull @Valid ScriptXO scriptXO)

  /**
   * Add a new {@link Script}.
   */
  @POST
  void add(@NotNull @Valid ScriptXO scriptXO)

  /**
   * Delete an existing {@link Script}.
   */
  @DELETE
  @Path('{name}')
  void delete(@PathParam('name') String name)

  /**
   * Run an existing {@link Script}.
   * @param name the name of the Script to execute
   * @param args the arguments to pass to the Script
   */
  @POST
  @Path('{name}/run')
  @Consumes([TEXT_PLAIN, APPLICATION_JSON])
  ScriptResultXO run(@PathParam('name') String name, String args)

}
