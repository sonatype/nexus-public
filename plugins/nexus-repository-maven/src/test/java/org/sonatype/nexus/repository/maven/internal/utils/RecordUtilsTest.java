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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.index.reader.Record;
import org.junit.Test;

import static org.apache.maven.index.reader.Record.ARTIFACT_ID;
import static org.apache.maven.index.reader.Record.FILE_EXTENSION;
import static org.apache.maven.index.reader.Record.GROUP_ID;
import static org.apache.maven.index.reader.Record.Type.ARTIFACT_ADD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.nexus.repository.maven.internal.utils.RecordUtils.gavceForRecord;

public class RecordUtilsTest
    extends TestSupport
{
  public static final String GROUP = "group";

  public static final String ARTIFACT = "artifact";

  public static final String VERSION = "version";

  public static final String CLASSIFIER = "classifier";

  public static final String EXTENSION = "extension";

  @Test
  public void shouldConvertRecordToGavce() throws Exception {
    Record record = new Record(ARTIFACT_ADD, ImmutableMap.of(GROUP_ID, GROUP,
        ARTIFACT_ID, ARTIFACT,
        Record.VERSION, VERSION,
        Record.CLASSIFIER, CLASSIFIER,
        FILE_EXTENSION, EXTENSION));

    assertThat(gavceForRecord(record), is(equalTo(GROUP + ARTIFACT + VERSION + CLASSIFIER + ":" + EXTENSION)));
  }

  @Test
  public void shouldHandleBlankClassifierWhenConvertRecordToGavce() throws Exception {
    String c = "";

    Record record = new Record(ARTIFACT_ADD, ImmutableMap.of(GROUP_ID, GROUP,
        ARTIFACT_ID, ARTIFACT,
        Record.VERSION, VERSION,
        Record.CLASSIFIER, c,
        FILE_EXTENSION, EXTENSION));

    assertThat(gavceForRecord(record), is(equalTo(GROUP + ARTIFACT + VERSION + "n/a" + ":" + EXTENSION)));
  }
}
