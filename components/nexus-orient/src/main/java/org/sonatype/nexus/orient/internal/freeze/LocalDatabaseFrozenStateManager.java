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
package org.sonatype.nexus.orient.internal.freeze;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.freeze.DatabaseFrozenStateManager;

/**
 * Default implementation of {@link DatabaseFrozenStateManager}
 *
 * @since 3.3
 */
@Named("local")
@Singleton
public class LocalDatabaseFrozenStateManager
    extends ComponentSupport
    implements DatabaseFrozenStateManager
{
  private static final String FROZEN_MARKER = "frozen.marker";

  private final File frozenMarkerFile;

  @Inject
  public LocalDatabaseFrozenStateManager(final ApplicationDirectories applicationDirectories) {
    this.frozenMarkerFile = new File(applicationDirectories.getWorkDirectory("db"), FROZEN_MARKER);
  }

  @Override
  public boolean get() {
    return frozenMarkerFile.exists();
  }

  @Override
  public void set(final boolean frozen) {
    if (frozen) {
      try {
        frozenMarkerFile.createNewFile();
      }
      catch (IOException e) {
        log.error("Unable to create database frozen state marker file {}", frozenMarkerFile, e);
      }
    }
    else {
      try {
        Files.deleteIfExists(frozenMarkerFile.toPath());
      }
      catch (IOException e) {
        log.error("Unable to delete database frozen state marker file {}", frozenMarkerFile, e);
      }
    }
  }
}
