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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class PathUtilsTest
{
  @Test
  public void depth() {
    assertThat(PathUtils.depthOf(""), equalTo(0));
    assertThat(PathUtils.depthOf("/"), equalTo(0));
    assertThat(PathUtils.depthOf("///"), equalTo(0));
    assertThat(PathUtils.depthOf("/org"), equalTo(1));
    assertThat(PathUtils.depthOf("/org/apache"), equalTo(2));
    assertThat(PathUtils.depthOf("/org/apache/maven"), equalTo(3));
    assertThat(PathUtils.depthOf("/org/apache/maven/"), equalTo(3));
    assertThat(PathUtils.depthOf("/org/apache///maven/"), equalTo(3));
  }

  @Test
  public void elements() {
    assertThat(PathUtils.elementsOf("").size(), equalTo(0));
    assertThat(PathUtils.elementsOf("/").size(), equalTo(0));
    assertThat(PathUtils.elementsOf("///").size(), equalTo(0));
    assertThat(PathUtils.elementsOf("/org"), contains("org"));
    assertThat(PathUtils.elementsOf("/org/apache"), contains("org", "apache"));
    assertThat(PathUtils.elementsOf("/org/apache/maven"), contains("org", "apache", "maven"));
    assertThat(PathUtils.elementsOf("/org/apache/maven/"), contains("org", "apache", "maven"));
    assertThat(PathUtils.elementsOf("/org/apache///maven/"), contains("org", "apache", "maven"));
  }

  @Test
  public void path() {
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("")), equalTo("/"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/")), equalTo("/"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("///")), equalTo("/"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org")), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache")), equalTo("/org/apache"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven")), equalTo("/org/apache/maven"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven/")), equalTo("/org/apache/maven"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache///maven/")), equalTo("/org/apache/maven"));

    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/archetype-catalog.xml")), equalTo("/archetype-catalog.xml"));
  }

  @Test
  public void pathWithMax() {
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org"), 1), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache"), 1), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven"), 1), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven/foo"), 1), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven/foo/bar"), 1), equalTo("/org"));

    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org"), 3), equalTo("/org"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache"), 3), equalTo("/org/apache"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven"), 3), equalTo("/org/apache/maven"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven/foo"), 3),
        equalTo("/org/apache/maven"));
    assertThat(PathUtils.pathFrom(PathUtils.elementsOf("/org/apache/maven/foo/bar"), 3),
        equalTo("/org/apache/maven"));
  }
}
