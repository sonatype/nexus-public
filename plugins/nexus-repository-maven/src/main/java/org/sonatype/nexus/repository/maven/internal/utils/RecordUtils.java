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
package org.sonatype.nexus.repository.maven.internal.utils;

import org.apache.maven.index.reader.Record;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.maven.index.reader.Record.CLASSIFIER;
import static org.apache.maven.index.reader.Record.FILE_EXTENSION;

/**
 * Provides utility methods for Maven records.
 *
 * @since 3.11
 */
public final class RecordUtils
{
  private RecordUtils() {
    //no-op
  }

  public static String gavceForRecord(final Record record) {
    String g = record.get(Record.GROUP_ID);
    String a = record.get(Record.ARTIFACT_ID);
    String v = record.get(Record.VERSION);
    String ce = defaultIfBlank(record.get(CLASSIFIER), "n/a") + ":" + record.get(FILE_EXTENSION);

    return g + a + v + ce;
  }
}
