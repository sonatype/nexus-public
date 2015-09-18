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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.common.io.DirSupport;

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
 * JUnit rule to provide a persistent {@link DatabaseInstance} and related components.
 *
 * @since 3.0
 */
public class PersistentDatabaseInstanceRule
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

  private static final Logger log = LoggerFactory.getLogger(PersistentDatabaseInstanceRule.class);

  private final String name;

  private MinimalDatabaseServer server;

  private PersistentDatabaseManager manager;

  private DatabaseInstance instance;

  public PersistentDatabaseInstanceRule(final String name) {
    this.name = checkNotNull(name);
  }

  public MinimalDatabaseServer getServer() {
    checkState(server != null);
    return server;
  }

  public DatabaseManager getManager() {
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

    manager = new PersistentDatabaseManager();
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

  class PersistentDatabaseManager
      extends DatabaseManagerSupport {

    private final File databasesDirectory;

    PersistentDatabaseManager() {
      File targetDir = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
      this.databasesDirectory = new File(targetDir, "test-db." + System.currentTimeMillis());
      log.info("Database dir: " + databasesDirectory);
      try { Thread.sleep(250); } catch (InterruptedException e) { }
    }

    /**
     * Returns the directory for the given named database.  Directory may or may not exist.
     */
    private File directory(final String name) throws IOException {
      return new File(databasesDirectory, name).getCanonicalFile();
    }

    @Override
    protected String connectionUri(final String name) {
      try {
        File dir = directory(name);
        DirSupport.mkdir(dir);

        return "plocal:" + dir.toURI().getPath();
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }


  }
}


