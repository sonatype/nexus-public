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
package org.sonatype.nexus.proxy;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link RequestContext}.
 */
public class RequestContextTest
    extends TestSupport
{
  @Test
  public void testNullParent() {
    RequestContext requestContext = new RequestContext(null);
    Assert.assertNull(requestContext.getParentContext());

    requestContext.setParentContext(null);
    Assert.assertNull(requestContext.getParentContext());
  }

  @Test
  public void testValidParent() {
    RequestContext parentContext = new RequestContext(null);
    RequestContext requestContext = new RequestContext(parentContext);
    Assert.assertEquals(parentContext, requestContext.getParentContext());

    requestContext.setParentContext(null);
    Assert.assertNull(requestContext.getParentContext());

    requestContext = new RequestContext();
    Assert.assertNull(requestContext.getParentContext());
    requestContext.setParentContext(parentContext);
    Assert.assertEquals(parentContext, requestContext.getParentContext());
  }

  @Test
  public void testSelfParent() {
    RequestContext requestContext = new RequestContext();
    Assert.assertNull(requestContext.getParentContext());

    try {
      requestContext.setParentContext(requestContext);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }

    Assert.assertNull(requestContext.getParentContext());
  }

  // 3-->2-->1-->3
  @Test
  public void testSelfAncestor() {
    RequestContext requestContext1 = new RequestContext();
    Assert.assertNull(requestContext1.getParentContext());
    RequestContext requestContext2 = new RequestContext(requestContext1);
    Assert.assertEquals(requestContext1, requestContext2.getParentContext());
    RequestContext requestContext3 = new RequestContext(requestContext2);
    Assert.assertEquals(requestContext2, requestContext3.getParentContext());

    try {
      requestContext1.setParentContext(requestContext3);
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException expected) {
    }

    Assert.assertNull(requestContext1.getParentContext());
  }
}
