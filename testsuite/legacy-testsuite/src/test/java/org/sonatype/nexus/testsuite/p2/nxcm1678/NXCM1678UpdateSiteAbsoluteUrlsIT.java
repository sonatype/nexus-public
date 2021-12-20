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
package org.sonatype.nexus.testsuite.p2.nxcm1678;

import java.io.File;

import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;

public class NXCM1678UpdateSiteAbsoluteUrlsIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1678UpdateSiteAbsoluteUrlsIT() {
    super("nxcm1678");
  }

  @Test
  public void test()
      throws Exception
  {
    final File nexusDir = new File(nexusWorkDir, "storage/nxcm1678");

    TaskScheduleUtil.run("1");
    TaskScheduleUtil.waitForAllTasksToStop();

    assertThat(new File(nexusDir, "features/com.sonatype.nexus.p2.its.feature_1.0.0.jar"), exists());
    assertThat(new File(nexusDir, "plugins/com.sonatype.nexus.p2.its.bundle_1.0.0.jar"), exists());
  }

}
