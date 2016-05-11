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
package org.sonatype.nexus.internal.app;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.KarafManager;

import com.google.common.io.Files;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link KarafManager} implementation.
 *
 * @since 3.1
 */
@Named
@Singleton
public class KarafManagerImpl
    extends ComponentSupport
    implements KarafManager
{
  private final ApplicationDirectories applicationDirectories;

  @Inject
  public KarafManagerImpl(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public void setCleanCacheOnRestart() {
    File workDirectory = applicationDirectories.getWorkDirectory();
    File cleanCacheFile = new File(workDirectory, "clean_cache");
    try {
      Files.touch(cleanCacheFile);
    }
    catch (IOException e) {
      log.error("Unable to touch clean_cache file", e);
    }
  }
}
