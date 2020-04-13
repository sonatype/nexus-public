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
package org.sonatype.nexus.repository.selector.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContentAuth}.
 */
public class ContentAuthTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repository-name";

  private static final String PATH = "path";

  private static final String FORMAT = "format";

  @Mock
  OrientContentAuthHelper contentAuthHelper;

  ContentAuth underTest;

  @Before
  public void setup() {
    underTest = new ContentAuth(contentAuthHelper);
  }

  @Test
  public void testPathPermitted() {
    when(contentAuthHelper.checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME})).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, REPOSITORY_NAME }, null), is(true));
    verify(contentAuthHelper, times(1)).checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME});
  }

  @Test
  public void testPathNotPermitted() {
    when(contentAuthHelper.checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME})).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, REPOSITORY_NAME }, null), is(false));
    verify(contentAuthHelper, times(1)).checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME});
  }

  @Test
  public void testPathPermitted_withGroupRepo() {
    when(contentAuthHelper.checkPathPermissions(PATH, FORMAT, new String[]{"group_repo"})).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, "group_repo" }, null), is(true));
    verify(contentAuthHelper).checkPathPermissions(PATH, FORMAT, new String[]{"group_repo"});
    verify(contentAuthHelper, never()).checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME});
  }

  @Test
  public void testPathNotPermitted_withGroupRepo() {
    when(contentAuthHelper.checkPathPermissions(PATH, FORMAT, new String[]{"group_repo"})).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, "group_repo" }, null), is(false));
    verify(contentAuthHelper).checkPathPermissions(PATH, FORMAT, new String[]{"group_repo"});
    verify(contentAuthHelper, never()).checkPathPermissions(PATH, FORMAT, new String[]{REPOSITORY_NAME});
  }

  @Test
  public void testPathPermitted_jexlOnly() {
    when(contentAuthHelper.checkPathPermissionsJexlOnly(PATH, FORMAT, new String[]{REPOSITORY_NAME})).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, REPOSITORY_NAME, true }, null),
        is(true));
    verify(contentAuthHelper, times(1)).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { REPOSITORY_NAME });
  }

  @Test
  public void testPathNotPermitted_jexlOnly() {
    when(contentAuthHelper.checkPathPermissionsJexlOnly(PATH, FORMAT, new String[]{REPOSITORY_NAME})).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, REPOSITORY_NAME, true }, null),
        is(false));
    verify(contentAuthHelper, times(1)).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { REPOSITORY_NAME });
  }

  @Test
  public void testPathPermitted_withGroupRepo_jexlOnly() {
    when(contentAuthHelper.checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { "group_repo" })).thenReturn(true);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, "group_repo", true }, null),
        is(true));
    verify(contentAuthHelper).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { "group_repo" });
    verify(contentAuthHelper, never()).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { REPOSITORY_NAME });
  }

  @Test
  public void testPathNotPermitted_withGroupRepo_jexlOnly() {
    when(contentAuthHelper.checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { "group_repo" })).thenReturn(false);
    assertThat(underTest.execute(underTest, null, null, new Object[] { PATH, FORMAT, "group_repo", true }, null),
        is(false));
    verify(contentAuthHelper).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { "group_repo" });
    verify(contentAuthHelper, never()).checkPathPermissionsJexlOnly(PATH, FORMAT, new String[] { REPOSITORY_NAME });
  }
}
