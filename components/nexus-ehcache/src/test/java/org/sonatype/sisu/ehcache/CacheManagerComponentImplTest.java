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
package org.sonatype.sisu.ehcache;

import java.io.File;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test for {@link CacheManagerComponentImpl}.
 */
public class CacheManagerComponentImplTest
    extends TestSupport
{
  private CacheManagerComponent underTest;

  private String originalTmpDir = System.getProperty("java.io.tmpdir");

  @Before
  public void setUp() throws Exception {
    System.setProperty("java.io.tmpdir", util.getTmpDir().getAbsolutePath());
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.shutdown();
      underTest = null;
    }

    System.setProperty("java.io.tmpdir", originalTmpDir);
  }

  @Test
  public void testConfigFromClasspath() throws Exception {
    underTest = new CacheManagerComponentImpl(null);
    assertConfigurationValid();
  }

  @Test
  public void testConfigFromFile() throws Exception {
    File file = util.resolveFile("src/test/resources/ehcache.xml");
    underTest = new CacheManagerComponentImpl(file);
    assertConfigurationValid();
  }

  private void assertConfigurationValid() {
    String path = underTest.getCacheManager().getDiskStorePath();
    File file = new File(path);

    assertThat(file.isAbsolute(), is(true));
    assertThat(file.getAbsolutePath(), is(new File(util.getTmpDir(), "ehcache").getAbsolutePath()));
  }
}
