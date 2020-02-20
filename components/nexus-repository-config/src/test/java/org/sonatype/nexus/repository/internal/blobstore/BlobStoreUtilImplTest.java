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
package org.sonatype.nexus.repository.internal.blobstore;

import java.io.File;

import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.sonatype.goodies.testsupport.TestSupport;

public class BlobStoreUtilImplTest extends TestSupport
{

  private static final int MAX_NAME_LENGTH = 255;

  private BlobStoreUtilImpl underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private RepositoryManager repositoryManager;

  @Before
  public void setup() throws Exception {
    underTest = new BlobStoreUtilImpl(repositoryManager);
  }

  @Test
  public void testRequirePath() throws Exception {
    exception.expect(NullPointerException.class);
    underTest.validateFilePath(null, MAX_NAME_LENGTH);
  }

  @Test
  public void testRequirePositiveNonZeroLength() throws Exception {
    exception.expectMessage("maxLength should be greater than zero.");
    exception.expect(IllegalArgumentException.class);
    underTest.validateFilePath("test", 0);
  }

  @Test
  public void testAcceptShortPath() throws Exception {
    String shortPath = File.separator + "test" + File.separator + "test";
    String longMemberFolder = Strings.padEnd("", MAX_NAME_LENGTH, 'x');

    assertTrue(underTest.validateFilePath(shortPath, MAX_NAME_LENGTH));

    assertTrue(underTest.validateFilePath(shortPath + File.separator + "longer", 255));

    assertTrue(underTest.validateFilePath(shortPath + File.separator + longMemberFolder, 255));
  }

  @Test
  public void testRejectLongPath() throws Exception {
    String shortPath = File.separator + "test" + File.separator + "testtestte";

    assertFalse(underTest.validateFilePath(shortPath, 9));

    String longMemberFolder = Strings.padEnd("", MAX_NAME_LENGTH + 1, 'x');
    shortPath = File.separator + longMemberFolder + shortPath;

    assertFalse(underTest.validateFilePath(shortPath, MAX_NAME_LENGTH));
  }
}
