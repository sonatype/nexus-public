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
package org.sonatype.nexus.content.maven.internal.search.table;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MavenSearchComponentPathFilterTest
    extends TestSupport
{
  private MavenSearchComponentPathFilter underTest;

  @Before
  public void setup() {
    underTest = new MavenSearchComponentPathFilter();
  }

  @Test
  public void shouldFilterMavenUncommonType() {
    String path = "foo/bar/foobar.jar.sha1";
    assertTrue(underTest.shouldFilterPathExtension(path));
  }

  @Test
  public void shouldNotFilterMavenCommonTypes() {
    validMavenPaths().forEach(path -> assertFalse(underTest.shouldFilterPathExtension(path)));
  }

  private List<String> validMavenPaths() {
    return asList("foo/bar/foobar.jar",
        "foo/bar/foobar.war",
        "foo/bar/foobar.aar",
        "foo/bar/foobar.zip",
        "foo/bar/foobar.pom",
        "foo/bar/foobar.tar.gz");
  }

}
