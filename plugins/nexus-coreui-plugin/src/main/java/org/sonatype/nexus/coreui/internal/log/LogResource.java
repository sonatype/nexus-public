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
package org.sonatype.nexus.coreui.internal.log;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.sonatype.nexus.common.log.LogManager.DEFAULT_LOGGER;

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
  public static final String RESOURCE_URI = "/logging/log";

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
  @RequiresPermissions("nexus:logging:read")
  public Response get(@QueryParam("fromByte") final Long fromByte,
                      @QueryParam("bytesCount") final Long bytesCount)
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
    String logName = logManager.getLogFor(DEFAULT_LOGGER)
        .orElseThrow(() -> new NotFoundException("Failed to determine log file name for " + DEFAULT_LOGGER));
    InputStream log = logManager.getLogFileStream(logName, from, count);
    if (log == null) {
      throw new NotFoundException("nexus.log not found");
    }
    return Response.ok(log)
        .header(CONTENT_DISPOSITION, format("attachment; filename=\"%s\"", logName))
        .build();
  }
}