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
package org.sonatype.nexus.configuration.application;

import java.io.File;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link ApplicationDirectories} implementation.
 *
 * @since 2.8
 */
@Named
@Singleton
public class ApplicationDirectoriesImpl
  extends ComponentSupport
  implements ApplicationDirectories
{
  private final File installDir;

  private final File workDir;

  private final File tempDir;

  // FIXME: Normalize properties used, deferring for now until we redesign installation layout and boot configuration
  // FIXME: bundleBasedir -> nexus.install
  // FIXME: nexus-work -> nexus.work

  @Inject
  public ApplicationDirectoriesImpl(final @Named("${bundleBasedir}") @Nullable File installDir,
                                    final @Named("${nexus-work}") File workDir)
  {
    if (installDir != null) {
      this.installDir = resolve(installDir, false);
      log.debug("Install dir: {}", this.installDir);
    }
    else {
      this.installDir = null;
      log.debug("Install dir not available");
    }

    this.workDir = resolve(workDir, true);
    log.debug("Work dir: {}", this.workDir);

    // TODO: May want to consider having the application tmp dir as a well known sub-directory under java.io.tmpdir
    // TODO: similar to how jetty does this with "jetty-<addr>-<port>-<context>".  If we normalize on a nexus
    // TODO: instance/node identifiers then this could be "nexus-<node-id>".  This however would still be
    // TODO: prefixed in the java.io.tmpdir location

    // Resolve the tmp dir from system properties.
    String tmplocation = System.getProperty("java.io.tmpdir", "tmp");
    this.tempDir = resolve(new File(tmplocation), true);
    log.debug("Temp dir: {}", this.tempDir);
  }

  @Nullable
  @Override
  public File getInstallDirectory() {
    return installDir;
  }

  @Override
  public File getTemporaryDirectory() {
    return tempDir;
  }

  @Override
  public File getWorkDirectory() {
    return workDir;
  }

  @Override
  public File getWorkDirectory(final String path, final boolean create) {
    checkNotNull(path);
    File dir = new File(workDir, path);
    return resolve(dir, create);
  }

  @Override
  public File getWorkDirectory(final String path) {
    return getWorkDirectory(path, true);
  }

  private File resolve(File dir, final boolean create) {
    checkNotNull(dir);

    log.trace("Resolving directory: {}; create: {}", dir, create);
    try {
      dir = dir.getCanonicalFile();
    }
    catch (Exception e) {
      log.error("Failed to canonicalize directory: {}", dir);
      throw Throwables.propagate(e);
    }

    if (create && !dir.isDirectory()) {
      try {
        DirSupport.mkdir(dir.toPath());
        log.debug("Created directory: {}", dir);
      }
      catch (Exception e) {
        log.error("Failed to create directory: {}", dir);
        throw Throwables.propagate(e);
      }
    }

    return dir;
  }
}
