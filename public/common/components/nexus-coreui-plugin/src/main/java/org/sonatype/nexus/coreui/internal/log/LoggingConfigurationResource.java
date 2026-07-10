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

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.common.log.LoggerLevel;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.repository.BadRequestException;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toSet;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.stereotype.Component;

@Component
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(LoggingConfigurationResource.RESOURCE_PATH)
public class LoggingConfigurationResource
    implements Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String RESOURCE_PATH = "internal/ui/loggingConfiguration";

  private static final String ROOT = "ROOT";

  private static final ReadWriteLock lock = new ReentrantReadWriteLock();

  private LogManager logManager;

  @Autowired
  public LoggingConfigurationResource(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @GET
  @RequiresPermissions("nexus:logging:read")
  public Collection<LoggerXO> readAll() {
    lock.readLock().lock();
    try {
      return logManager.getEffectiveLoggersUpdatedByFetchedOverrides()
          .entrySet()
          .stream()
          .map(LoggerXO::fromEntry)
          .collect(toSet());
    }
    finally {
      lock.readLock().unlock();
    }
  }

  @POST
  @Path("/reset")
  @RequiresPermissions("nexus:logging:update")
  public void resetAll() {
    lock.writeLock().lock();
    try {
      logManager.resetLoggers();
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @GET
  @Path("/{name}")
  @RequiresPermissions("nexus:logging:read")
  public LoggerXO read(@PathParam("name") final String name) {
    lock.readLock().lock();
    try {
      LoggerXO logger = new LoggerXO();
      logger.setName(name);
      logger.setLevel(logManager.getLoggerEffectiveLevel(name));
      logger.setOverride(logManager.getLoggers().containsKey(name));
      return logger;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  @PUT
  @Path("/{name}")
  @RequiresPermissions("nexus:logging:update")
  public void update(@PathParam("name") final String name, final UpdateLoggingConfigurationRequest request) {
    lock.writeLock().lock();
    try {
      if (request.getLevel() == LoggerLevel.DEFAULT) {
        logManager.unsetLoggerLevel(name);
      }
      else {
        logManager.setLoggerLevel(name, request.getLevel());
      }
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  @POST
  @Path("/{name}/reset")
  @RequiresPermissions("nexus:logging:update")
  public void reset(@PathParam("name") final String name) {
    lock.writeLock().lock();
    try {
      logManager.unsetLoggerLevel(name);
    }
    finally {
      lock.writeLock().unlock();
    }
  }
}
