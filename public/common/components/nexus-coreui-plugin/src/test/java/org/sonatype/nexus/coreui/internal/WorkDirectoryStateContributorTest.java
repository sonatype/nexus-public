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
package org.sonatype.nexus.coreui.internal;

import java.io.File;
import java.util.Map;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.coreui.internal.WorkDirectoryStateContributor.WORK_DIRECTORY_KEY;

public class WorkDirectoryStateContributorTest
{
  @Test
  public void shouldReturnWorkDirectoryAbsolutePath() {
    ApplicationDirectories directories = mock(ApplicationDirectories.class);
    when(directories.getWorkDirectory()).thenReturn(new File("/nexus-data"));

    WorkDirectoryStateContributor underTest = new WorkDirectoryStateContributor(directories);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(WORK_DIRECTORY_KEY), is("/nexus-data"));
  }

  @Test
  public void shouldReturnAbsolutePathForRelativeWorkDirectory() {
    ApplicationDirectories directories = mock(ApplicationDirectories.class);
    File workDir = new File("relative/path");
    when(directories.getWorkDirectory()).thenReturn(workDir);

    WorkDirectoryStateContributor underTest = new WorkDirectoryStateContributor(directories);

    Map<String, Object> state = underTest.getState();

    assertThat(state.get(WORK_DIRECTORY_KEY), is(workDir.getAbsolutePath()));
  }
}
