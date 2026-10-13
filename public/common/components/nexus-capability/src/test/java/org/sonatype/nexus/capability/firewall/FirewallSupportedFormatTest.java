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
package org.sonatype.nexus.capability.firewall;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FirewallSupportedFormatTest
{

  @Test
  public void testEnumValues() {
    FirewallSupportedFormat[] expectedValues = {
        FirewallSupportedFormat.R,
        FirewallSupportedFormat.GO,
        FirewallSupportedFormat.P2,
        FirewallSupportedFormat.RUBYGEMS,
        FirewallSupportedFormat.NPM,
        FirewallSupportedFormat.YUM,
        FirewallSupportedFormat.PYPI,
        FirewallSupportedFormat.CARGO,
        FirewallSupportedFormat.CONAN,
        FirewallSupportedFormat.CONDA,
        FirewallSupportedFormat.NUGET,
        FirewallSupportedFormat.MAVEN2,
        FirewallSupportedFormat.COMPOSER,
        FirewallSupportedFormat.COCOAPODS,
        FirewallSupportedFormat.HUGGINGFACE,
        FirewallSupportedFormat.SWIFT,
        FirewallSupportedFormat.DOCKER,
        FirewallSupportedFormat.RAW,
        FirewallSupportedFormat.PUB
    };
    assertArrayEquals(expectedValues, FirewallSupportedFormat.values());
  }

  @Test
  public void testGetValue() {
    assertEquals("r", FirewallSupportedFormat.R.getValue());
    assertEquals("go", FirewallSupportedFormat.GO.getValue());
    assertEquals("p2", FirewallSupportedFormat.P2.getValue());
    assertEquals("rubygems", FirewallSupportedFormat.RUBYGEMS.getValue());
    assertEquals("npm", FirewallSupportedFormat.NPM.getValue());
    assertEquals("yum", FirewallSupportedFormat.YUM.getValue());
    assertEquals("pypi", FirewallSupportedFormat.PYPI.getValue());
    assertEquals("cargo", FirewallSupportedFormat.CARGO.getValue());
    assertEquals("conan", FirewallSupportedFormat.CONAN.getValue());
    assertEquals("conda", FirewallSupportedFormat.CONDA.getValue());
    assertEquals("nuget", FirewallSupportedFormat.NUGET.getValue());
    assertEquals("maven2", FirewallSupportedFormat.MAVEN2.getValue());
    assertEquals("composer", FirewallSupportedFormat.COMPOSER.getValue());
    assertEquals("cocoapods", FirewallSupportedFormat.COCOAPODS.getValue());
    assertEquals("huggingface", FirewallSupportedFormat.HUGGINGFACE.getValue());
    assertEquals("swift", FirewallSupportedFormat.SWIFT.getValue());
    assertEquals("docker", FirewallSupportedFormat.DOCKER.getValue());
    assertEquals("raw", FirewallSupportedFormat.RAW.getValue());
    assertEquals("pub", FirewallSupportedFormat.PUB.getValue());
  }

  @Test
  public void testGetValues() {
    String[] expectedValues = {
        "r", "go", "p2", "rubygems", "npm", "yum", "pypi", "cargo", "conan", "conda", "nuget", "maven2",
        "composer", "cocoapods", "huggingface", "swift", "docker", "raw", "pub"
    };
    assertArrayEquals(expectedValues, FirewallSupportedFormat.getValues());
  }
}
