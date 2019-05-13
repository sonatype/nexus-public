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
package org.sonatype.nexus.script.plugin.internal.rest

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.NotFoundException
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Response

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.common.script.ScriptService
import org.sonatype.nexus.rest.Resource
import org.sonatype.nexus.script.Script
import org.sonatype.nexus.script.ScriptClient
import org.sonatype.nexus.script.ScriptManager
import org.sonatype.nexus.script.ScriptResultXO
import org.sonatype.nexus.script.ScriptXO
import org.sonatype.nexus.script.plugin.internal.security.ScriptPermission
import org.sonatype.nexus.security.BreadActions
import org.sonatype.nexus.security.SecurityHelper

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.LoggerFactory

import static com.google.common.base.Preconditions.checkArgument
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport.ALL

/**
 * BREAD resource for managing {@link Script} instances.
 *
 * @since 3.0
 */
@Named
@Singleton
@Path(ScriptResource.RESOURCE_URI)
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
@Api('script')
class ScriptResource
    extends ComponentSupport
    implements ScriptClient, Resource
{
  public static final String RESOURCE_URI = '/v1/script'

  @Inject
  ScriptManager scriptManager

  @Inject
  SecurityHelper securityHelper

  @Inject
  ScriptService scriptService

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('List all stored scripts')
  List<ScriptXO> browse() {
    securityHelper.ensurePermitted(scriptPermission(ALL, BreadActions.BROWSE))
    return scriptManager.browse().collect { convert(it) }
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('Read stored script by name')
  @ApiResponses(
      @ApiResponse(code = 404, message = 'No script with the specified name')
  )
  ScriptXO read(@PathParam('name') final String name) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.READ))
    return convert(findOr404(name))
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('Update stored script by name')
  @ApiResponses([
      @ApiResponse(code = 204, message = 'Script was updated'),
      @ApiResponse(code = 404, message = 'No script with the specified name')
  ])
  void edit(@PathParam('name') final String name, @NotNull @Valid final ScriptXO scriptXO) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.EDIT))
    checkArgument(name == scriptXO.name, "Path parameter: $name does not match data name: ${scriptXO.name}")
    findOr404(name)
    log.debug('Updating Script named: {}', name)
    scriptManager.update(name, scriptXO.content)
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('Add a new script')
  @ApiResponses(
      @ApiResponse(code = 204, message = 'Script was added')
  )
  void add(@NotNull @Valid final ScriptXO scriptXO) {
    securityHelper.ensurePermitted(scriptPermission(ALL, BreadActions.ADD))
    log.debug('Adding Script named: {}', scriptXO.name)
    scriptManager.create(scriptXO.name, scriptXO.content, scriptXO.type)
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('Delete stored script by name')
  @ApiResponses([
      @ApiResponse(code = 204, message = 'Script was deleted'),
      @ApiResponse(code = 404, message = 'No script with the specified name')
  ])
  void delete(@PathParam('name') final String name) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.DELETE))
    log.debug('Deleting Script named: {}', name)
    scriptManager.delete(findOr404(name).name)
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation('Run stored script by name')
  @ApiResponses([
      @ApiResponse(code = 404, message = 'No script with the specified name'),
      @ApiResponse(code = 500, message = 'Script execution failed with exception')
  ])
  ScriptResultXO run(final @PathParam('name') String name, final String args) {
    securityHelper.ensurePermitted(scriptPermission(name, RUN_ACTION))
    log.debug('Running Script named: {}', name)
    Script script = findOr404(name)

    // execute, capturing any possible errors
    Object result
    try {
      result = scriptService.eval(script.type, script.content,
          [
              log : LoggerFactory.getLogger(this.getClass()),
              args: args?.trim(),
              scriptName: script.name
          ]
      )
    }
    catch (e) {
      log.error('Exception in script execution for script named: {}', name, e)
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(new ScriptResultXO(script.name, e.message)).build()
      )
    }
    log.trace('Result: {}', result)
    return new ScriptResultXO(name: name, result: result.toString())
  }

  private Script findOr404(String name) {
    Script script = scriptManager.get(name)
    if (!script) {
      throw new NotFoundException("Script with name: '${name}' not found")
    }
    return script
  }

  static ScriptXO convert(final Script script) {
    new ScriptXO(script.name, script.content, script.type)
  }

  static ScriptPermission scriptPermission(final String name, final String action) {
    new ScriptPermission(name, [action])
  }
}
