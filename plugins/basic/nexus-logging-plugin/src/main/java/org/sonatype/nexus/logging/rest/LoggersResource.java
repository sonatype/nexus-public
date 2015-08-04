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
package org.sonatype.nexus.logging.rest;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.logging.LoggingConfigurator;
import org.sonatype.nexus.logging.LoggingPlugin;
import org.sonatype.nexus.logging.model.LoggerXO;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.siesta.common.Resource;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Loggers REST resource.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(LoggersResource.RESOURCE_URI)
public class LoggersResource
    extends ComponentSupport
    implements Resource
{

  public static final String RESOURCE_URI = LoggingPlugin.REST_PREFIX + "/loggers";

  private final LoggingConfigurator configurator;

  @Inject
  public LoggersResource(final LoggingConfigurator configurator) {
    this.configurator = checkNotNull(configurator, "configurator");
  }

  /**
   * Returns a list of configured loggers (never null).
   *
   * @see LoggingConfigurator#getLoggers()
   */
  @GET
  @Produces({APPLICATION_JSON, APPLICATION_XML})
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "read")
  public List<LoggerXO> get() {
    return Lists.newArrayList(configurator.getLoggers());
  }

  /**
   * Helper to check precondition of logger name.
   */
  private static void checkLoggerName(final String name) {
    checkArgument(name.equals(StringEscapeUtils.escapeHtml(name)), "Invalid logger name");
  }

  /**
   * Sets the level of a logger.
   *
   * @param logger logger name/level (cannot be null)
   * @throws NullPointerException     If logger is null or level is empty
   * @throws IllegalArgumentException If logger name is null or empty
   */
  @POST
  @Consumes({APPLICATION_JSON, APPLICATION_XML})
  @Produces({APPLICATION_JSON, APPLICATION_XML})
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "update")
  public LoggerXO post(final LoggerXO logger)
      throws Exception
  {
    checkNotNull(logger, "logger");
    checkNotNull(logger.getLevel(), "logger level");
    checkArgument(StringUtils.isNotEmpty(logger.getName()), "name cannot be empty");
    checkLoggerName(logger.getName());

    return logger.withLevel(configurator.setLevel(logger.getName(), logger.getLevel()));
  }

  /**
   * Sets the level of a logger.
   *
   * @param name   logger name
   * @param logger logger name/level (cannot be null)
   * @throws NullPointerException If name is null or logger is null or level is null
   */
  @PUT
  @Path("/{name}")
  @Consumes({APPLICATION_JSON, APPLICATION_XML})
  @Produces({APPLICATION_JSON, APPLICATION_XML})
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "update")
  public LoggerXO put(final @PathParam("name") String name,
                      final LoggerXO logger)
      throws Exception
  {
    checkNotNull(name, "name");
    checkLoggerName(name);
    checkNotNull(logger, "logger");
    checkNotNull(logger.getLevel(), "logger level");

    return logger.withLevel(configurator.setLevel(name, logger.getLevel()));
  }

  /**
   * Un-sets the level of a logger.
   *
   * @param name logger name
   * @throws NullPointerException If name is null
   */
  @DELETE
  @Path("/{name}")
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "update")
  public void delete(final @PathParam("name") String name)
      throws Exception
  {
    checkNotNull(name, "name");

    configurator.remove(name);
  }

  /**
   * Resets all loggers to their default levels.
   */
  @DELETE
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "update")
  public void reset()
      throws Exception
  {
    configurator.reset();
  }


}
