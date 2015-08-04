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
package org.sonatype.nexus.testsuite.maven.nexus634;

import java.io.File;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test SnapshotRemoverTask to remove old artifacts but keep updated artifacts
 *
 * @author marvin
 */
public class Nexus634KeepNewSnapshotsIT
    extends AbstractSnapshotRemoverIT
{

  @Test
  public void keepNewSnapshots()
      throws Exception
  {
    // This is THE important part
    runSnapshotRemover("nexus-test-harness-snapshot-repo", 0, 10, true);

    Collection<File> jars = listFiles(artifactFolder, new String[]{"jar"}, false);
    Assert.assertEquals("SnapshotRemoverTask should remove only old artifacts.  Artifacts found: " + jars, jars.size(),
        1);
  }

}
