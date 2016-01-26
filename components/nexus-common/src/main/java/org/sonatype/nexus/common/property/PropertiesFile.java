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
package org.sonatype.nexus.common.property;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.sonatype.goodies.common.FileReplacer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Persistent properties file.
 *
 * @since 3.0
 */
public class PropertiesFile
    extends Properties
{
  private static final Logger log = LoggerFactory.getLogger(PropertiesFile.class);

  private final File file;

  public PropertiesFile(final File file) {
    this.file = checkNotNull(file);
  }

  public void load() throws IOException {
    log.debug("Loading: {}", file);
    try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
      load(in);
    }
  }

  public void store() throws IOException {
    log.debug("Storing: {}", file);
    FileReplacer replacer = new FileReplacer(file);
    replacer.setDeleteBackupFile(true);
    replacer.replace(output -> store(output, null));
  }

  public File getFile() {
    return file;
  }
}
