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
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
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
import org.mockito.Mock;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.io.DirectoryHelper.mkdir;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.REPLICATIONLOG;

public class ReplicationLogCustomizerTest
    extends TestSupport
{
  private static final long ONE_MINUTE = MINUTES.toMinutes(1);

  private static final long TIME_2400 = HOURS.toMinutes(24);

  private static final long TIME_2359 = TIME_2400 - ONE_MINUTE;

  private static final long TIME_2401 = TIME_2400 + ONE_MINUTE;

  @Rule
  public TemporaryFolder temporaryWorkDirectory = new TemporaryFolder();

  @Mock
  private ApplicationDirectories mockApplicationDirectories;

  private File replicationLogsHome;

  private ReplicationLogCustomizer underTest;

  @Before
  public void setup() throws IOException {
    setupTestDirectories();
    initializeSystemUnderTest();
  }

  @Test
  public void replicationLogInclusion() throws IOException {
    // valid log files
    createLogFile("/log/replication/replication-repo0.log", TIME_2359);
    createLogFile("/log/replication/replication-repo1.log", TIME_2359);

    SupportBundle supportBundle = new SupportBundle();
    underTest.customize(supportBundle);

    List<ContentSource> list = supportBundle.getSources();
    assertThat(list.size(), equalTo(2)); // only two valid files

    // includes valid files
    assertThat(list,
        hasItem(containsLogSource(FileContentSourceSupport.class, "log/replication/replication-repo0.log", DEFAULT,
            REPLICATIONLOG)));

    assertThat(list,
        hasItem(containsLogSource(FileContentSourceSupport.class, "log/replication/replication-repo1.log", DEFAULT,
            REPLICATIONLOG)));
  }

  @Test
  public void replicationLogExclusion() throws IOException {
    // modified out of expected range
    createLogFile("/log/replication/replication-repo2.log", TIME_2401);
    // gzipped file in range (should not be grabbed , not valid file extension)
    createLogFile("/log/replication/replication-repo-test-beta.gz", TIME_2359);

    // Add a sub-folder to implicitly assert that it will not be included in the zip
    createLogFile("/log/replication/extrasubfolder/replication-extra.log", 0);

    SupportBundle supportBundle = new SupportBundle();
    underTest.customize(supportBundle);

    List<ContentSource> list = supportBundle.getSources();
    assertThat(list.size(), equalTo(0)); // should be empty , not valid files found
  }

  private void setupTestDirectories() throws IOException {
    when(mockApplicationDirectories.getWorkDirectory()).thenReturn(temporaryWorkDirectory.getRoot());

    File logDir = mkdir(temporaryWorkDirectory.getRoot(), "log");
    replicationLogsHome = mkdir(logDir, "replication");
    mkdir(replicationLogsHome, "extrasubfolder");
  }

  private void initializeSystemUnderTest() {
    underTest = new ReplicationLogCustomizer()
    {
      @Override
      public Optional<String> getReplicationLogsHome() {
        return Optional.of(replicationLogsHome.getAbsolutePath());
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
