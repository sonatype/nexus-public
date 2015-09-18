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
package org.sonatype.nexus.orient;

import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.junit.rules.ExternalResource;
import org.junit.runners.model.MultipleFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * JUnit rule to provide {@link DatabaseInstance} and related components.
 *
 * @since 3.0
 */
public class DatabaseInstanceRule
  extends ExternalResource
{
  static {
    try {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    } catch (LinkageError e) {
      // no-op, jul-to-slf4j not installed
    }
  }

  private static final Logger log = LoggerFactory.getLogger(DatabaseInstanceRule.class);

  private final String name;

  private MinimalDatabaseServer server;

  private MemoryDatabaseManager manager;

  private DatabaseInstance instance;

  public DatabaseInstanceRule(final String name) {
    this.name = checkNotNull(name);
  }

  public MinimalDatabaseServer getServer() {
    checkState(server != null);
    return server;
  }

  public MemoryDatabaseManager getManager() {
    checkState(manager != null);
    return manager;
  }

  public DatabaseInstance getInstance() {
    checkState(instance != null);
    return instance;
  }

  @Override
  protected void before() throws Throwable {
    log.info("Preparing database instance: {}", name);

    server = new MinimalDatabaseServer();
    server.start();

    manager = new MemoryDatabaseManager();
    manager.start();

    instance = manager.instance(name);

    log.info("Database instance prepared");
  }

  @Override
  protected void after() {
    log.info("Cleaning up database instance: {}", name);

    instance = null;

    // capture any errors so we can propagate to test harness
    List<Throwable> errors = Lists.newArrayListWithCapacity(2);

    if (manager != null) {
      try {
        manager.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop database manager", e);
        errors.add(e);
      }
      manager = null;
    }

    if (server != null) {
      try {
        server.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop database server", e);
        errors.add(e);
      }
      server = null;
    }

    if (!errors.isEmpty()) {
      log.error("Failed to clean up database instance");
      throw Throwables.propagate(new MultipleFailureException(errors));
    }

    log.info("Database instance cleaned up");
  }
}
