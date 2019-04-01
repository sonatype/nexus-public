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
package org.sonatype.nexus.repository.search;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.entity.EntityLog.UnknownDeltaException;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;

import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class IndexSyncServiceTest
    extends TestSupport
{
  @Mock
  Provider<DatabaseInstance> componentDatabase;

  @Mock
  ComponentEntityAdapter componentEntityAdapter;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  ApplicationDirectories directories;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  IndexRequestProcessor indexRequestProcessor;

  @Mock
  public IndexRebuildManager indexRebuildManager;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File nexusLsn;

  private IndexSyncService underTest;

  private long MARKER_POSITION = 0L;

  private long MARKER_SEGMENT = 35L;

  @Before
  public void setUp() throws Exception {
    File elasticsearchDir = temporaryFolder.newFolder();
    when(directories.getWorkDirectory("elasticsearch")).thenReturn(elasticsearchDir);
    underTest = new IndexSyncService(componentDatabase, componentEntityAdapter,
                                      assetEntityAdapter, directories, nodeAccess,
                                      indexRequestProcessor, indexRebuildManager);
    nexusLsn = new File(elasticsearchDir, "nexus.lsn");
  }

  @Test (expected = UnknownDeltaException.class)
  public void testLoadCheckpointFile_withUpgradeMarker() throws Exception {
    byte[] upgradeMarkerBytes = IndexSyncService.INDEX_UPGRADE_MARKER.getBytes();
    try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(nexusLsn))) {
      outputStream.write(upgradeMarkerBytes);
    }
    underTest.loadCheckpoint();
  }

  @Test
  public void testLoadCheckpointFile_withoutUpgradeMarker() throws Exception {
    try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(nexusLsn))) {
      new OLogSequenceNumber(MARKER_SEGMENT, MARKER_POSITION).toStream(outputStream);
    }
    OLogSequenceNumber oLogSequenceNumber = underTest.loadCheckpoint();
    assertThat(oLogSequenceNumber.getPosition(), is(MARKER_POSITION));
    assertThat(oLogSequenceNumber.getSegment(), is(MARKER_SEGMENT));
  }
}
