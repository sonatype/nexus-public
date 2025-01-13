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
package org.sonatype.nexus.repository.raw;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * {@link RawCoordinatesHelper} tests.
 */
@RunWith(Parameterized.class)
public class RawCoordinatesHelperTest
{

  @Parameterized.Parameter(0)
  public String path;

  @Parameterized.Parameter(1)
  public String expectedGroup;

  @Parameterized.Parameters(name = "group of {0} is {1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"/foo/bar", "/foo"},
        {"foo/bar", "/foo"},
        {"foobar.txt", "/"},
        {"/foobar.txt", "/"},
        {"/some/long/involved/path.txt", "/some/long/involved"},
        {"some/long/involved/path.txt", "/some/long/involved"}
    });
  }

  @Test
  public void testGetGroup() {
    String group = RawCoordinatesHelper.getGroup(path);
    assertEquals(expectedGroup, group);
  }
}
