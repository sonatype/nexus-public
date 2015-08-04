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
package org.sonatype.nexus.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.security.filter.authc.NexusHttpAuthenticationFilter;
import org.sonatype.security.SecuritySystem;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.DelegatingSession;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * There is a problem either with Shiro (or how we are using it) that effects logging and logging out when using a
 * DelegatingSession (Nexus).</BR>
 * On logout the actual session is expired (see nexus-4378), but the DelegatingSession is not handling this.  Same for
 * login (NEXUS-4257), if the RUNAS user attribute is set to a session that is expired, the user will NOT be able to
 * login.</BR>
 * </BR>
 * NOTE: I don't know why the session has the RUNAS attribute set, we do not use this in nexus.
 */
public class NexusHttpAuthenticationFilterTest
{
  private DelegatingSubject subject;

  private EnterpriseCacheSessionDAO sessionDAO;

  private SimpleSession simpleSession;

  private HttpServletRequest request;

  private HttpServletResponse response;

  private SecuritySystem securitySystem;

  @Before
  public void bindSubjectToThread() {
    // setup a simple realm for authc
    SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();
    simpleAccountRealm.addAccount("anonymous", "anonymous");
    DefaultSecurityManager securityManager = new DefaultSecurityManager();
    securityManager.setRealm(simpleAccountRealm);

    SecurityUtils.setSecurityManager(securityManager);

    DefaultSessionManager sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
    sessionDAO = new EnterpriseCacheSessionDAO();
    sessionManager.setSessionDAO(sessionDAO);

    simpleSession = new SimpleSession();
    sessionDAO.create(simpleSession);

    List<PrincipalCollection> principalCollectionList = new ArrayList<PrincipalCollection>();
    principalCollectionList.add(new SimplePrincipalCollection("other Principal", "some-realm"));

    simpleSession.setAttribute(DelegatingSubject.class.getName() + ".RUN_AS_PRINCIPALS_SESSION_KEY",
        principalCollectionList);

    DelegatingSession delegatingSession =
        new DelegatingSession(sessionManager, new DefaultSessionKey(simpleSession.getId()));

    // set the user

    subject = new DelegatingSubject(new SimplePrincipalCollection("anonymous", "realmName"), true, null,
        delegatingSession, securityManager);
    ThreadContext.bind(subject);
  }

  @Before
  public void setupMockResponseAndRequest() {
    // setup the MOCK
    request = mock(HttpServletRequest.class);
    when(request.getAttribute(eq(NexusHttpAuthenticationFilter.ANONYMOUS_LOGIN))).thenReturn("true");
    when(request.getAttribute(eq(org.sonatype.nexus.web.Constants.ATTR_KEY_REQUEST_IS_AUTHZ_REJECTED))).thenReturn(null);
    // end fun with mocks

    response = mock(HttpServletResponse.class);
  }

  @Before
  public void setupSecuritySystem() {
    securitySystem = mock(SecuritySystem.class);
    when(securitySystem.getAnonymousUsername()).thenReturn("anonymous");
    when(securitySystem.getAnonymousPassword()).thenReturn("anonymous");
  }

  @After
  public void unbindSubjectFromThread() {
    ThreadContext.remove();
  }

  /**
   * Test that executeAnonymousLogin will attempt to recover after an UnknownSessionException is thrown.
   */
  @Test
  public void testExecuteAnonymousLoginForAnonUserWithInvalidSession()
      throws Exception
  {
    // ******
    // Delete the session directly to mimic what I think is the cause of the Unknown SessionException
    // ******
    sessionDAO.delete(simpleSession);

    // Verify this does not throw an exception when the session is expired
    assertThat("Anonymous user was not logged in after UnknownSessionException", callExecuteAnonymousLogin());
  }

  /**
   * Test the typical anonymous login path.  executeAnonymousLogin should return true.
   */
  @Test
  public void testExecuteAnonymousLoginHappyPath()
      throws Exception
  {
    // Verify this does not throw an exception when the session is expired
    assertThat("Anonymous user should have been logged in", callExecuteAnonymousLogin());
  }

  /**
   * Calls a protected the method 'executeAnonymousLogin' in NexusHttpAuthenticationFilter, and returns the result.
   */
  private boolean callExecuteAnonymousLogin() {
    return new NexusHttpAuthenticationFilter()
    {
      // expose protected method
      @Override
      public boolean executeAnonymousLogin(ServletRequest request, ServletResponse response) {
        return super.executeAnonymousLogin(request, response);
      }

      @Override
      protected SecuritySystem getSecuritySystem() {
        return securitySystem;
      }
    }.executeAnonymousLogin(request, response);
    // what a hack... just to call a protected method
  }


}
