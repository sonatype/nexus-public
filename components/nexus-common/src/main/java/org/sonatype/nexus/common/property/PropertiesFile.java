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

import org.sonatype.goodies.common.FileReplacer;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Persistent properties file.
 *
 * @since 3.0
 */
public class PropertiesFile
    extends ImplicitSourcePropertiesFile
{
  private static final Logger log = LoggerFactory.getLogger(PropertiesFile.class);

  private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSSZ");
  
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
    store(null);
  }

  /**
   * Store the file with specific comments. If none are provided a timestamp will be added. This is done using
   * the same pattern used in log files to ease reconciliation of timing across the system.
   * @since 3.7
   */
  public void store(final String comments) throws IOException {
    log.debug("Storing: {}", file);
    FileReplacer replacer = new FileReplacer(file);
    replacer.setDeleteBackupFile(true);
    String comment = comments != null ? comments : timestamp();
    replacer.replace(output -> store(output, comment));
  }

  private String timestamp() {
    return new DateTime().toString(FORMATTER);
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean exists() throws IOException {
    return file.exists();
  }

  @Override
  public String toString() {
    return file + " " + super.toString();
  }
}
