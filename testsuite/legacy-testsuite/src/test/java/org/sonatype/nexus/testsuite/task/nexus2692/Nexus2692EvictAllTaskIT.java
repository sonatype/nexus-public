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
package org.sonatype.nexus.testsuite.task.nexus2692;

import java.util.SortedSet;

import org.junit.Assert;
import org.junit.Test;

public class Nexus2692EvictAllTaskIT
    extends AbstractEvictTaskIt
{
  @Test
  public void testEvictAllRepos()
      throws Exception
  {
    int days = 6;
    // run Task
    runTask(days, "all_repo");

    // check files
    SortedSet<String> resultStorageFiles = getItemFilePaths();

    SortedSet<String> expectedResults = buildListOfExpectedFilesForAllRepos(days);

    // calc the diff ( files that were deleted and should not have been )
    expectedResults.removeAll(resultStorageFiles);
    Assert.assertTrue("The following files were deleted and should not have been: "
        + expectedResults, expectedResults.isEmpty());

    // now the other way
    expectedResults = buildListOfExpectedFilesForAllRepos(days);
    resultStorageFiles.removeAll(expectedResults);
    Assert.assertTrue("The following files should have been deleted: "
        + resultStorageFiles, resultStorageFiles.isEmpty());

    // make sure we don't have any empty directories
    checkForEmptyDirectories();
  }
}
