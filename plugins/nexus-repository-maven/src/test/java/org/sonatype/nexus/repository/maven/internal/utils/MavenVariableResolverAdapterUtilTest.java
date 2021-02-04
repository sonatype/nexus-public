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
package org.sonatype.nexus.repository.maven.internal.utils;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;

import org.junit.Test;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.maven.MavenPath.SignatureType.GPG;
import static org.sonatype.nexus.repository.maven.internal.utils.MavenVariableResolverAdapterUtil.createCoordinateMap;

public class MavenVariableResolverAdapterUtilTest
    extends TestSupport
{
  @Test
  public void shouldCopyCoordinatesToMap() {
    Coordinates coordinates = new Coordinates(false, "org.mockito", "mockito-core",
        "3.24", 3600L, 100, "3.24", "test", ".jar", GPG);

    Map<String, String> map = createCoordinateMap(coordinates);

    assertThat(map, hasEntry("groupId", "org.mockito"));
    assertThat(map, hasEntry("artifactId", "mockito-core"));
    assertThat(map, hasEntry("version", "3.24"));
    assertThat(map, hasEntry("extension", ".jar"));
    assertThat(map, hasEntry("classifier", "test"));
  }

  @Test
  public void classifierShouldBeEmptyStringWhenNotSet() {
    Coordinates coordinates = new Coordinates(false, "org.mockito", "mockito-core",
        "3.24", 3600L, 100, "3.24", null, ".jar", GPG);

    Map<String, String> map = createCoordinateMap(coordinates);

    assertThat(map, hasEntry("classifier", EMPTY));
  }
}
