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
package org.sonatype.nexus.testsuite.feed.nexus3929;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.UserCreationUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.StatusMatchers.isSuccess;

public class Nexus3929TimelineCorruptionIT
    extends AbstractNexusIntegrationTest
{

  @Override
  @Before
  public void oncePerClassSetUp()
      throws Exception
  {
    synchronized (AbstractNexusIntegrationTest.class) {
      if (NEEDS_INIT) {
        super.oncePerClassSetUp();

        stopNexus();

        final File tl = new File(nexusWorkDir, "timeline/index");
        while (getTimelineLuceneSegments(tl).size() < 7) {
          startNexus();
          stopNexus();
        }

        List<File> cfs = getTimelineLuceneSegments(tl);
        // just delete some files to wreck the index
        FileUtils.forceDelete(cfs.get(0));
        FileUtils.forceDelete(cfs.get(2));
        FileUtils.forceDelete(cfs.get(5));

        startNexus();
      }
    }
  }

  protected List<File> getTimelineLuceneSegments(final File timelineLuceneIndexDirectory) {
    @SuppressWarnings("unchecked")
    final List<File> luceneFiles =
        new ArrayList<File>(FileUtils.listFiles(timelineLuceneIndexDirectory, new String[]{
            "cfs", "fnm", "fdt",
            "fdx", "frm", "frq", "nrm", "prx", "tii", "tis"
        }, false));

    // filter it
    final Iterator<File> lfi = luceneFiles.iterator();
    while (lfi.hasNext()) {
      final File luceneFile = lfi.next();
      if (luceneFile.isFile() && luceneFile.getName().startsWith("segments.")) {
        lfi.remove();
      }
    }

    return luceneFiles;
  }

  @Test
  public void login()
      throws Exception
  {
    assertThat(UserCreationUtil.login(), isSuccess());
  }

  @Test
  public void status()
      throws Exception
  {
    Assert.assertEquals("STARTED", getNexusStatusUtil().getNexusStatus().getData().getState());
  }
}
