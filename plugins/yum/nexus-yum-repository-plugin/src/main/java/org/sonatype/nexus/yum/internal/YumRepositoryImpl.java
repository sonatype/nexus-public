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
package org.sonatype.nexus.yum.internal;

import java.io.File;
import java.util.Arrays;

import org.sonatype.nexus.yum.YumRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since yum 3.0
 */
public class YumRepositoryImpl
    implements YumRepository
{

  private static final Logger LOG = LoggerFactory.getLogger(YumRepositoryImpl.class);

  private final File baseDir;

  private boolean dirty = false;

  private final String version;

  private final String id;

  public YumRepositoryImpl(final File baseDir, final String repositoryId, final String version) {
    this.baseDir = baseDir;
    this.id = repositoryId;
    this.version = version;
    if (LOG.isDebugEnabled()) {
      String[] files = null;
      final File repodata = new File(baseDir, "repodata");
      if (repodata.exists() && repodata.isDirectory()) {
        files = repodata.list();
      }
      LOG.debug(
          "Yum repository {}/{} available at {} contains {}",
          repositoryId, version, repodata.getAbsolutePath(), files == null ? "no files" : Arrays.toString(files)
      );
    }
  }

  public File getBaseDir() {
    return baseDir;
  }

  @Override
  public File resolvePath(final String path) {
    return (path == null || "/".equals(path)) ? baseDir : new File(baseDir, path.trim());
  }

  public boolean isDirty() {
    return dirty;
  }

  public void setDirty() {
    this.dirty = true;
  }

  public String version() {
    return version;
  }

  public String nexusRepositoryId() {
    return id;
  }

}
