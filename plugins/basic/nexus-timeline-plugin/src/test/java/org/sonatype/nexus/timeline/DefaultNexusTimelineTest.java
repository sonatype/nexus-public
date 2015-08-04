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
package org.sonatype.nexus.timeline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.NexusAppTestSupport;

import com.google.common.base.Predicate;
import org.junit.Test;

public class DefaultNexusTimelineTest
    extends NexusAppTestSupport
{
  protected NexusTimeline nexusTimeline;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    nexusTimeline = (NexusTimeline) this.lookup(NexusTimeline.class);
  }

  protected void tearDown()
      throws Exception
  {
    super.tearDown();
  }

  /**
   * Handy method that does what was done before: keeps all in memory, but this is usable for small amount of data,
   * like these in UT. This should NOT be used in production code, unless you want app that kills itself with OOM.
   */
  protected List<Entry> asList(int fromItem, int count, Set<String> types, Set<String> subTypes,
                               Predicate<Entry> filter)
  {
    final EntryListCallback result = new EntryListCallback();
    nexusTimeline.retrieve(fromItem, count, types, subTypes, filter, result);
    return result.getEntries();
  }

  @Test
  public void testSimpleTimestamp() {
    HashMap<String, String> data = new HashMap<String, String>();
    data.put("a", "a");
    data.put("b", "b");

    nexusTimeline.add(System.currentTimeMillis() - 1L * 60L * 60L * 1000L, "TEST", "1", data);

    nexusTimeline.add(System.currentTimeMillis() - 1L * 60L * 60L * 1000L, "TEST", "2", data);

    List<Entry> res =
        asList(1, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"1"})), null);

    assertEquals(0, res.size());

    res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"1"})), null);

    assertEquals(1, res.size());

    res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"2"})), null);

    assertEquals(1, res.size());

    res = asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(2, res.size());
  }

  @Test
  public void testSimpleItem() {
    HashMap<String, String> data = new HashMap<String, String>();
    data.put("a", "a");
    data.put("b", "b");

    nexusTimeline.add(System.currentTimeMillis() - 1L * 60L * 60L * 1000L, "TEST", "1", data);

    nexusTimeline.add(System.currentTimeMillis() - 1L * 60L * 60L * 1000L, "TEST", "2", data);

    List<Entry> res = asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(2, res.size());

    res = asList(1, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(1, res.size());
    assertEquals("b", res.get(0).getData().get("b"));

    res = asList(2, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(0, res.size());

    res = asList(0, 1, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(1, res.size());
    assertEquals("a", res.get(0).getData().get("a"));

    res = asList(0, 0, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(0, res.size());

    res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"1"})), null);

    assertEquals(1, res.size());

    res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"1"})), null);

    assertEquals(1, res.size());

    res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"2"})), null);

    assertEquals(1, res.size());

    res = asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})), null, null);

    assertEquals(2, res.size());
  }

  @Test
  public void testOrder() {
    HashMap<String, String> data = new HashMap<String, String>();
    data.put("place", "2nd");
    data.put("x", "y");

    nexusTimeline.add(System.currentTimeMillis() - 2L * 60L * 60L * 1000L, "TEST", "1", data);

    data.put("place", "1st");

    nexusTimeline.add(System.currentTimeMillis() - 1L * 60L * 60L * 1000L, "TEST", "1", data);

    List<Entry> res =
        asList(0, 10, new HashSet<String>(Arrays.asList(new String[]{"TEST"})),
            new HashSet<String>(Arrays.asList(new String[]{"1"})), null);

    assertEquals(2, res.size());

    assertEquals("1st", res.get(0).getData().get("place"));

    assertEquals("2nd", res.get(1).getData().get("place"));
  }
}
