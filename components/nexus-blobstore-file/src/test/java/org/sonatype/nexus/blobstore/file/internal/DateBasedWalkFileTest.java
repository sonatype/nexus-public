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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.time.UTC;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.CONTENT_PREFIX;

/**
 * Unit Tests for {@link DateBasedWalkFile}.
 */
public class DateBasedWalkFileTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void testWalkFilesWithDifferentDuration() throws Exception {
    File contentDir = new File(temporaryFolder.getRoot().getPath(), CONTENT_PREFIX);

    OffsetDateTime blobCreated = UTC.now();
    String bytesFileNow = "now";
    String bytesFile1minOld = "1minOld";
    String bytesFile3minOld = "3minOld";
    String bytesFile10minOld = "10minOld";

    String bytesFile1hOld = "1hOld";
    String bytesFile3hOld = "3hOld";
    String bytesFile10hOld = "10hOld";

    String bytesFile1dOld = "1dOld";
    String bytesFile3dOld = "3dOld";
    String bytesFile10dOld = "10dOld";

    String bytesFile1mOld = "1mOld";
    String bytesFile3mOld = "3mOld";
    String bytesFile10mOld = "10mOld";

    // create 4 files: now, 1 min ago, 3 min ago, and 10 min ago
    File storageDirNow = new File(contentDir, getDatePath(blobCreated));
    assertThat(storageDirNow.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDirNow, bytesFileNow + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir1MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(1)));
    assertThat(storageDir1MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1MinOld, bytesFile1minOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(3)));
    assertThat(storageDir3MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3MinOld, bytesFile3minOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10MinOld = new File(contentDir, getDatePath(blobCreated.minusMinutes(10)));
    assertThat(storageDir10MinOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10MinOld, bytesFile10minOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 hour ago, 3 hours ago, and 10 hours ago
    File storageDir1hOld = new File(contentDir, getDatePath(blobCreated.minusHours(1)));
    assertThat(storageDir1hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1hOld, bytesFile1hOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3hOld = new File(contentDir, getDatePath(blobCreated.minusHours(3)));
    assertThat(storageDir3hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3hOld, bytesFile3hOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10hOld = new File(contentDir, getDatePath(blobCreated.minusHours(10)));
    assertThat(storageDir10hOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10hOld, bytesFile10hOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 day ago, 3 days ago, and 10 days ago
    File storageDir1dOld = new File(contentDir, getDatePath(blobCreated.minusDays(1)));
    assertThat(storageDir1dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1dOld, bytesFile1dOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3dOld = new File(contentDir, getDatePath(blobCreated.minusDays(3)));
    assertThat(storageDir3dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3dOld, bytesFile3dOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10dOld = new File(contentDir, getDatePath(blobCreated.minusDays(10)));
    assertThat(storageDir10dOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10dOld, bytesFile10dOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    // create 3 files: 1 month ago, 3 months ago, and 10 months ago
    File storageDir1mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(1)));
    assertThat(storageDir1mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir1mOld, bytesFile1mOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir3mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(3)));
    assertThat(storageDir3mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir3mOld, bytesFile3mOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    File storageDir10mOld = new File(contentDir, getDatePath(blobCreated.minusMonths(10)));
    assertThat(storageDir10mOld.mkdirs(), is(Boolean.TRUE));
    assertThat(new File(storageDir10mOld, bytesFile10mOld + ".bytes").createNewFile(), is(Boolean.TRUE));

    // find all files that have been created 5 min ago
    Duration fiveMin = Duration.ofSeconds(blobCreated.toEpochSecond() - blobCreated.minusMinutes(5L).toEpochSecond());
    DateBasedWalkFile walkFile = new DateBasedWalkFile(contentDir.getAbsolutePath(), fiveMin);
    List<String> blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(bytesFileNow, bytesFile1minOld, bytesFile3minOld));

    // find all files that have been created 5 hours ago
    Duration fiveHours = Duration.ofSeconds(blobCreated.toEpochSecond() - blobCreated.minusHours(5L).toEpochSecond());
    walkFile = new DateBasedWalkFile(contentDir.getAbsolutePath(), fiveHours);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        bytesFileNow, bytesFile1minOld, bytesFile3minOld, bytesFile10minOld,
        bytesFile1hOld, bytesFile3hOld));

    // find all files that have been created 5 days ago
    Duration fiveDays = Duration.ofSeconds(blobCreated.toEpochSecond() - blobCreated.minusDays(5L).toEpochSecond());
    walkFile = new DateBasedWalkFile(contentDir.getAbsolutePath(), fiveDays);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        bytesFileNow, bytesFile1minOld, bytesFile3minOld, bytesFile10minOld,
        bytesFile1hOld, bytesFile3hOld, bytesFile10hOld,
        bytesFile1dOld, bytesFile3dOld));

    // find all files that have been created 5 months ago
    Duration fiveMonths = Duration.ofSeconds(blobCreated.toEpochSecond() - blobCreated.minusMonths(5L).toEpochSecond());
    walkFile = new DateBasedWalkFile(contentDir.getAbsolutePath(), fiveMonths);
    blobIds = new ArrayList<>(walkFile.getBlobIdToDateRef().keySet());
    assertThat(blobIds, containsInAnyOrder(
        bytesFileNow, bytesFile1minOld, bytesFile3minOld, bytesFile10minOld,
        bytesFile1hOld, bytesFile3hOld, bytesFile10hOld,
        bytesFile1dOld, bytesFile3dOld, bytesFile10dOld,
        bytesFile1mOld, bytesFile3mOld));
  }

  private static String getDatePath(final OffsetDateTime blobCreated) {
    return blobCreated.format(BlobRef.DATE_TIME_PATH_FORMATTER);
  }
}
