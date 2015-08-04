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
package org.sonatype.nexus.util;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author juven
 */
public class ItemPathUtilsTest
  extends TestSupport
{
  @Test
  public void testGetLCPPathFromPair()
      throws Exception
  {
    String pathA = "";
    String pathB = "/org";
    Assert.assertNull(PathUtils.getLCPPath(pathA, pathB));

    pathA = null;
    pathB = "/org";
    Assert.assertNull(PathUtils.getLCPPath(pathA, pathB));

    pathA = "/org/apache/maven";
    pathB = "/org/apache/";
    String expected = "/org/apache/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(pathA, pathB));

    pathA = "org/sonatype/nexus/";
    pathB = "org/sonatype/nexus/nexus-api";
    expected = "org/sonatype/nexus/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(pathA, pathB));

    pathA = "/commons-attributes/commons-attributes-api/2.1/commons-attributes-api-2.1.pom";
    pathB = "/commons-io/commons-io/1.3.1/commons-io-1.3.1.pom";
    expected = "/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(pathA, pathB));

    pathA = "/commons-io/commons-io/1.4/commons-io-1.4-sources.jar";
    pathB = "/commons-io/commons-io/1.3.1/commons-io-1.3.1.pom";
    expected = "/commons-io/commons-io/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(pathA, pathB));

    pathA = "/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom";
    pathB = "/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090212.053150-427.pom";
    expected = "/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(pathA, pathB));
  }

  @Test
  public void testGetLCPPathFromCollection()
      throws Exception
  {
    List<String> paths = new ArrayList<String>();
    Assert.assertNull(PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("");
    paths.add("/org/apache");
    Assert.assertNull(PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("/");
    String expected = "/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom");
    paths.add("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090212.053150-427.pom");
    expected = "/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090123.170636-198.pom");
    paths.add("/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/nexus-1.3.0-20090212.053150-427.pom");
    paths.add("/org/sonatype/nexus/nexus/1.2.0.4/nexus-1.2.0.4.pom");
    expected = "/org/sonatype/nexus/nexus/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("/org/apache/maven/plugins/maven-archetype-plugin/2.0-alpha-5-SNAPSHOT");
    paths.add("/org/apache/maven/plugins/maven-idea-plugin/2.3-SNAPSHOT/maven-idea-plugin-2.3-SNAPSHOT.pom");
    paths.add("/org/apache/maven/plugins/maven-rar-plugin/2.2/maven-rar-plugin-2.2.pom");
    expected = "/org/apache/maven/plugins/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(paths));

    paths.clear();
    paths.add("/");
    paths.add("/org/apache/maven/plugins/maven-archetype-plugin/2.0-alpha-5-SNAPSHOT");
    expected = "/";
    Assert.assertEquals(expected, PathUtils.getLCPPath(paths));
  }

  @Test
  public void testPathDepth() {
    assertThat(PathUtils.getPathDepth("/"), equalTo(0));
    assertThat(PathUtils.getPathDepth("/archetype-catalog.xml"), equalTo(0));
    assertThat(PathUtils.getPathDepth("/foo/archetype-catalog.xml"), equalTo(1));
    assertThat(PathUtils.getPathDepth("/foo/bar/archetype-catalog.xml"), equalTo(2));
    assertThat(PathUtils.getPathDepth("/0/1/2/3/4/5"), equalTo(5));
    assertThat(PathUtils.getPathDepth("/0/1/2/3/4/5/6/7/8/9/10"), equalTo(10));
  }
}
