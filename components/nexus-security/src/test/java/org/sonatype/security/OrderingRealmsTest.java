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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.security.usermanagement.User;

import junit.framework.Assert;

public class OrderingRealmsTest
    extends AbstractSecurityTest
{

  public void testOrderedGetUser()
      throws Exception
  {

    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    List<String> realmHints = new ArrayList<String>();
    realmHints.add("MockRealmA");
    realmHints.add("MockRealmB");
    securitySystem.setRealms(realmHints);

    User jcoder = securitySystem.getUser("jcoder");
    Assert.assertNotNull(jcoder);

    // make sure jcoder is from MockUserManagerA
    Assert.assertEquals("MockUserManagerA", jcoder.getSource());

    // now change the order
    realmHints.clear();
    realmHints.add("MockRealmB");
    realmHints.add("MockRealmA");
    securitySystem.setRealms(realmHints);

    jcoder = securitySystem.getUser("jcoder");
    Assert.assertNotNull(jcoder);

    // make sure jcoder is from MockUserManagerA
    Assert.assertEquals("MockUserManagerB", jcoder.getSource());

  }

}
