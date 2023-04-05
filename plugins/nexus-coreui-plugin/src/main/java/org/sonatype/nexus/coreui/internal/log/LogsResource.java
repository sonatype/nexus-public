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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.logging.task.TaskLogHome;
import org.sonatype.nexus.rest.APIConstants;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Logs REST resource.
 *
 * @since 3.3
 */
@Named
@Singleton
@Path(LogsResource.RESOURCE_URI)
public class LogsResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = APIConstants.INTERNAL_API_PREFIX + "/logging/logs";

  public static final String DEFAULT_MARK = "MARK";

  private final LogManager logManager;

  @Inject
  public LogsResource(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  /**
   * List the log files known by the system
   */
  @GET
  @Produces({APPLICATION_JSON})
  @RequiresPermissions("nexus:logging:read")
  public Set<LogXO> listLogs() throws IOException {

    Set<LogXO> logs = logManager.getLogFiles().stream().map(file -> new LogXO(file.toPath())).collect(toSet());

    if (TaskLogHome.getTaskLogsHome() != null) {
      aggregateLogs(logs, TaskLogHome.getTaskLogsHome());
    }

    if (TaskLogHome.getReplicationLogsHome().isPresent()) {
      aggregateLogs(logs, TaskLogHome.getReplicationLogsHome().get());
    }

    return logs;
  }

  private void aggregateLogs(final Set<LogXO> logs, final String pathname) throws IOException {
    if (pathname != null) {
      try (Stream<java.nio.file.Path> paths = Files.list(Paths.get(pathname))) {
        paths.filter(logManager::isValidLogFile).forEach(path -> logs.add(new LogXO(path)));
      }
    }
  }

  /**
   * Downloads a part of a log file or the complete log file if fromByte/bytesCount are null.
   */
  @GET
  @Path("/{filename: .*\\.log}")
  @Produces({TEXT_PLAIN})
  @RequiresPermissions("nexus:logging:read")
  public Response get(
      @PathParam("filename") final String filename,
      @QueryParam("fromByte") final Long fromByte,
      @QueryParam("bytesCount") final Long bytesCount)
      throws NotFoundException, IOException
  {
    Long from = fromByte;
    if (from == null || from < 0) {
      from = 0L;
    }
    Long count = bytesCount;
    if (count == null) {
      count = Long.MAX_VALUE;
    }
    InputStream log = logManager.getLogFileStream(filename, from, count);
    if (log == null) {
      throw new NotFoundException(format("%s not found", filename));
    }
    return Response.ok(log)
        .header(CONTENT_DISPOSITION, format("attachment; filename=\"%s\"", filename))
        .build();
  }
}
