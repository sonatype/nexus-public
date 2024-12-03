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
package org.sonatype.nexus.script.plugin.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.script.ScriptService;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptClient;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.ScriptResultXO;
import org.sonatype.nexus.script.ScriptRunEvent;
import org.sonatype.nexus.script.ScriptXO;
import org.sonatype.nexus.script.plugin.internal.ScriptingDisabledException;
import org.sonatype.nexus.script.plugin.internal.security.ScriptPermission;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * BREAD resource for managing {@link Script} instances.
 *
 * @since 3.0
 */
@Named
@Singleton
@Path(ScriptResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Script")
public class ScriptResource
    extends ComponentSupport
    implements ScriptClient, Resource
{
  public static final String RESOURCE_URI = "/v1/script";

  private final ScriptManager scriptManager;

  private final SecurityHelper securityHelper;

  private final ScriptService scriptService;

  private final EventManager eventManager;

  @Inject
  public ScriptResource(
      final ScriptManager scriptManager,
      final SecurityHelper securityHelper,
      final ScriptService scriptService,
      final EventManager eventManager)
  {
    this.scriptManager = checkNotNull(scriptManager);
    this.securityHelper = checkNotNull(securityHelper);
    this.scriptService = checkNotNull(scriptService);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("List all stored scripts")
  @RequiresPermissions("nexus:script:*:browse")
  public List<ScriptXO> browse() {
    List<ScriptXO> storedScripts = new ArrayList<>();
    scriptManager.browse()
        .forEach(script -> storedScripts.add(convert(script)));
    return storedScripts;
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("Read stored script by name")
  @ApiResponses(@ApiResponse(code = 404, message = "No script with the specified name"))
  public ScriptXO read(@PathParam("name") final String name) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.READ));
    return convert(findOr404(name));
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("Update stored script by name")
  @ApiResponses({
      @ApiResponse(code = 204, message = "Script was updated"),
      @ApiResponse(code = 404, message = "No script with the specified name"),
      @ApiResponse(code = 410, message = "Script updating is disabled")
  })
  public void edit(@PathParam("name") final String name, @NotNull @Valid final ScriptXO scriptXO) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.EDIT));
    checkArgument(name.equals(scriptXO.getName()),
        "Path parameter: " + name + " does not match data name: " + scriptXO.getName());
    findOr404(name);
    log.debug("Updating Script named: {}", name);
    try {
      scriptManager.update(name, scriptXO.getContent());
    }
    catch (ScriptingDisabledException e) { // NOSONAR
      log.debug("Failed to update script {}, creating and updating scripts is disabled", name, e);
      throw new WebApplicationException(
          Response.status(Response.Status.GONE).entity(new ScriptResultXO(name, e.getMessage())).build());
    }
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("Add a new script")
  @ApiResponses({
      @ApiResponse(code = 204, message = "Script was added"),
      @ApiResponse(code = 410, message = "Script creation is disabled")
  })
  @RequiresPermissions("nexus:script:*:add")
  public void add(@NotNull @Valid final ScriptXO scriptXO) {
    log.debug("Adding Script named: {}", scriptXO.getName());
    try {
      scriptManager.create(scriptXO.getName(), scriptXO.getContent(), scriptXO.getType());
    }
    catch (ScriptingDisabledException e) { // NOSONAR
      log.debug("Failed to create script {}, creating and updating scripts is disabled", scriptXO.getName(), e);
      throw new WebApplicationException(
          Response.status(Response.Status.GONE).entity(new ScriptResultXO(scriptXO.getName(), e.getMessage())).build());
    }
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("Delete stored script by name")
  @ApiResponses({
      @ApiResponse(code = 204, message = "Script was deleted"),
      @ApiResponse(code = 404, message = "No script with the specified name")
  })
  public void delete(@PathParam("name") final String name) {
    securityHelper.ensurePermitted(scriptPermission(name, BreadActions.DELETE));
    log.debug("Deleting Script named: {}", name); // NOSONAR
    scriptManager.delete(findOr404(name).getName());
  }

  @Override
  @Timed
  @ExceptionMetered
  @ApiOperation("Run stored script by name")
  @ApiResponses({
      @ApiResponse(code = 404, message = "No script with the specified name"),
      @ApiResponse(code = 500, message = "Script execution failed with exception")
  })
  public ScriptResultXO run(@PathParam("name") final String name, final String args) {
    securityHelper.ensurePermitted(scriptPermission(name, RUN_ACTION));
    log.debug("Running Script named: {}", name); // NOSONAR
    Script script = findOr404(name);

    // execute, capturing any possible errors
    Object result;
    try {
      Map<String, Object> customBindings = new HashMap<>();
      customBindings.put("log", LoggerFactory.getLogger(this.getClass()));
      customBindings.put("args", args != null ? args.trim() : null);
      customBindings.put("scriptName", script.getName());
      result = scriptService.eval(script.getType(), script.getContent(), customBindings);
      eventManager.post(new ScriptRunEvent(script));
    }
    catch (Exception e) { // NOSONAR
      log.error("Exception in script execution for script named: {}", name, e);
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST)
              .entity(new ScriptResultXO(script.getName(), e.getMessage()))
              .build());
    }
    log.trace("Result: {}", result);
    String resultString = Optional.ofNullable(result).map(Object::toString).orElse("");
    return new ScriptResultXO(name, resultString);
  }

  private Script findOr404(String name) {
    Script script = scriptManager.get(name);
    if (script == null) {
      throw new NotFoundException("Script with name: '" + name + "' not found");
    }
    return script;
  }

  private static ScriptXO convert(final Script script) {
    return new ScriptXO(script.getName(), script.getContent(), script.getType());
  }

  private static ScriptPermission scriptPermission(final String name, final String action) {
    return new ScriptPermission(name, Collections.singletonList(action));
  }
}
