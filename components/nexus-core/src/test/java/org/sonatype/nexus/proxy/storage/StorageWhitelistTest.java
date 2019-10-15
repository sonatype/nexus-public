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
package org.sonatype.nexus.proxy.storage;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @since 2.14.15
 */
public class StorageWhitelistTest
{
  private static final String ALLOWED_PATH = "/dir1/dir2/dir3";

  private StorageWhitelist underTest;

  @Before
  public void setup() {
    underTest = new StorageWhitelist(ALLOWED_PATH);
  }

  @Test
  public void testIsApproved() {
    assertThat(underTest.isApproved(ALLOWED_PATH), is(true));
    assertThat(underTest.isApproved("/foo"), is(false));
    assertThat(underTest.isApproved(""), is(false));
    assertThat(underTest.isApproved("/"), is(false));
  }

  @Test
  public void testAddWhitelistPath() {
    String path = "/foo/bar/baz";
    assertThat(underTest.isApproved(path), is(false));
    underTest.addWhitelistPath(path);
    assertThat(underTest.isApproved(path), is(true));
  }

  @Test
  public void trailingSlashIgnored() {
    assertThat(underTest.isApproved(ALLOWED_PATH +"/"), is(true));
    assertThat(underTest.isApproved("/foldera/folderb/folderd"), is(false));
    assertThat(underTest.isApproved("/foldera/folderb/folderd/"), is(false));
  }

  @Test
  public void subdirectoriesAreAllowed() {
    assertThat(underTest.isApproved(ALLOWED_PATH + "/subfolder"), is(true));
  }

  @Test
  public void intermediarySubdirectoriesNotAllowed() {
    assertThat(underTest.isApproved("/dir1/subdir"), is(false));
    assertThat(underTest.isApproved("/dir1/dir2/subdir"), is(false));
    assertThat(underTest.isApproved("/dir1/dir10/subdir"), is(false));
  }

  @Test
  public void reverseTraversalNotAllowed() {
    assertThat(underTest.isApproved(ALLOWED_PATH + "/../../../usr/local"), is(false));
  }

  @Test
  public void testWindowsPathParsing() {
    assertThat(underTest.isApproved("file:/C:/temp"), is(false));
  }
}
