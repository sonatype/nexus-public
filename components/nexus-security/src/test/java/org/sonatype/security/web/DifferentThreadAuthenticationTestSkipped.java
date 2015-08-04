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
package org.sonatype.security.web;

import org.sonatype.security.SecuritySystem;

import junit.framework.Assert;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.eclipse.sisu.launch.InjectedTestCase;

public class DifferentThreadAuthenticationTestSkipped
    extends AbstractWebSecurityTest
{

  public void testGetSubjectFromThread()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    securitySystem.start();

    // need to bind to a request
    // this.setupLoginContext( "testGetSubjectFromThread" );

    Assert.assertNotNull(securitySystem.login(new UsernamePasswordToken("jcoder", "jcoder")));

    // WebUtils.unbindServletRequest();
    // WebUtils.unbindServletResponse();
    //
    // now with the thread
    SubjectRetrievingThread thread = new SubjectRetrievingThread(this);

    thread.setContextClassLoader(null);
    thread.start();
    thread.join(500);
    Assert.assertNotNull(thread.getSubject());
    Subject subject = thread.getSubject();
    Assert.assertTrue(subject.hasRole("RoleA"));

    // if we login again with the jcoder user we should need to bind the request again
    try {
      securitySystem.login(new UsernamePasswordToken("jcoder", "jcoder"));
      Assert.fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
      // this is not a great exception to catch...
      // but we check the success on the next call
    }

    this.setupLoginContext("testGetSubjectFromThread-again");
    subject = securitySystem.login(new UsernamePasswordToken("jcoder", "jcoder"));
    Assert.assertNotNull(subject);

  }

  class SubjectRetrievingThread
      extends Thread
  {
    private InjectedTestCase testCase;

    // private Subject subject;
    private SecuritySystem securitySystem;

    private SubjectRetrievingThread(InjectedTestCase testCase) {
      this.testCase = testCase;
      this.securitySystem = testCase.lookup(SecuritySystem.class);
    }

    @Override
    public void run() {
      // FIXME: add this back in
      // this.securitySystem.runAs( new SimplePrincipalCollection("jcoder", "") );
    }

    public Subject getSubject() {
      return this.securitySystem.getSubject();
    }
  }
}
