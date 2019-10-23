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
package org.sonatype.nexus.repository.cocoapods.internal.pod;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PodPathParserTest
    extends TestSupport
{
  private PodPathParser podPathParser = new PodPathParser("api.github.com");

  @Test
  public void shouldParsePodPathWithHttpUri() {
    final PodInfo info = podPathParser.parse("pods/name/version/http/example.com/test.tar.gz?param=value");
    assertEquals(info.getName(), "name");
    assertEquals(info.getVersion(), "version");
    assertEquals(info.getUri().toString(), "http://example.com/test.tar.gz?param=value");
  }

  @Test
  public void shouldParsePodPathWithGithubUri() {
    final PodInfo info = podPathParser.parse("pods/name/version/https/api.github.com/test.tar.gz");
    assertEquals(info.getName(), "name");
    assertEquals(info.getVersion(), "version");
    assertEquals(info.getUri().toString(), "https://api.github.com/test");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionOnPathStartingWithSlash() {
    podPathParser.parse("/pods/name/version/https/api.github.com/test.tar.gz");
  }
}
