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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.maven.plugin.logging.Log;

/**
 * Omission token aware file appender.
 *
 * @since 3.0
 */
public class OmissionFileAppender
    extends FileAppender
{
  public static final String IF_START_PREFIX = "//<if ";

  public static final String IF_START_SUFFIX = ">";

  public static final String IF_END = "//</if>";

  private final Map<String, Boolean> flags = new HashMap<>();

  public OmissionFileAppender(
      final Log log,
      final Writer output,
      @Nullable final Map<String, String> rawFlags)
  {
    super(log, output);
    if (rawFlags != null) {
      parseFlags(rawFlags);
    }
  }

  public Map<String, Boolean> getFlags() {
    return flags;
  }

  private void parseFlags(final Map<String, String> rawFlags) {
    for (Entry<String, String> entry : rawFlags.entrySet()) {
      boolean flag = Boolean.parseBoolean(entry.getValue());
      flags.put(entry.getKey(), flag);
    }
    log.debug("Omit flags: " + flags);
  }

  private boolean omit;

  @Override
  public void append(final File source) throws IOException {
    this.omit = false;

    super.append(source);

    // complain if still in omit mode at the end of the file
    if (omit) {
      log.warn("Missing omit end token: " + source);
    }
  }

  @Override
  protected void append(final String line) throws IOException {
    // check for omission start and stop tokens
    String trimmedLine = line.trim();
    if (isOmitStart(trimmedLine)) {
      // complain if already in omit mode and we found a start token (nesting is not allowed)
      if (omit) {
        log.warn("Unexpected omit start token: " + source + "#" + linenum);
      }
      omit = true;
    }
    else if (isOmitStop(trimmedLine)) {
      omit = false;
    }
    else if (omit) {
      output.write("//");
    }

    super.append(line);
  }

  private boolean isOmitStart(final String line) {
    // assumes line is trimmed
    if (line.startsWith(IF_START_PREFIX) && line.endsWith(IF_START_SUFFIX)) {
      String flagName = line.substring(IF_START_PREFIX.length(), line.length() - IF_START_SUFFIX.length());
      Boolean flag = flags.get(flagName);
      return flag != null && flag;
    }
    return false;
  }

  private boolean isOmitStop(final String line) {
    // assumes line is trimmed
    return line.equals(IF_END);
  }
}
