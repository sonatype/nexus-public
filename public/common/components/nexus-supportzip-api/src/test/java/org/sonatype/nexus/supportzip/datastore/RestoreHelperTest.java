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
package org.sonatype.nexus.supportzip.datastore;

import java.io.File;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.ApplicationDirectories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RestoreHelper}
 */
@RunWith(MockitoJUnitRunner.class)
public class RestoreHelperTest
{
  private static final File DB_DIRECTORY = new File("/tmp/work/db");

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Test
  public void getDbPathReturnsWorkDirectoryAsPath() {
    when(applicationDirectories.getWorkDirectory("db", false)).thenReturn(DB_DIRECTORY);

    RestoreHelper underTest = new RestoreHelper(applicationDirectories);

    assertThat(underTest.getDbPath(), is(DB_DIRECTORY.toPath()));
  }

  @Test
  public void constructorResolvesDbWorkDirectory() {
    when(applicationDirectories.getWorkDirectory("db", false)).thenReturn(DB_DIRECTORY);

    new RestoreHelper(applicationDirectories);

    verify(applicationDirectories).getWorkDirectory("db", false);
  }

  @Test
  public void constructorRejectsNullApplicationDirectories() {
    assertThrows(NullPointerException.class, () -> new RestoreHelper(null));
  }

  @Test
  public void fileSuffixConstantIsJson() {
    assertThat(RestoreHelper.FILE_SUFFIX, is(".json"));
  }

  @Test
  public void getDbPathReturnsSameInstanceOnRepeatedCalls() {
    when(applicationDirectories.getWorkDirectory("db", false)).thenReturn(DB_DIRECTORY);

    RestoreHelper underTest = new RestoreHelper(applicationDirectories);

    assertThat(underTest.getDbPath(), is(sameInstance(underTest.getDbPath())));
  }
}
