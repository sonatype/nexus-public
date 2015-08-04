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
package org.sonatype.security;

import org.sonatype.security.email.SecurityEmailer;
import org.sonatype.security.mock.MockEmailer;
import org.sonatype.security.usermanagement.UserNotFoundException;

import com.google.inject.Binder;
import junit.framework.Assert;

public class EmailerTest
    extends AbstractSecurityTest
{
  private MockEmailer emailer = new MockEmailer();

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(SecurityEmailer.class).toInstance(emailer);
  }

  public void testForgotUsername()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    securitySystem.forgotUsername("cdugas@sonatype.org");

    Assert.assertTrue(((MockEmailer) emailer).getForgotUserIds().contains("cdugas"));
    Assert.assertEquals(1, ((MockEmailer) emailer).getForgotUserIds().size());
  }

  public void testDoNotRecoverAnonUserName()
      throws Exception
  {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    try {
      securitySystem.forgotUsername("anonymous@sonatype.org");
      Assert.fail("UserNotFoundException expected");
    }
    catch (UserNotFoundException e) {
      // expected
    }

  }

}
