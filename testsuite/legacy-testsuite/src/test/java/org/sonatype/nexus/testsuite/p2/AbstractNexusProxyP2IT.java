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
package org.sonatype.nexus.testsuite.p2;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.nexus.test.utils.TestProperties;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.After;
import org.junit.Before;

import static org.sonatype.nexus.test.utils.FileTestingUtils.interpolationDirectoryCopy;

public abstract class AbstractNexusProxyP2IT
    extends AbstractNexusP2IT
{
  protected RemoteRepositories remoteRepositories;

  protected static final String localStorageDir;

  static {
    localStorageDir = TestProperties.getString("proxy.repo.base.dir");
  }

  protected AbstractNexusProxyP2IT() {
    super();
  }

  protected AbstractNexusProxyP2IT(final String testRepositoryId) {
    super(testRepositoryId);
  }

  @Before
  public void startProxy()
      throws Exception
  {
    remoteRepositories = RemoteRepositories.builder()
        .repo("remote", TestProperties.getString("proxy-repo-target-dir"))
        .build().start();
  }

  @After
  public void stopProxy()
      throws Exception
  {
    if (remoteRepositories != null) {
      remoteRepositories.stop();
      remoteRepositories = null;
    }
  }

  protected void replaceInFile(final String filename, final String target, final String replacement)
      throws IOException
  {
    String content = FileUtils.readFileToString(new File(filename));
    content = content.replace(target, replacement);
    FileUtils.write(new File(filename), content);
  }

  @Override
  protected void copyTestResources()
      throws IOException
  {
    super.copyTestResources();

    final File dest = new File(localStorageDir);

    if (dest.exists()) {
      FileUtils.forceDelete(dest);
    }

    File source = getTestResourceAsFile("proxy-repo");
    if (source == null || !source.exists()) {
      return;
    }

    interpolationDirectoryCopy(source, dest, TestProperties.getAll());
  }

}
