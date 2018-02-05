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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link TestWatcher} which cleans selected files when the test passes.
 * <p>
 * Use {@code -Dtest.cleanOnSuccess=false} to disable cleaning of test data.
 *
 * @since 3.1
 */
public class TestCleaner
    extends TestWatcher
{
  public static final String CLEAN_ON_SUCCESS_KEY = "test.cleanOnSuccess";

  private static final boolean CLEAN_ON_SUCCESS = !"false".equalsIgnoreCase(System.getProperty(CLEAN_ON_SUCCESS_KEY));

  private static final Logger log = LoggerFactory.getLogger(TestCleaner.class);

  public void cleanOnSuccess(final File file) {
    if (CLEAN_ON_SUCCESS) {
      TreeCleaner.cleanOnSuccess.add(file.toPath());
    }
  }

  @Override
  protected void failed(final Throwable cause, final Description description) {
    TreeCleaner.failed = true; // NOSONAR: flag is only read at JVM shutdown
  }

  /**
   * Use nested class to avoid eager registration of the shutdown hook.
   */
  private static class TreeCleaner
      extends SimpleFileVisitor<Path>
  {
    static final Set<Path> cleanOnSuccess = Collections.synchronizedSet(new HashSet<>());

    static volatile boolean failed;

    static {
      Runtime.getRuntime().addShutdownHook(new Thread(new TreeCleaner()::clean));
    }

    private void clean() {
      if (!failed) {
        cleanOnSuccess.forEach(this::cleanTree);
      }
    }

    private void cleanTree(final Path path) {
      try {
        Files.walkFileTree(path, this);
      }
      catch (final IOException e) {
        log.warn("Unable to clean {}", path, e);
      }
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
      try {
        Files.delete(file);
      }
      catch (final IOException e) {
        log.warn("Unable to clean {}", file, e);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException ioe) {
      try {
        Files.delete(dir);
      }
      catch (final IOException e) {
        log.warn("Unable to clean {}", dir, e);
      }
      return FileVisitResult.CONTINUE;
    }
  }
}
