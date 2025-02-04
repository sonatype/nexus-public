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
package org.sonatype.nexus.repository.httpbridge.internal.describe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DescribeType}.
 */
public class DescribeTypeTest
{

  @Test
  public void parseHtml() {
    assertEquals(DescribeType.HTML, DescribeType.parse("html"));
  }

  @Test
  public void parseJson() {
    assertEquals(DescribeType.JSON, DescribeType.parse("json"));
  }

  @Test
  public void parseBlankAsHtml() {
    assertEquals(DescribeType.HTML, DescribeType.parse(""));
  }

  @Test
  public void parseTrueAsHtml() {
    assertEquals(DescribeType.HTML, DescribeType.parse("true"));
  }
}
