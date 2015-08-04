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
package org.sonatype.nexus.testsuite.timeline.nexus5348;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.testsuite.timeline.TimelineITSupport;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * IT for NEXUS-5348: Purge timeline task should remove persisted files too (not only Lucene index should be shrinked).
 *
 * @author cstamas
 * @since 2.6.1
 */
public class PurgePersistFilesIT
    extends TimelineITSupport
{
  /**
   * Copied format from org.sonatype.timeline.internal.DefaultTimelinePersistor as plugin internals are not on
   * classpath.
   */
  private static final String V3_DATA_FILE_NAME_PREFIX = "timeline.";

  /**
   * Copied format from org.sonatype.timeline.internal.DefaultTimelinePersistor as plugin internals are not on
   * classpath.
   */
  private static final String V3_DATA_FILE_NAME_SUFFIX = "-v3.dat";

  /**
   * Copied format from org.sonatype.timeline.internal.DefaultTimelinePersistor as plugin internals are not on
   * classpath.
   */
  private static final String V3_DATA_FILE_NAME_DATE_FORMAT = "yyyy-MM-dd.HH-mm-ssZ";

  public PurgePersistFilesIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  protected File generatedPersistFilename(final File persistDirectory, final long timestamp) {
    final SimpleDateFormat dateFormat = new SimpleDateFormat(V3_DATA_FILE_NAME_DATE_FORMAT);
    final StringBuilder fileName = new StringBuilder();
    fileName.append(V3_DATA_FILE_NAME_PREFIX).append(dateFormat.format(new Date(timestamp))).append(
        V3_DATA_FILE_NAME_SUFFIX);
    return new File(persistDirectory, fileName.toString());
  }

  @Test
  public void sneakyTest()
      throws Exception
  {
    // nexus is started here
    // at start, at least one file will be created
    final File persistDirectory = new File(nexus().getWorkDirectory(), "timeline/persist");
    final long now = System.currentTimeMillis();
    Files.write("foobar", generatedPersistFilename(persistDirectory, now - TimeUnit.DAYS.toMillis(1)),
        Charset.defaultCharset());
    Files.write("foobar", generatedPersistFilename(persistDirectory, now - TimeUnit.DAYS.toMillis(2)),
        Charset.defaultCharset());
    Files.write("foobar", generatedPersistFilename(persistDirectory, now - TimeUnit.DAYS.toMillis(3)),
        Charset.defaultCharset());
    Files.write("foobar", generatedPersistFilename(persistDirectory, now - TimeUnit.DAYS.toMillis(4)),
        Charset.defaultCharset());
    Files.write("foobar", generatedPersistFilename(persistDirectory, now - TimeUnit.DAYS.toMillis(5)),
        Charset.defaultCharset());

    {
      // verify starting conditions: we have 6 files
      final List<File> leftFiles = Arrays.asList(persistDirectory.listFiles());
      assertThat(leftFiles, hasSize(6));
    }

    final Map<String, String> properties = Maps.newHashMap();
    properties.put("purgeOlderThan", String.valueOf(2));
    scheduler().run("PurgeTimeline", properties);
    Thread.sleep(200); // give some time to server to at least perform schedule
    scheduler().waitForAllTasksToStop(); // wait for it

    {
      // verify ending conditions: we have 3 files: todays, today-1 and today-2 (as older than 2) used.
      final List<File> leftFiles = Arrays.asList(persistDirectory.listFiles());
      assertThat(leftFiles, hasSize(3));
    }
  }
}
