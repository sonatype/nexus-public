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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.timeline.Timeline;
import org.sonatype.timeline.TimelineCallback;
import org.sonatype.timeline.TimelineConfiguration;
import org.sonatype.timeline.TimelineRecord;

import com.google.common.base.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is the "real thing": implementation backed by spice Timeline. Until now, it was in Core, but it kept many
 * important and key dependencies in core too, and making Nexus Core literally a hostage of it.
 *
 * @author cstamas
 * @since 2.0
 */
@Named
@Singleton
public class DefaultNexusTimeline
    extends ComponentSupport
    implements NexusTimeline
{

  private static final String TIMELINE_BASEDIR = "timeline";

  private final Timeline timeline;

  private final ApplicationConfiguration applicationConfiguration;

  @Inject
  public DefaultNexusTimeline(final Timeline timeline,
                              final ApplicationConfiguration applicationConfiguration)
  {
    this.timeline = checkNotNull(timeline);
    this.applicationConfiguration = checkNotNull(applicationConfiguration);

    try {
      log.info("Initializing Nexus Timeline...");

      moveLegacyTimeline();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to move legacy Timeline!", e);
    }
    try {
      log.info("Starting Nexus Timeline...");
      updateConfiguration();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to initialize Timeline!", e);
    }
  }

  @Override
  public void shutdown() {
    try {
      log.info("Stopping Nexus Timeline...");
      timeline.stop();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to cleanly stop Timeline!", e);
    }
  }

  private void moveLegacyTimeline()
      throws IOException
  {
    File timelineDir = applicationConfiguration.getWorkingDirectory(TIMELINE_BASEDIR);

    File legacyIndexDir = timelineDir;

    File newIndexDir = new File(timelineDir, "index");

    File[] legacyIndexFiles = legacyIndexDir.listFiles(new FileFilter()
    {
      public boolean accept(File file) {
        return file.isFile();
      }
    });

    if (legacyIndexFiles == null || legacyIndexFiles.length == 0) {
      return;
    }

    if (newIndexDir.exists() && newIndexDir.listFiles().length > 0) {
      return;
    }

    log.info(
        "Moving legacy timeline index from '" + legacyIndexDir.getAbsolutePath() + "' to '"
            + newIndexDir.getAbsolutePath() + "'.");

    DirSupport.mkdir(newIndexDir.toPath());
    for (File legacyIndexFile : legacyIndexFiles) {
      // legacy was just plain Lucene index (so, we move lucene files from here into a SUBDIRECTORY)
      if (Files.isRegularFile(legacyIndexFile.toPath())) {
        Files.move(legacyIndexFile.toPath(), new File(newIndexDir, legacyIndexFile.getName()).toPath());
      }
    }
  }

  private void updateConfiguration()
      throws IOException
  {
    final TimelineConfiguration config =
        new TimelineConfiguration(applicationConfiguration.getWorkingDirectory(TIMELINE_BASEDIR));
    timeline.start(config);
  }

  @Override
  public void add(long timestamp, String type, String subType, Map<String, String> data) {
    timeline.add(new TimelineRecord(timestamp, type, subType, data));
  }

  @Override
  public void retrieve(int fromItem, int count, Set<String> types, Set<String> subtypes, Predicate<Entry> filter,
                       TimelineCallback cb)
  {
    if (filter != null) {
      timeline.retrieve(fromItem, count, types, subtypes, new PredicateTimelineFilter(filter), cb);
    }
    else {
      timeline.retrieve(fromItem, count, types, subtypes, null, cb);
    }
  }

  @Override
  public void purgeOlderThan(int days) {
    timeline.purgeOlderThan(days);
  }
}
