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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.sonatype.sisu.litmus.testsupport.group.Slow;
import org.sonatype.timeline.Timeline;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineRecord;

import org.junit.experimental.categories.Category;

@Category(Slow.class)
public class MultithreadTimelineIT
    extends AbstractInternalTimelineTestCase
{
  protected File persistorDirectory;

  protected File indexDirectory;

  protected static final Random rnd = new Random();

  // number of deploy threads (and search threads) to launch
  protected static final int THREAD_COUNT = 10;

  // max time the deploy threads will sleep between adding to timeline
  protected static final long MAX_DEPLOY_SLEEP_TIME = 100;

  // max time the search threads will sleep between searches
  protected static final long MAX_SEARCH_SLEEP_TIME = 500;

  // time to sleep between starting threads, and stopping them
  protected static final long SLEEP_TIME = THREAD_COUNT * MAX_DEPLOY_SLEEP_TIME * 10;

  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();
    persistorDirectory = new File(getBasedir(), "target/persistor");
    indexDirectory = new File(getBasedir(), "target/index");
    cleanDirectory(persistorDirectory);
    cleanDirectory(indexDirectory);
    timeline.start(new TimelineConfiguration(persistorDirectory, indexDirectory));
  }

  public void testKindaNexus()
      throws Exception
  {
    // nexus has deploys and searches happening same time
    // so start 20 deployer (content change in nexus) threads and 10 searcher (RSS fetching) threads

    List<DeployerThread> deployerThreads = new ArrayList<DeployerThread>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      deployerThreads.add(new DeployerThread(timeline, new TimelineRecord(System.currentTimeMillis(),
          "DT" + i, "1", new HashMap<String, String>())));
    }

    List<SearcherThread> searcherThreads = new ArrayList<SearcherThread>();

    for (int i = 0; i < THREAD_COUNT; i++) {
      searcherThreads.add(new SearcherThread(timeline, "DT" + (i)));
    }

    for (DeployerThread thread : deployerThreads) {
      thread.start();
    }

    for (SearcherThread thread : searcherThreads) {
      thread.start();
    }

    // let them do some work
    Thread.sleep(SLEEP_TIME);

    // kill timeline
    // this tests NEXUS-4459: threads will still try to add() or retrieve()
    // from timeline, even after it is shut down
    timeline.stop();

    // let them try do some more
    Thread.sleep(SLEEP_TIME);

    // kill'em
    for (DeployerThread thread : deployerThreads) {
      thread.stopAndJoin();
    }

    // let the searchers run for more
    Thread.sleep(MAX_SEARCH_SLEEP_TIME);

    // stop them nicely (to pick up last dt thread changes)
    for (SearcherThread thread : searcherThreads) {
      thread.stopAndJoin();
    }

    for (DeployerThread thread : deployerThreads) {
      assertEquals(thread.getTimelineRecord().getType() + " deployer is not fine!", null,
          unravelThrowable(thread.getLastException()));
    }

    for (SearcherThread thread : searcherThreads) {
      assertEquals(thread.getTypeToSearchFor() + " searcher is not fine!", null,
          unravelThrowable(thread.getLastException()));
    }

    for (int i = 0; i < THREAD_COUNT; i++) {
      assertTrue("Added should be strictly more to found ones (we shot down timeline in the middle!)",
          deployerThreads.get(i).getAdded() > searcherThreads.get(i).getLastCount());
    }
  }

  protected String unravelThrowable(Throwable e) {
    if (e == null) {
      return null;
    }
    else {
      final Writer result = new StringWriter();

      final PrintWriter printWriter = new PrintWriter(result);

      e.printStackTrace(printWriter);

      return result.toString();
    }
  }

  private static class DeployerThread
      extends Thread
  {
    private final Timeline timeline;

    private final TimelineRecord timelineRecord;

    private int added;

    private Throwable ex;

    private boolean running;

    public DeployerThread(Timeline timeline, TimelineRecord timelineRecord) {
      this.timeline = timeline;

      this.timelineRecord = timelineRecord;

      this.added = 0;

      this.ex = null;

      this.running = true;
    }

    public void stopAndJoin()
        throws InterruptedException
    {
      this.running = false;

      join();
    }

    public void run() {
      try {
        while (running) {
          timeline.add(new TimelineRecord(System.currentTimeMillis(), timelineRecord.getType(),
              timelineRecord.getSubType(), timelineRecord.getData()));
          added++;
          sleep(Math.abs(rnd.nextLong()) % MAX_DEPLOY_SLEEP_TIME);
        }
      }
      catch (InterruptedException e) {
        // we don't care, probably we've been interrupted
      }
      catch (Exception e) {
        ex = e;
      }
    }

    public int getAdded() {
      return added;
    }

    public Throwable getLastException() {
      return ex;
    }

    public TimelineRecord getTimelineRecord() {
      return timelineRecord;
    }
  }

  private static class SearcherThread
      extends Thread
  {
    private final DefaultTimeline timeline;

    private final String typeToSearchFor;

    private boolean running;

    private int lastCount = 0;

    private Throwable ex;

    public SearcherThread(DefaultTimeline timeline, String typeToSearchFor) {
      this.timeline = timeline;

      this.typeToSearchFor = typeToSearchFor;

      this.running = true;
    }

    public void stopAndJoin()
        throws InterruptedException
    {
      this.running = false;

      join();
    }

    public void run() {
      try {
        // this one cycle is "inversed": first sleeps then does the work
        // this is needed to have proper "ending", and pick up all the latest changes when stopping
        do {
          sleep(Math.abs(rnd.nextLong()) % MAX_SEARCH_SLEEP_TIME);

          AsList cb = new AsList();
          timeline.retrieve(0, Integer.MAX_VALUE, Collections.singleton(typeToSearchFor), null, null, cb);
          List<TimelineRecord> records = cb.getRecords();

          if (records.size() == 0 && lastCount == 0) {
            // still working, give it some time to catch up
          }
          else if (lastCount <= records.size()) {
            // all fine, we should get only equal or more than lastCount (other thread "deploys")
            lastCount = records.size();
          }
          else if (timeline.isStarted()) {
            // this protects against NEXUS-4459: thread will still try to retrieve() and will get empty results
            throw new IllegalStateException("We got error: lastCount=" + lastCount
                + " but we got records.size=" + records.size());
          }
        }
        while (running);
      }
      catch (InterruptedException e) {
        // we don't care, probably we've been interrupted
      }
      catch (Exception e) {
        ex = e;
      }
    }

    public int getLastCount() {
      return lastCount;
    }

    public Throwable getLastException() {
      return ex;
    }

    public String getTypeToSearchFor() {
      return typeToSearchFor;
    }
  }
}
