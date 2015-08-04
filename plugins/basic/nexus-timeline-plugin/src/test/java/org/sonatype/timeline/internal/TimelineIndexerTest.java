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
package org.sonatype.timeline.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineFilter;
import org.sonatype.timeline.TimelineRecord;

import org.junit.Ignore;

/**
 * Test the timeline indexer
 *
 * @author juven
 */
public class TimelineIndexerTest
    extends AbstractInternalTimelineTestCase
{
  protected File indexDirectory;

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();
    indexDirectory = new File(getBasedir(), "target/index");
    cleanDirectory(indexDirectory);
    indexer.start(new TimelineConfiguration(null, indexDirectory));
  }

  @Override
  public void tearDown()
      throws Exception
  {
    indexer.stop();
    super.tearDown();
  }

  /**
   * Helper method to lessen typing.
   */
  protected List<TimelineRecord> asList(final long fromTime, final long toTime, final Set<String> types,
                                        final Set<String> subTypes, int from, int count, final TimelineFilter filter)
      throws IOException
  {
    final AsList cb1 = new AsList();
    indexer.retrieve(fromTime, toTime, types, subTypes, from, count, filter, cb1);
    return cb1.getRecords();
  }

  /**
   * Helper method to lessen typing.
   */
  protected int sizeOf(List<TimelineRecord> lst) {
    return lst.size();
  }

  public void testIndexOneRecord()
      throws Exception
  {
    TimelineRecord record = createTimelineRecord();
    indexer.add(record);

    Set<String> types = new HashSet<String>();
    types.add(record.getType());
    List<TimelineRecord> results = asList(0, System.currentTimeMillis(), types, null, 0, 100, null);

    assertEquals(1, results.size());
    assertEquals(record.getData(), results.get(0).getData());
  }

  public void testIndexMutipleRecords()
      throws Exception
  {
    for (int i = 0; i < 10; i++) {
      indexer.add(createTimelineRecord());
    }

    List<TimelineRecord> results = asList(0, System.currentTimeMillis(), null, null, 0, 100, null);
    assertEquals(10, results.size());
  }

  public void testSearchByTime()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(1000000L);
    TimelineRecord rec2 = createTimelineRecord(2000000L);
    TimelineRecord rec3 = createTimelineRecord(3000000L);
    TimelineRecord rec4 = createTimelineRecord(4000000L);
    indexer.add(rec1);
    indexer.add(rec2);
    indexer.add(rec3);
    indexer.add(rec4);

    assertEquals(4, asList(0, System.currentTimeMillis(), null, null, 0, 100, null).size());
    assertEquals(3, asList(0, 3500000L, null, null, 0, 100, null).size());
    assertEquals(2, asList(1500000L, 3500000L, null, null, 0, 100, null).size());
    assertEquals(0, asList(4500000L, System.currentTimeMillis(), null, null, 0, 100, null).size());
  }

  public void testSearchByType()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(System.currentTimeMillis(), "typeA", "foo", null);
    TimelineRecord rec2 = createTimelineRecord(System.currentTimeMillis(), "typeB", "foo", null);
    TimelineRecord rec3 = createTimelineRecord(System.currentTimeMillis(), "typeB", "foo", null);
    TimelineRecord rec4 = createTimelineRecord(System.currentTimeMillis(), "typeC", "foo", null);
    indexer.add(rec1);
    indexer.add(rec2);
    indexer.add(rec3);
    indexer.add(rec4);

    Set<String> types = new HashSet<String>();

    types.add("typeA");
    assertEquals(1, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
    types.clear();
    types.add("typeB");
    assertEquals(2, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
    types.clear();
    types.add("typeA");
    types.add("typeB");
    assertEquals(3, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
    types.clear();
    types.add("typeA");
    types.add("typeB");
    types.add("typeC");
    assertEquals(4, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
    types.clear();
    assertEquals(4, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
    types.clear();
    types.add("typeX");
    assertEquals(0, asList(0, System.currentTimeMillis(), types, null, 0, 100, null).size());
  }

  public void testSearchBySubType()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(System.currentTimeMillis(), "foo", "subA", null);
    TimelineRecord rec2 = createTimelineRecord(System.currentTimeMillis(), "foo", "subB", null);
    TimelineRecord rec3 = createTimelineRecord(System.currentTimeMillis(), "foo", "subB", null);
    TimelineRecord rec4 = createTimelineRecord(System.currentTimeMillis(), "foo", "subC", null);
    indexer.add(rec1);
    indexer.add(rec2);
    indexer.add(rec3);
    indexer.add(rec4);

    Set<String> subTypes = new HashSet<String>();

    subTypes.add("subA");
    assertEquals(1, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
    subTypes.clear();
    subTypes.add("subB");
    assertEquals(2, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
    subTypes.clear();
    subTypes.add("subA");
    subTypes.add("subB");
    assertEquals(3, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
    subTypes.clear();
    subTypes.add("subA");
    subTypes.add("subB");
    subTypes.add("subC");
    assertEquals(4, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
    subTypes.clear();
    assertEquals(4, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
    subTypes.clear();
    subTypes.add("subX");
    assertEquals(0, asList(0, System.currentTimeMillis(), null, subTypes, 0, 100, null).size());
  }

  public void testSearchByTypeAndSubType()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(System.currentTimeMillis(), "typeA", "subX", null);
    TimelineRecord rec2 = createTimelineRecord(System.currentTimeMillis(), "typeB", "subX", null);
    TimelineRecord rec3 = createTimelineRecord(System.currentTimeMillis(), "typeB", "subY", null);
    TimelineRecord rec4 = createTimelineRecord(System.currentTimeMillis(), "typeA", "subX", null);
    indexer.add(rec1);
    indexer.add(rec2);
    indexer.add(rec3);
    indexer.add(rec4);

    Set<String> types = new HashSet<String>();
    Set<String> subTypes = new HashSet<String>();

    types.add("typeA");
    subTypes.add("subX");
    assertEquals(2, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));

    types.clear();
    subTypes.clear();
    types.add("typeB");
    subTypes.add("subX");
    assertEquals(1, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));

    types.clear();
    subTypes.clear();
    types.add("typeA");
    types.add("typeB");
    subTypes.add("subX");
    assertEquals(3, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));

    types.clear();
    subTypes.clear();
    types.add("typeA");
    subTypes.add("subY");
    assertEquals(0, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));

    types.clear();
    subTypes.clear();
    types.add("typeB");
    subTypes.add("subY");
    assertEquals(1, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));

    types.clear();
    subTypes.clear();
    types.add("typeA");
    types.add("typeB");
    subTypes.add("subX");
    subTypes.add("subY");
    assertEquals(4, sizeOf(asList(0, System.currentTimeMillis(), types, subTypes, 0, 100, null)));
  }

  public void testSearchByCount()
      throws Exception
  {
    for (int i = 0; i < 50; i++) {
      indexer.add(createTimelineRecord());
    }

    int count = 50;
    assertEquals(count, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, count, null)));
    count = 49;
    assertEquals(count, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, count, null)));
    count = 25;
    assertEquals(count, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, count, null)));
    count = 0;
    assertEquals(count, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, count, null)));
    count = 1;
    assertEquals(count, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, count, null)));
  }

  public void testSearchByFrom()
      throws Exception
  {
    for (int i = 0; i < 50; i++) {
      indexer.add(createTimelineRecord());
    }

    int from = 49;
    assertEquals(50 - from, sizeOf(asList(0, System.currentTimeMillis(), null, null, from, 1000, null)));
    from = 1;
    assertEquals(50 - from, sizeOf(asList(0, System.currentTimeMillis(), null, null, from, 1000, null)));
    from = 25;
    assertEquals(50 - from, sizeOf(asList(0, System.currentTimeMillis(), null, null, from, 1000, null)));
    from = 0;
    assertEquals(50 - from, sizeOf(asList(0, System.currentTimeMillis(), null, null, from, 1000, null)));
    from = 50;
    assertEquals(50 - from, sizeOf(asList(0, System.currentTimeMillis(), null, null, from, 1000, null)));
  }

  public void testSearchResultOrderByTime()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(1000000L);
    rec1.getData().put("t", "1");
    TimelineRecord rec2 = createTimelineRecord(2000000L);
    rec2.getData().put("t", "2");
    TimelineRecord rec3 = createTimelineRecord(3000000L);
    rec3.getData().put("t", "3");
    TimelineRecord rec4 = createTimelineRecord(4000000L);
    rec4.getData().put("t", "4");

    indexer.add(rec2);
    indexer.add(rec1);
    indexer.add(rec4);
    indexer.add(rec3);

    List<TimelineRecord> results = asList(0, System.currentTimeMillis(), null, null, 0, 100, null);

    assertEquals(4, results.size());
    assertEquals("4", results.get(0).getData().get("t"));
    assertEquals("3", results.get(1).getData().get("t"));
    assertEquals("2", results.get(2).getData().get("t"));
    assertEquals("1", results.get(3).getData().get("t"));
  }

  public void testPurgeByTime()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord(1000000L);
    rec1.getData().put("t", "1");
    TimelineRecord rec2 = createTimelineRecord(2000000L);
    rec2.getData().put("t", "2");
    TimelineRecord rec3 = createTimelineRecord(3000000L);
    rec3.getData().put("t", "3");
    TimelineRecord rec4 = createTimelineRecord(4000000L);
    rec4.getData().put("t", "4");

    indexer.add(rec2);
    indexer.add(rec1);
    indexer.add(rec4);
    indexer.add(rec3);

    assertEquals(2, indexer.purge(1500000L, 3500000L, null, null));
    List<TimelineRecord> results = asList(0, System.currentTimeMillis(), null, null, 0, 100, null);
    assertEquals(2, results.size());
    assertEquals("4", results.get(0).getData().get("t"));
    assertEquals("1", results.get(1).getData().get("t"));
    assertEquals(2, indexer.purge(0, 4500000L, null, null));
    assertEquals(0, sizeOf(asList(0, System.currentTimeMillis(), null, null, 0, 100, null)));
  }

  public void testSearchWithPagination()
      throws Exception
  {
    String key = "count";

    for (int i = 0; i < 30; i++) {
      TimelineRecord rec = createTimelineRecord(10000000L - i * 60000L);
      rec.getData().put(key, "" + i);

      indexer.add(rec);
    }
    List<TimelineRecord> results;
    results = asList(0, System.currentTimeMillis(), null, null, 0, 10, null);
    assertEquals(10, results.size());
    assertEquals("0", results.get(0).getData().get(key));
    assertEquals("9", results.get(9).getData().get(key));

    results = asList(0, System.currentTimeMillis(), null, null, 10, 10, null);
    assertEquals(10, results.size());
    assertEquals("10", results.get(0).getData().get(key));
    assertEquals("19", results.get(9).getData().get(key));

    results = asList(0, System.currentTimeMillis(), null, null, 20, 10, null);
    assertEquals(10, results.size());
    assertEquals("20", results.get(0).getData().get(key));
    assertEquals("29", results.get(9).getData().get(key));

    results = asList(0, System.currentTimeMillis(), null, null, 30, 10, null);
    assertEquals(0, results.size());
  }

  public void testSearchWithFilter()
      throws Exception
  {
    TimelineRecord rec1 = createTimelineRecord();
    rec1.getData().put("key", "a1");
    TimelineRecord rec2 = createTimelineRecord();
    rec2.getData().put("key", "a2");
    TimelineRecord rec3 = createTimelineRecord();
    rec3.getData().put("key", "a3");
    TimelineRecord rec4 = createTimelineRecord();
    rec4.getData().put("key", "b1");
    TimelineRecord rec5 = createTimelineRecord();
    rec5.getData().put("key1", "b2");

    indexer.add(rec1);
    indexer.add(rec2);
    indexer.add(rec3);
    indexer.add(rec4);
    indexer.add(rec5);

    List<TimelineRecord> results;

    results = asList(0, System.currentTimeMillis(), null, null, 0, 100, new TimelineFilter()
    {
      public boolean accept(TimelineRecord hit) {
        if (hit.getData().containsKey("key")) {
          return true;
        }
        return false;
      }
    });
    assertEquals(4, results.size());

    results = asList(0, System.currentTimeMillis(), null, null, 0, 100, new TimelineFilter()
    {
      public boolean accept(TimelineRecord hit) {
        if (hit.getData().containsKey("key") && hit.getData().get("key").startsWith("a")) {
          return true;
        }
        return false;
      }
    });
    assertEquals(3, results.size());

    results = asList(0, System.currentTimeMillis(), null, null, 0, 100, new TimelineFilter()
    {
      public boolean accept(TimelineRecord hit) {
        if (hit.getData().containsKey("key") && hit.getData().get("key").startsWith("b")) {
          return true;
        }
        return false;
      }
    });
    assertEquals(1, results.size());
  }

  public void testSearchWithFilterAndPagination()
      throws Exception
  {
    String key = "count";

    for (int i = 0; i < 30; i++) {
      TimelineRecord rec = createTimelineRecord(10000000L - i * 60000L);
      rec.getData().put(key, "" + i);

      indexer.add(rec);
    }

    TimelineFilter filter = new TimelineFilter()
    {
      public boolean accept(TimelineRecord hit) {
        if (hit.getData().containsKey("count")) {
          int v = Integer.parseInt(hit.getData().get("count"));

          if (v % 2 == 0) {
            return true;
          }

          return false;
        }

        return false;
      }

    };

    List<TimelineRecord> results;
    results = asList(0, System.currentTimeMillis(), null, null, 0, 10, filter);
    assertEquals(10, results.size());
    assertEquals("0", results.get(0).getData().get(key));
    assertEquals("18", results.get(9).getData().get(key));

    results = asList(0, System.currentTimeMillis(), null, null, 10, 10, filter);
    assertEquals(5, results.size());
    assertEquals("20", results.get(0).getData().get(key));
    assertEquals("28", results.get(4).getData().get(key));
  }
}
