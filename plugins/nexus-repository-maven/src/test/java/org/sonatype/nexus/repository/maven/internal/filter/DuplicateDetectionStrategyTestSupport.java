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
package org.sonatype.nexus.repository.maven.internal.filter;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.index.reader.Record;

import static org.apache.maven.index.reader.Record.ARTIFACT_ID;
import static org.apache.maven.index.reader.Record.CLASSIFIER;
import static org.apache.maven.index.reader.Record.FILE_EXTENSION;
import static org.apache.maven.index.reader.Record.GROUP_ID;
import static org.apache.maven.index.reader.Record.Type.ARTIFACT_ADD;
import static org.apache.maven.index.reader.Record.VERSION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DuplicateDetectionStrategyTestSupport
    extends TestSupport
{
  public void verifyDuplicateDetection(final DuplicateDetectionStrategy<Record> strategy) throws Exception {
    int expectedUnique = 3;

    for (int i = 1; i <= expectedUnique; i++) {
      Record uniqueRecord = buildRecord("group1", "artifact" + i, "1.0", "sources", "jar");
      assertTrue(strategy.apply(uniqueRecord));
    }

    for (int i = 0; i < 10; i++) {
      Record duplicateRecord = buildRecord("group1", "artifact" + expectedUnique, "1.0", "sources", "jar");
      assertFalse(strategy.apply(duplicateRecord));
    }

    strategy.close();
  }

  private Record buildRecord(final String g,
                             final String a,
                             final String v,
                             final String c,
                             final String e)
  {
    return new Record(ARTIFACT_ADD, ImmutableMap.of(GROUP_ID, g,
        ARTIFACT_ID, a,
        VERSION, v,
        CLASSIFIER, c,
        FILE_EXTENSION, e));
  }
}
