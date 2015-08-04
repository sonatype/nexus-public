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
package org.sonatype.nexus.testsuite.deploy.nexus4955;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;

import org.junit.Test;

/**
 * try to upload (deploy it) a file like "id" (no extension, just file name "id") into repo, nexus will die, it was
 * reported by user on mailing list
 *
 * @author Marvin Froeder ( velo at sonatype.com )
 */
public class NEXUS4955UploadWOExtensionIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void deploy()
      throws Exception
  {
    File file = getTestFile("content.txt");
    getDeployUtils().deployWithWagon("http", getRepositoryUrl(REPO_TEST_HARNESS_REPO), file,
        "nxcm4955/test/1.0/id");
  }
}
