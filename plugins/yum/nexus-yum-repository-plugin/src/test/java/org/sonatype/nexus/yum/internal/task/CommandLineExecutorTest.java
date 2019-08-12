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
package org.sonatype.nexus.yum.internal.task;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandLineExecutorTest
    extends YumNexusTestSupport
{
  private CommandLineExecutor underTest;

  private ApplicationDirectories applicationDirectories;

  @Before
  public void setup() {
    applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getWorkDirectory()).thenReturn(new File("/a/dir"));
    underTest = new CommandLineExecutor(applicationDirectories, "createrepo,mergerepo");
  }

  //IOException should be thrown as validation will pass, but executable isn't there
  @Test(expected = IOException.class)
  public void exec_createRepo() throws Exception
  {
    underTest.exec("/fake/path/createrepo", "--version");
  }

  //IOException should be thrown as validation will pass, but executable isn't there
  @Test(expected = IOException.class)
  public void exec_createRepoNoPath() throws Exception
  {
    underTest = new CommandLineExecutor(applicationDirectories, "createreposhouldntexist,mergereposhouldntexist");
    underTest.exec("createreposhouldntexist", "--version");
  }

  //IOException should be thrown as validation will pass, but executable isn't there
  @Test(expected = IOException.class)
  public void exec_mergeRepo() throws Exception
  {
    underTest.exec("/fake/path/mergerepo", "--version");
  }

  //IOException should be thrown as validation will pass, but executable isn't there
  @Test(expected = IOException.class)
  public void exec_mergeRepoNoPath() throws Exception
  {
    underTest = new CommandLineExecutor(applicationDirectories, "createreposhouldntexist,mergereposhouldntexist");
    underTest.exec("mergereposhouldntexist", "--version");
  }

  //IllegalAccessException should be thrown as this executable isn't allowed
  @Test(expected = IllegalAccessException.class)
  public void exec_notAllowed() throws Exception
  {
    underTest.exec("/fake/path/somethingelse", "--someotherflag");
  }

  //IllegalAccessException should be thrown as commands launching things inside nexus are not allowed
  @Test(expected = IllegalAccessException.class)
  public void exec_pathNotAllowed() throws Exception
  {
    underTest.exec("/a/dir/createrepo", "--someotherflag");
  }

  @Test(expected = IllegalAccessException.class)
  public void exec_extraConfigNotAllowed() throws Exception
  {
    underTest.exec("/fake/path/createrepo --somethingbad", "--someotherflag");
  }
}
