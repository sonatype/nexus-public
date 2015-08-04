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
package org.sonatype.nexus.index;

import java.io.File;
import java.net.URL;

import org.sonatype.nexus.proxy.repository.GroupRepository;

import org.junit.Test;

public class GroupReindexIndexerManagerTest
    extends AbstractIndexerManagerTest
{

  @Test
  public void testGroupReindex()
      throws Exception
  {
    fillInRepo();

    GroupRepository group = (GroupRepository) repositoryRegistry.getRepository("public");

    File groupRoot = new File(new URL(group.getLocalUrl()).toURI());
    File index = new File(groupRoot, ".index");

    File indexFile = new File(index, "nexus-maven-repository-index.gz");
    File incrementalIndexFile = new File(index, "nexus-maven-repository-index.1.gz");

    assertFalse("No index .gz file should exist.", indexFile.exists());
    assertFalse("No incremental chunk should exists.", incrementalIndexFile.exists());

    indexerManager.reindexRepository(null, group.getId(), true);

    assertTrue("Index .gz file should exist.", indexFile.exists());
    assertFalse("No incremental chunk should exists.", incrementalIndexFile.exists());

    // copy some _new_ stuff, not found in any of the members
    File sourceApacheSnapshotsRoot = new File(getBasedir(), "src/test/resources/reposes/apache-snapshots-2");
    File snapshotsRoot = new File(new URL(snapshots.getLocalUrl()).toURI());
    copyDirectory(sourceApacheSnapshotsRoot, snapshotsRoot);
    indexerManager.reindexRepository(null, group.getId(), false);

    assertTrue("Index .gz file should exist.", indexFile.exists());
    assertTrue("Incremental chunk should exists.", incrementalIndexFile.exists());

    assertTrue("We expected less than 300 bytes but got " + incrementalIndexFile.length(),
        incrementalIndexFile.length() < 300);

  }
}
