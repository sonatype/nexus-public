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
package org.sonatype.nexus.extjs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.plugin.logging.Log;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * File appender.
 *
 * @since 3.0
 */
public class FileAppender
{
  protected final Log log;

  protected final Writer output;

  public FileAppender(final Log log, final Writer output) {
    this.log = checkNotNull(log);
    this.output = checkNotNull(output);
  }

  protected File source;

  protected long linenum;

  public void append(final File source) throws IOException {
    this.source = checkNotNull(source);
    this.linenum = 0;

    log.debug("Appending: " + source);

    BufferedReader input = new BufferedReader(new FileReader(source));
    try {
      String line;
      while ((line = input.readLine()) != null) {
        linenum++;
        append(line);
      }
    }
    finally {
      input.close();
    }

    output.write('\n');
  }

  protected void append(final String line) throws IOException {
    output.write(line);
    output.write('\n');
  }
}
