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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.nexus.logging.LoggingPlugin;
import org.sonatype.nexus.logging.model.MarkerXO;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.siesta.common.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Log REST resource.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(LogResource.RESOURCE_URI)
public class LogResource
    extends ComponentSupport
    implements Resource
{

  public static final String RESOURCE_URI = LoggingPlugin.REST_PREFIX + "/log";

  private static final Logger log = LoggerFactory.getLogger(LogResource.class);

  private final LogManager logManager;

  @Inject
  public LogResource(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  /**
   * Downloads a part of nexus.log (specified by fromByte/bytesCount) or full nexus.log (if fromByte/bytesCount are
   * null).
   *
   * @param fromByte   starting position
   * @param bytesCount number of bytes
   * @return part or full nexus.log
   * @throws Exception If getting log fails
   */
  @GET
  @Produces({TEXT_PLAIN})
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOG + "read")
  public Response get(final @QueryParam("fromByte") Long fromByte,
                      final @QueryParam("bytesCount") Long bytesCount)
      throws Exception
  {
    Long from = fromByte;
    if (from == null || from < 0) {
      from = 0L;
    }
    Long count = bytesCount;
    if (count == null) {
      count = Long.MAX_VALUE;
    }
    return Response.ok(logManager.getApplicationLogAsStream("nexus.log", from, count).getInputStream())
        .header("Content-Disposition", "attachment; filename=\"nexus.log\"")
        .build();
  }

  /**
   * Logs a message at INFO level.
   *
   * @param marker message to be logger (cannot be null/empty)
   * @throws NullPointerException     If marker is null
   * @throws IllegalArgumentException If marker message is null or empty
   */
  @PUT
  @Path("/mark")
  @Consumes({APPLICATION_JSON, APPLICATION_XML})
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  @RequiresPermissions(LoggingPlugin.PERMISSION_PREFIX_LOGGERS + "update")
  public void put(final MarkerXO marker)
      throws Exception
  {
    checkNotNull(marker);
    checkArgument(StringUtils.isNotEmpty(marker.getMessage()));

    // ensure that level for marking logger is enabled
    logManager.setLoggerLevel(log.getName(), LoggerLevel.INFO);

    String asterixes = StringUtils.repeat("*", marker.getMessage().length() + 4);
    log.info("\n"
        + asterixes + "\n"
        + "* " + marker.getMessage() + " *" + "\n"
        + asterixes
    );
  }

}
