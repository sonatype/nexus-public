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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.logging.task.TaskLogHome;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import com.google.common.annotations.VisibleForTesting;

import static java.time.Instant.ofEpochMilli;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.DEFAULT;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.REPLICATIONLOG;

/**
 * Class to add replication v2 logs to support bundle
 */
@Named
@Singleton
public class ReplicationLogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  @Override
  public void customize(final SupportBundle supportBundle) {
    Instant cutOff = ZonedDateTime.now().minusHours(24).toInstant();

    getReplicationLogsHome()
        .map(File::new)
        .map(f -> getChildren(f, Collections.singletonList("log"), false))
        .ifPresent(
            iterator -> iterator.forEachRemaining(file -> this.updateSupportBundle(file, supportBundle, cutOff)));
  }

  private Iterator<File> getChildren(final File folder, final List<String> extensions, final boolean recursive) {
    List<File> validFiles = new ArrayList<>();

    if (folder.exists()) {
      File[] children = folder.listFiles();

      for (File child : children) {
        if (child.isDirectory() && recursive) {
          Iterator<File> childrenResults = getChildren(child, extensions, recursive);
          childrenResults.forEachRemaining(validFiles::add);
        }
        else if (isValidFileExtension(child, extensions)) {
          validFiles.add(child);
        }
      }
    }

    return validFiles.iterator();
  }

  private boolean isValidFileExtension(final File child, final List<String> extensions) {
    String fileName = child.getName();
    int idx = fileName.lastIndexOf(".");

    if (idx > 0) {
      String extension = fileName.substring(idx + 1);
      return extensions.contains(extension);
    }

    return false;
  }

  private void updateSupportBundle(final File file, final SupportBundle supportBundle, final Instant cutOff) {
    if (ofEpochMilli(file.lastModified()).isAfter(cutOff)) {
      log.debug("adding replication log file '{}'", file);
      supportBundle.add(
          new FileContentSourceSupport(REPLICATIONLOG, String.format("log/replication/%s", file.getName()), file,
              DEFAULT));
    }
    else {
      log.debug("Skipping replication log file [past 24 hours]: {}", file);
    }
  }

  @VisibleForTesting
  Optional<String> getReplicationLogsHome() {
    return TaskLogHome.getReplicationLogsHome();
  }
}
