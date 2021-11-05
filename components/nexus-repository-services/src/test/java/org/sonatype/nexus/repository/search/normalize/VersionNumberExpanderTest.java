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
package org.sonatype.nexus.repository.search.normalize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionNumberExpanderTest
{
  @Test
  public void testBlank() {
    assertEquals("", VersionNumberExpander.expand(null));
    assertEquals("", VersionNumberExpander.expand(""));
    assertEquals("", VersionNumberExpander.expand(" "));
  }

  @Test
  public void noExpansion() {
    assertEquals("alpha", VersionNumberExpander.expand("alpha"));
    assertEquals("beta release", VersionNumberExpander.expand("beta release"));
  }

  @Test
  public void numbers() {
    assertEquals("000000001", VersionNumberExpander.expand("1"));
    assertEquals("000023007", VersionNumberExpander.expand("23007"));
    assertEquals("000000001.000000000.000000002", VersionNumberExpander.expand("1.0.2"));
    assertEquals("000000001.000000023.000000004", VersionNumberExpander.expand("1.23.4"));
  }

  @Test
  public void mixedText() {
    assertEquals("000000001alpha", VersionNumberExpander.expand("1alpha"));
    assertEquals("beta-000000002", VersionNumberExpander.expand("beta-2"));
    assertEquals("000000001.000000000a000000004", VersionNumberExpander.expand("1.0a4"));
    assertEquals("beta-000000001.000000023-alpha000000004-snapshot", VersionNumberExpander.expand("beta-1.23-alpha4-snapshot"));
  }

  @Test
  public void longNumber() {
    assertEquals("v000000001-rev020181217-000000001.000000027.0123456789123456789123456789", VersionNumberExpander.expand("v1-rev20181217-1.27.0123456789123456789123456789"));
  }
}
