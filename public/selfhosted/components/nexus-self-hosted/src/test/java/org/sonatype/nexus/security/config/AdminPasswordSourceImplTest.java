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
package org.sonatype.nexus.security.config;

import java.io.IOException;

import org.sonatype.nexus.security.internal.AdminPasswordFileManagerImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AdminPasswordSourceImplTest
{
  @Mock
  private AdminPasswordFileManagerImpl adminPasswordFileManager;

  private AdminPasswordSourceImpl underTest;

  @Before
  public void setup() {
    underTest = new AdminPasswordSourceImpl(adminPasswordFileManager);
  }

  @Test
  public void shouldBePasswordFromFileWhenFileExists() throws Exception {
    when(adminPasswordFileManager.readFile()).thenReturn("password");

    assertThat(underTest.getPassword(false), is("password"));
    verify(adminPasswordFileManager, never()).writeFile(any());
  }

  @Test
  public void shouldBeDefaultPasswordWhenRandomIsFalse() throws Exception {
    assertThat(underTest.getPassword(false), is("admin123"));

    verify(adminPasswordFileManager, never()).writeFile(any());
  }

  @Test
  public void shouldBeRandomPasswordWhenRandomIsTruePasswordFileNotExistsAndPasswordFileWritten() throws Exception {
    when(adminPasswordFileManager.writeFile(any())).thenReturn(true);

    assertThat(underTest.getPassword(true), is(not("admin123")));

    verify(adminPasswordFileManager).writeFile(any());
  }

  @Test
  public void shouldBeDefaultPasswordWhenRandomIsTruePasswordFileNotExistsAndPasswordFileWriteFails() throws Exception {
    assertThat(underTest.getPassword(true), is("admin123"));

    verify(adminPasswordFileManager).writeFile(any());
  }

  /*
   * this test ensures that we don't inadvertently overwrite the serialized password from the first run
   */
  @Test
  public void testAdminPasswordIsWrittenOnlyOnce() throws IOException {
    when(adminPasswordFileManager.writeFile(any())).thenReturn(true);

    String password = underTest.getPassword(true);
    assertThat(password, is(notNullValue()));

    when(adminPasswordFileManager.readFile()).thenReturn(password);
    assertThat(underTest.getPassword(true), is(password));

    // should only write once
    verify(adminPasswordFileManager).writeFile(any());
  }
}
