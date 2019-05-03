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

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.security.AbstractSecurityTest;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaticSecurityConfigurationSourceTest
    extends AbstractSecurityTest
{
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private PasswordService passwordService = mock(PasswordService.class);

  private ApplicationDirectories applicationDirectories = mock(ApplicationDirectories.class);

  private StaticSecurityConfigurationSource underTest;

  @Before
  public void setup() {
    underTest = new StaticSecurityConfigurationSource(applicationDirectories, passwordService, true);
    when(applicationDirectories.getWorkDirectory()).thenReturn(tmp.getRoot());
    when(passwordService.encryptPassword(any())).thenReturn("encrypted");
  }

  @Test
  public void testGetConfiguration_argNonRandom() throws IOException {
    underTest = new StaticSecurityConfigurationSource(applicationDirectories, passwordService, false);

    SecurityConfiguration configuration = underTest.getConfiguration();
    CUser user = configuration.getUser("admin");
    assertThat(user.getPassword(), is("encrypted"));
    verify(passwordService).encryptPassword("admin123");

    assertFalse(new File(tmp.getRoot(), "admin.password").exists());
  }

  @Test
  public void testGetConfiguration_randomGeneration() throws IOException {
    SecurityConfiguration configuration = underTest.getConfiguration();
    CUser user = configuration.getUser("admin");
    assertThat(user.getPassword(), is("encrypted"));
    String password = getFilePassword();
    verify(passwordService).encryptPassword(password);
  }

  @Test
  public void testGetConfiguration_adminUserStatusCheck() {
    SecurityConfiguration configuration = underTest.getConfiguration();
    CUser user = configuration.getUser("admin");
    assertThat(user.getStatus(), is(CUser.STATUS_CHANGE_PASSWORD));
  }

  /*
   * this test ensures that we don't inadvertently overwrite the serialized password from the first run
   */
  @Test
  public void testGetConfiguration_reloads() throws IOException {
    underTest.getConfiguration();
    String password = getFilePassword();

    underTest.getConfiguration();

    assertThat(getFilePassword(), is(password));
  }

  private String getFilePassword() throws IOException {
    return FileUtils.readFileToString(new File(tmp.getRoot(), "admin.password"));
  }
}
