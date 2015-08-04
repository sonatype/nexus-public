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
package org.sonatype.nexus.rest;

import org.sonatype.nexus.auth.ClientInfo;

import junit.framework.Assert;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class RestletClientInfoProviderTest
{
  private RestletClientInfoProvider testSubject;

  @Before
  public void setUp() {
    this.testSubject = new RestletClientInfoProvider();
  }

  protected void mockSecurity() {
    final Subject mock = Mockito.mock(Subject.class);
    Mockito.when(mock.getPrincipal()).thenReturn("superman");
    ThreadContext.bind(mock);
  }

  protected void unmockSecurity() {
    ThreadContext.unbindSubject();
  }

  /**
   * Without any Shiro config around, Shiro is not viable. Anyway, from Shiro perspective (and hence, any Shiro using
   * app's perspective), this is a fatal configuration error. See {@link UnavailableSecurityManagerException}
   * javadoc.
   * <p>
   * TEST IGNORED for now, as actually it does not make any sense to test "broken configuration" state of Shiro
   * itself. In those cases, Nexus would be broken too anyway. Note: when run in "solo" this test passes, but when
   * run
   * together with all other tests, there is configured Shiro present, and it fails.
   */
  @Test(expected = UnavailableSecurityManagerException.class)
  @Ignore
  public void withoutAnything() {
    Subject existingSubject = null;
    SecurityManager existingSecurityManager = null;
    try {
      existingSubject = ThreadContext.unbindSubject();
      existingSecurityManager = ThreadContext.unbindSecurityManager();
      final ClientInfo ci = testSubject.getCurrentThreadClientInfo();
      Assert.assertNull(ci);
    }
    finally {
      if (existingSubject != null) {
        ThreadContext.bind(existingSubject);
      }
      if (existingSecurityManager != null) {
        ThreadContext.bind(existingSecurityManager);
      }
    }
  }

  /**
   * Without Restlet request we expect null.
   */
  @Test
  public void withoutRestletRequest() {
    try {
      mockSecurity();
      final ClientInfo ci = testSubject.getCurrentThreadClientInfo();
      Assert.assertNull(ci);
    }
    finally {
      unmockSecurity();
    }
  }

  /**
   * Without Restlet request we expect null.
   */
  @Test
  public void withRestletRequest() {
    try {
      // mock security
      mockSecurity();
      // mock Restlet request
      final Request request = new Request(Method.GET, "http://repository.sonatype.org/foo/bar");
      request.getAttributes().put("org.restlet.http.headers", new Form()); // this happens in Restlet HTTP
      // connector
      request.getClientInfo().setAgent("SuperBrowser/1.0");
      request.getClientInfo().setAddress("Planet Crypton's IP address in IPv8"); // this is a free form string
      // actually
      Response.setCurrent(new Response(request));

      final ClientInfo ci = testSubject.getCurrentThreadClientInfo();
      Assert.assertNotNull(ci);
      Assert.assertEquals("superman", ci.getUserid());
      Assert.assertEquals("SuperBrowser/1.0", ci.getUserAgent());
      Assert.assertEquals("Planet Crypton's IP address in IPv8", ci.getRemoteIP());
    }
    finally {
      unmockSecurity();
      Response.setCurrent(null);
    }
  }
}
