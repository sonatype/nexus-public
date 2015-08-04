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
 * Test SnapshotRemoverTask to remove all artifacts
 *
 * @author marvin
 */
public class Nexus634RemoveAllIT
    extends AbstractSnapshotRemoverIT
{

  @Test
  public void removeAllSnapshots()
      throws Exception
  {
    // This is THE important part
    runSnapshotRemover("nexus-test-harness-snapshot-repo", 0, 0, true);

        /*
         * This IT is now very wrong, as snapshot remover will no longer remove -SNAPSHOT artifacts,
         * only timestamped snapshot artifacts (unless there is a release version and remove when released is set)
        // this IT is wrong: nexus will remove the parent folder too, if the GAV folder is emptied completely
        // Collection<File> jars = listFiles( artifactFolder, new String[] { "jar" }, false );
        // Assert.assertTrue( "All artifacts should be deleted by SnapshotRemoverTask. Found: " + jars, jars.isEmpty()
        // );

        // looking at the IT resources, there is only one artifact in there, hence, the dir should be removed
        Assert.assertFalse(
            "The folder should be removed since all artifacts should be gone, instead there are files left!",
            artifactFolder.exists() );
        */

    Collection<File> jars = listFiles(artifactFolder, new String[]{"jar"}, false);
    Assert.assertEquals(1, jars.size());
  }

}
