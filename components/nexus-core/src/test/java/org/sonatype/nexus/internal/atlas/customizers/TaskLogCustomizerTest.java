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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.supportzip.ContentSourceSupport;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.common.io.DirectoryHelper.mkdir;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.TASKLOG;

public class TaskLogCustomizerTest
    extends TestSupport
{
  private static final long ONE_HOUR = HOURS.toMinutes(1);

  private static final long ONE_MINUTE = MINUTES.toMinutes(1);

  private static final long TIME_2300 = HOURS.toMinutes(23);

  private static final long TIME_2400 = HOURS.toMinutes(24);

  private static final long TIME_2500 = HOURS.toMinutes(25);

  private static final long TIME_2359 = TIME_2400 - ONE_MINUTE;

  private static final long TIME_2401 = TIME_2400 + ONE_MINUTE;

  @Rule
  public TemporaryFolder temporaryWorkDirectory = new TemporaryFolder();

  private File tasksHome;

  private TaskLogCustomizer underTest;

  @Before
  public void setup() throws IOException {
    setupTestDirectories();
    initializeSystemUnderTest();
  }

  @Test
  public void taskLogInclusion() throws IOException {
    // create log files up to 25 hours in age
    for (int i = 0; i < TIME_2500; i += ONE_HOUR) {
      createLogFile(format("/log/tasks/task%s.log", i), i);
    }
    // additionally create log files at 23:59(included) and 24:01(excluded)
    createLogFile(format("/log/tasks/task%s.log", TIME_2359), TIME_2359);
    createLogFile(format("/log/tasks/task%s.log", TIME_2401), TIME_2401);

    // Add a sub-folder to implicitly assert that it will not be included in the task zip
    createLogFile("/log/tasks/extrasubfolder/extra.log", 0);

    SupportBundle supportBundle = new SupportBundle();
    underTest.customize(supportBundle);

    List<ContentSource> list = supportBundle.getSources();
    assertThat(list.size(), equalTo(25)); // 25 = 24 hourly task logs + 23:59 task log

    // hourly up to 23:00
    for (long i = 0; i <= TIME_2300; i += ONE_HOUR) {
      assertThat(list,
          hasItem(
              containsLogSource(FileContentSourceSupport.class, format("log/tasks/task%s.log", i), DEFAULT, TASKLOG)));
    }
    // includes 23:59
    assertThat(list,
        hasItem(containsLogSource(FileContentSourceSupport.class, format("log/tasks/task%s.log", TIME_2359), DEFAULT,
            TASKLOG)));
    // excludes 24:00 and up hourly
    for (long i = TIME_2400; i < TIME_2500; i += ONE_HOUR) {
      assertThat(list, not(hasItem(
          containsLogSource(FileContentSourceSupport.class, format("log/tasks/task%s.log", i), DEFAULT, TASKLOG))));
    }
    // excludes 24:01
    assertThat(list, not(hasItem(
        containsLogSource(FileContentSourceSupport.class, format("log/tasks/task%s.log", TIME_2401), DEFAULT,
            TASKLOG))));
    // excludes sub-folder
    assertThat(list, not(hasItem(
        containsLogSource(FileContentSourceSupport.class, "log/tasks/extrasubfolder/extra.log", DEFAULT, TASKLOG))));
  }

  private void setupTestDirectories() throws IOException {
    File logDir = mkdir(temporaryWorkDirectory.getRoot(), "log");
    tasksHome = mkdir(logDir, "tasks");
    mkdir(tasksHome, "extrasubfolder");
  }

  private void initializeSystemUnderTest() {
    underTest = new TaskLogCustomizer()
    {
      @Override
      public String getTaskLogHome() {
        return tasksHome.getAbsolutePath();
      }
    };
  }

  private void createLogFile(final String name, final long minutesOld) throws IOException {
    File file = new File(temporaryWorkDirectory.getRoot(), name);
    file.createNewFile();
    file.setLastModified(ZonedDateTime.now().minusMinutes(minutesOld).toInstant().toEpochMilli());
  }

  private Matcher<Iterable<ContentSource>> containsLogSource(
      final Class<? extends ContentSource> clazz,
      final String path,
      final Priority priority,
      final Type type)
  {
    return new BaseMatcher<Iterable<ContentSource>>()
    {
      @Override
      public boolean matches(final Object item) {
        if (!clazz.isAssignableFrom(item.getClass())) {
          return false;
        }
        ContentSourceSupport contentSourceSupport = (ContentSourceSupport) item;
        return contentSourceSupport.getPath().equals(path) &&
            contentSourceSupport.getPriority().equals(priority) &&
            contentSourceSupport.getType().equals(type);
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("ContentSource[")
            .appendValue(path)
            .appendText(", ")
            .appendValue(priority)
            .appendText(", ")
            .appendValue(type)
            .appendText("]");
      }
    };
  }
}
