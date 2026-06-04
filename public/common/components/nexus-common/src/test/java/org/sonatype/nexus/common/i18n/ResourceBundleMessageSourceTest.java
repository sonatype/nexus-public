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
package org.sonatype.nexus.common.i18n;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ResourceBundleMessageSource}.
 */
public class ResourceBundleMessageSourceTest
{
  private MessageSource messages;

  @Before
  public void setUp() {
    messages = new ResourceBundleMessageSource(getClass());
  }

  @Test
  public void testLoadAndGetMessage() {
    String a = messages.getMessage("a");
    assertEquals("1", a);

    String b = messages.getMessage("b");
    assertEquals("2", b);

    String c = messages.getMessage("c");
    assertEquals("3", c);

    String f = messages.format("f", a, b, c);
    assertEquals("1 2 3", f);
  }

  @Test
  public void testMissingResource() throws Exception {
    try {
      messages.getMessage("no-such-code");
    }
    catch (ResourceNotFoundException e) {
      // ignore
    }
  }

  @Test
  public void testMissingResourceWithDefault() throws Exception {
    String msg = messages.getMessage("no-such-code", "foo");
    assertEquals("foo", msg);
  }
}
