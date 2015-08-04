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
package org.sonatype.timeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.sisu.litmus.testsupport.TestUtil;
import org.sonatype.timeline.internal.DefaultTimeline;

import org.eclipse.sisu.launch.InjectedTestCase;

public abstract class AbstractTimelineTestCase
    extends InjectedTestCase
{
  static {
    // This setting is only checked once per-testsuite and is needed for Nexus tests.
    // So in case this test runs first, make sure we have the right setting in place.
    System.setProperty("guice.disable.misplaced.annotation.check", "true");
  }

  private static final String loremIpsum =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur rutrum urna ac est sagittis in lacinia "
          + "tortor porta. Maecenas accumsan hendrerit nulla vel lobortis. Phasellus purus sapien, fermentum non "
          + "aliquet vitae, vulputate a nulla. Nunc a diam eget augue accumsan suscipit nec at tellus. Donec sit "
          + "amet tellus mi, vitae gravida justo. Nulla iaculis ullamcorper sodales. Pellentesque ut viverra tellus. "
          + "Pellentesque quis lacus velit. Nullam nec orci id ante pharetra dictum. In tincidunt fringilla metus, "
          + "id faucibus ligula condimentum id. Aenean augue odio, auctor fermentum sodales non, gravida sit amet "
          + "diam. Maecenas ac dolor at lorem ullamcorper pretium convallis ut felis. Mauris ut nisi ut leo "
          + "fringilla auctor sed et lectus.";

  protected TestUtil testUtil = new TestUtil(this);

  protected DefaultTimeline timeline;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    timeline = (DefaultTimeline) this.lookup(Timeline.class);
  }

  @Override
  public void tearDown()
      throws Exception
  {
    timeline.stop();
    super.tearDown();
  }

  protected void cleanDirectory(File directory)
      throws Exception
  {
    if (directory.exists()) {
      for (File file : directory.listFiles()) {
        file.delete();
      }
      directory.delete();
    }
  }

  protected TimelineRecord createTimelineRecord() {
    return createTimelineRecord(System.currentTimeMillis());
  }

  protected TimelineRecord createTimelineRecord(final long ts) {
    Map<String, String> data = new HashMap<String, String>();
    data.put("k1", "v1");
    data.put("k2", "v2");
    data.put("k3", "v3");
    data.put("k4", loremIpsum);
    return createTimelineRecord(ts, "type", "subType", data);
  }

  protected TimelineRecord createTimelineRecord(final long ts, final String type, final String subType,
                                                final Map<String, String> data)
  {
    return new TimelineRecord(ts, type, subType, data);
  }

  public static class AsList
      implements TimelineCallback
  {

    private final ArrayList<TimelineRecord> records = new ArrayList<TimelineRecord>();

    @Override
    public boolean processNext(TimelineRecord rec)
        throws IOException
    {
      records.add(rec);
      return true;
    }

    public List<TimelineRecord> getRecords() {
      return records;
    }
  }
}
