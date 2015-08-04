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
package org.sonatype.nexus.maven.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.repository.LocalStatus;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author juven
 */
// This is an IT just because it runs longer then 15 seconds
public class DefaultSnapshotRemoverIT
    extends AbstractMavenRepoContentTests
{
  private SnapshotRemover snapshotRemover;
  
  @Override
  protected void setUp() throws Exception
  {
    super.setUp();
    snapshotRemover = lookup(SnapshotRemover.class);
  }

  protected void validateResults(MavenRepository repository, Map<String, Boolean> results)
      throws Exception
  {
    for (Map.Entry<String, Boolean> entry : results.entrySet()) {
      try {
        ResourceStoreRequest request = new ResourceStoreRequest(entry.getKey());

        repository.retrieveItem(false, request);

        // we succeeded, the value must be true
        assertTrue(
            "The entry '" + entry.getKey() + "' was found in repository '" + repository.getId() + "' !",
            entry.getValue());
      }
      catch (ItemNotFoundException e) {
        // we succeeded, the value must be true
        assertFalse("The entry '" + entry.getKey() + "' was not found in repository '" + repository.getId()
            + "' !", entry.getValue());
      }
    }
  }

  @Test
  public void testNexus2234()
      throws Exception
  {
    fillInRepo();

    long tenDaysAgo = System.currentTimeMillis() - 10 * 86400000L;

    final URL snapshotsRootUrl = new URL(snapshots.getLocalUrl());

    final File snapshotsRoot = new File(snapshotsRootUrl.toURI()).getAbsoluteFile();

    File itemFile =
        new File(snapshotsRoot,
            "/org/nonuniquesnapgroup/nonuniquesnap/1.1-SNAPSHOT/nonuniquesnap-1.1-SNAPSHOT.jar");

    itemFile.setLastModified(tenDaysAgo);

    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 1, 10, true);

    assertTrue(itemFile.exists());

    SnapshotRemovalResult result = lookup(SnapshotRemover.class).removeSnapshots(snapshotRemovalRequest);

    assertTrue(result.isSuccessful());

    assertTrue(itemFile.exists());
  }

  /**
   * @see <a href='https://issues.sonatype.org/browse/NEXUS-1331'>https://issues.sonatype.org/browse/NEXUS-1331</a>
   */
  @Test
  public void testNexus1331()
      throws Exception
  {
    fillInRepo();

    repositoryRegistry.getRepository("central").setLocalStatus(LocalStatus.OUT_OF_SERVICE);

    nexusConfiguration().saveConfiguration();

    // ---------------------------------
    // make the jar should be deleted, while the pom should be kept
    long threeDayAgo = System.currentTimeMillis() - 3 * 86400000L;

    final URL snapshotsRootUrl = new URL(snapshots.getLocalUrl());

    final File snapshotsRoot = new File(snapshotsRootUrl.toURI()).getAbsoluteFile();

    File itemFile =
        new File(snapshotsRoot,
            "/org/sonatype/nexus/nexus-indexer/1.0-beta-3-SNAPSHOT/nexus-indexer-1.0-beta-3-SNAPSHOT.jar");

    itemFile.setLastModified(threeDayAgo);
    // -----------------------------

    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 3, 1, true);

    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(snapshotRemovalRequest);

    assertTrue(result.isSuccessful());

  }

  @Test
  public void testSnapshotRemoverRemoveReleased()
      throws Exception
  {
    fillInRepo();

    // XXX: the test stuff is published on sonatype, so put the real central out of service for test
    repositoryRegistry.getRepository("central").setLocalStatus(LocalStatus.OUT_OF_SERVICE);

    nexusConfiguration().saveConfiguration();

    // and now setup the request
    // process the apacheSnapshots, leave min 1 snap, remove older than 0 day and delete them if release exists
    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 1, 0, true);

    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(snapshotRemovalRequest);

    assertEquals(1, result.getProcessedRepositories().size());

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    // 1.0-beta-4-SNAPSHOT should be nuked completely
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-cli.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-jdk14.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-sources.jar",
        Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.pom",
        Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.jar",
        Boolean.FALSE);

    // 1.0-beta-5-SNAPSHOT should have only one snapshot remaining, the newest
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.jar.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom.sha1",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar.sha1",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom.sha1",
        Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml.sha1", Boolean.TRUE);

    validateResults(snapshots, expecting);
  }

  @Test
  public void testSnapshotRemoverDoNotRemoveReleased()
      throws Exception
  {
    fillInRepo();

    // and now setup the request
    // process the apacheSnapshots, leave min 2 snap, do not remove released ones
    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 2, -1, false);

    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(snapshotRemovalRequest);

    assertEquals(1, result.getProcessedRepositories().size());

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    // 1.0-beta-4-SNAPSHOT should be untouched completely
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-cli.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-jdk14.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-sources.jar",
        Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.pom",
        Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.jar",
        Boolean.TRUE);

    // 1.0-beta-5-SNAPSHOT should have only two snapshot remaining, the two newest
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom",
        Boolean.TRUE);

    validateResults(snapshots, expecting);
  }


  @Test
  public void snapshotsNotRemovedIfReleasedBeforeGracePeriod()
      throws Exception
  {
    fillInRepo();

    // XXX: the test stuff is published on sonatype, so put the real central out of service for test
    repositoryRegistry.getRepository("central").setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    nexusConfiguration().saveConfiguration();

    SnapshotRemovalRequest snapshotRemovalRequest = new SnapshotRemovalRequest(
        snapshots.getId(), 1, 0, true, 2, false
    );

    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(snapshotRemovalRequest);

    assertThat(result.getProcessedRepositories().size(), is(1));
    assertThat(result.isSuccessful(), is(true));

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    // 1.0-beta-4-SNAPSHOT should not be removed because even if the release (1.4-beta-4) is present, the grace
    // period of two days did not yet pass
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-cli.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-jdk14.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-sources.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml",
        Boolean.TRUE
    );
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml.sha1",
        Boolean.TRUE
    );

    validateResults(snapshots, expecting);
  }

  @Test
  public void snapshotsRemovedIfReleasedAfterGracePeriod()
      throws Exception
  {
    fillInRepo();

    // XXX: the test stuff is published on sonatype, so put the real central out of service for test
    repositoryRegistry.getRepository("central").setLocalStatus(LocalStatus.OUT_OF_SERVICE);
    nexusConfiguration().saveConfiguration();

    SnapshotRemovalRequest snapshotRemovalRequest = new SnapshotRemovalRequest(
        snapshots.getId(), 1, 0, true, 2, false
    );

    final File released = new File(
        new File(new URL(releases.getLocalUrl()).toURI()),
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4/nexus-indexer-1.0-beta-4.pom"
    );

    // set the creation time 3 days ago, to simulate that grace period had passed
    released.setLastModified(System.currentTimeMillis() - 3 * 86400000L);

    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(snapshotRemovalRequest);

    assertThat(result.getProcessedRepositories().size(), is(1));
    assertThat(result.isSuccessful(), is(true));

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    // 1.0-beta-4-SNAPSHOT should be nuked
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-cli.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-jdk14.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT-sources.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/nexus-indexer-1.0-beta-4-SNAPSHOT.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml",
        Boolean.FALSE
    );
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-4-SNAPSHOT/maven-metadata.xml.sha1",
        Boolean.FALSE
    );

    validateResults(snapshots, expecting);
  }

  /**
   * When there are snapshot files and the metadata file is correct
   */
  @Test
  public void testHostedRepoWithMdCorrect()
      throws Exception
  {
    fillInRepo();

    Metadata mdBefore =
        readMavenMetadata(retrieveFile(snapshots, "org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/maven-metadata.xml"));

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 1, -1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    Metadata mdAfter =
        readMavenMetadata(retrieveFile(snapshots, "org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/maven-metadata.xml"));

    // cstamas: simply not true since maven md 1.1 support
    // assertEquals( mdBefore.getVersioning().getLastUpdated(), mdAfter.getVersioning().getLastUpdated() );
    assertEquals(mdBefore.getVersioning().getSnapshot().getTimestamp(),
        mdAfter.getVersioning().getSnapshot().getTimestamp());
    assertEquals(mdBefore.getVersioning().getSnapshot().getBuildNumber(),
        mdAfter.getVersioning().getSnapshot().getBuildNumber());

  }

  /**
   * When all the snapshot files are removed, but there's other version
   */
  @Test
  public void testHostedRepoWithMdRemoved1()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 0, 1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    // whole version folder was removed, including version metadata
    expecting.put("org/sonatype/nexus/nexus/1.3.0-SNAPSHOT", Boolean.FALSE);
    expecting.put("org/sonatype/nexus/nexus/1.2.2-SNAPSHOT", Boolean.TRUE);
    // artifact metadata does exist
    expecting.put("org/sonatype/nexus/nexus/maven-metadata.xml", Boolean.TRUE);

    validateResults(snapshots, expecting);

    Metadata md = readMavenMetadata(retrieveFile(snapshots, "org/sonatype/nexus/nexus/maven-metadata.xml"));
    assertFalse("The artifact metadata should not contain the removed version!",
        md.getVersioning().getVersions().contains("1.3.0-SNAPSHOT"));
  }

  /**
   * When all the snapshot files are removed, and all versions are removed
   */
  @Test
  public void testHostedRepoWithMdRemoved2()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 0, -1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    // whole version folder was removed, including version metadata
    expecting.put("org/sonatype/nexus/nexus/1.3.0-SNAPSHOT", Boolean.FALSE);

    // This is no longer valid, since we will NEVER remove non-timestamped artifacts
    // Unless a release version is found, and remove if released is in effect
    // so changed from FALSE to TRUE
    expecting.put("org/sonatype/nexus/nexus/1.2.2-SNAPSHOT", Boolean.TRUE);
    expecting.put("org/sonatype/nexus/nexus/maven-metadata.xml", Boolean.TRUE);

    validateResults(snapshots, expecting);
  }

  /**
   * When the metadata is incorrect, fix it
   */
  @Test
  public void testHostedRepoWithMdIncorrect()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 2, -1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-3-SNAPSHOT/maven-metadata.xml", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-3-SNAPSHOT/maven-metadata.xml.sha1", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-3-SNAPSHOT/maven-metadata.xml.md5", Boolean.TRUE);

    validateResults(snapshots, expecting);

    Metadata md =
        readMavenMetadata(retrieveFile(snapshots,
            "org/sonatype/nexus/nexus-indexer/1.0-beta-3-SNAPSHOT/maven-metadata.xml"));

    Assert.assertEquals(2, md.getVersioning().getSnapshot().getBuildNumber());
    Assert.assertEquals("20010711.162119", md.getVersioning().getSnapshot().getTimestamp());
  }

  /**
   * When the metadata is missing, fix it
   */
  @Test
  public void testHostedRepoWithMdMissing()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 1, -1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();

    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml.sha1", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/maven-metadata.xml.md5", Boolean.TRUE);

    validateResults(snapshots, expecting);

  }

  @Test
  public void testMinToKeep()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 1, 1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-20080123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-SNAPSHOT.pom", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090202.142204-272.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090209.062729-356.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090210.090218-375.pom", Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom",
        Boolean.TRUE);

    validateResults(snapshots, expecting);
  }

  @Test
  public void testAllReposNotDoingRepoMoreThanOnce()
      throws Exception
  {
    fillInRepo();

    // run on the public group, which contains the snapshot repo
    SnapshotRemovalRequest request = new SnapshotRemovalRequest(null, 1, 1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    // we should have skipped once, when processing the public group
    assertEquals("should have found 1 instance of skipped repo", 1,
        result.getProcessedRepositories().get("snapshots").getSkippedCount());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-20080123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-SNAPSHOT.pom", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090202.142204-272.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090209.062729-356.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090210.090218-375.pom", Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom",
        Boolean.TRUE);

    validateResults(snapshots, expecting);
  }

  @Test
  public void testGroup()
      throws Exception
  {
    fillInRepo();

    // run on the public group, which contains the snapshot repo
    SnapshotRemovalRequest request = new SnapshotRemovalRequest("public", 1, 1, false);
    SnapshotRemovalResult result = snapshotRemover.removeSnapshots(request);

    assertTrue(result.isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-20080123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.2.2-SNAPSHOT/nexus-1.2.2-SNAPSHOT.pom", Boolean.TRUE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.160704-197.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090202.142204-272.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090209.062729-356.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090210.090218-375.pom", Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080711.162119-2.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080718.231118-50.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080730.002543-149.pom",
        Boolean.FALSE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.jar",
        Boolean.TRUE);
    expecting.put(
        "/org/sonatype/nexus/nexus-indexer/1.0-beta-5-SNAPSHOT/nexus-indexer-1.0-beta-5-20080731.150252-163.pom",
        Boolean.TRUE);

    validateResults(snapshots, expecting);
  }

  @Test
  public void testContinueOnException()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 0, -1, false);

    assertTrue(snapshotRemover.removeSnapshots(request).isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    expecting.put(
        "/org/myorg/very.very.long.project.id/1.1-SNAPSHOT/very.very.long.project.id-1.1-20070807.081844-1.jar",
        Boolean.FALSE);
    expecting.put(
        "/org/myorg/very.very.long.project.id/1.0.0-SNAPSHOT/1.0.0-SNAPSHOT/very.very.long.project.id-1.0.0-20070807.081844-1.jar",
        Boolean.FALSE);
    validateResults(snapshots, expecting);

    // we could not retrieve the illegal artifact, but we can check the file system
    File snapshotsStorageBase = new File(getWorkHomeDir(), "storage/" + snapshots.getId());
    File illegalArtifact =
        new File(
            snapshotsStorageBase,
            "org/myorg/very.very.long.project.id/1.0.0-SNAPSHOT/1.0.0-SNAPSHOT/very.very.long.project.id-1.0.0-20070807.081844-1.jar");
    assertTrue(illegalArtifact.exists());
  }

  /**
   * @see <a href='https://issues.sonatype.org/browse/NEXUS-3148'>NEXUS-3148</a>
   */
  @Test
  public void testEndWithSNAPSHOT()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest request = new SnapshotRemovalRequest(snapshots.getId(), 0, -1, false);

    assertTrue(snapshotRemover.removeSnapshots(request).isSuccessful());

    HashMap<String, Boolean> expecting = new HashMap<String, Boolean>();
    expecting.put("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.064938-1.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.064938-1.jar", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.065026-2.pom", Boolean.FALSE);
    expecting.put("/org/sonatype/nexus-3148/1.0.SNAPSHOT/nexus-3148-1.0.20100111.065026-2.jar", Boolean.FALSE);

    validateResults(snapshots, expecting);
  }

  private Metadata readMavenMetadata(File mdFle)
      throws MetadataException, IOException
  {
    FileInputStream inputStream = new FileInputStream(mdFle);
    Metadata md = null;

    try {
      md = MetadataBuilder.read(inputStream);
    }
    finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        }
        catch (IOException e1) {
        }
      }
    }
    return md;
  }

}
