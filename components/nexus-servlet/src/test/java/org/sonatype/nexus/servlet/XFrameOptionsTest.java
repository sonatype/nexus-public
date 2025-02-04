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
package org.sonatype.nexus.servlet;

import java.util.Arrays;
import java.util.Collection;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @since 3.11
 */
@RunWith(Parameterized.class)
public class XFrameOptionsTest
    extends TestSupport
{
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{"/foo/", true, "DENY"}, {"/foo/", false, "SAMEORIGIN"},
        {"/swagger-ui/", true, "SAMEORIGIN"}, {"/COPYRIGHT.html", true, "SAMEORIGIN"},
        {"/OC-LICENSE.html", true, "SAMEORIGIN"}, {"/PRO-LICENSE.html", true, "SAMEORIGIN"},
        {"/static/healthcheck-tos.html", true, "SAMEORIGIN"}});
  }

  @Parameter
  public String url;

  @Parameter(1)
  public boolean deny;

  @Parameter(2)
  public String expected;

  @Test
  public void testGetValueForPath() {
    XFrameOptions xFrameOptions = new XFrameOptions(deny);
    assertThat(xFrameOptions.getValueForPath(url), is(expected));
  }
}
