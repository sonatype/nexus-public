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
package org.sonatype.nexus.security.realm;

import java.util.List;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.AbstractSecurityTest.BaseSecurityConfiguration;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(BaseSecurityConfiguration.class)
public class OrderingRealmsTest
    extends AbstractSecurityTest
{
  @Test
  void testOrderedGetUser() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    RealmManager realmManager = lookup(RealmManager.class);
    realmManager.setConfiguredRealmIds(List.of("MockRealmA", "MockRealmB"));

    User jcoder = securitySystem.getUser("jcoder");
    assertNotNull(jcoder);

    // make sure jcoder is from MockUserManagerA
    assertEquals("MockUserManagerA", jcoder.getSource());

    // now change the order
    realmManager.setConfiguredRealmIds(List.of("MockRealmB", "MockRealmA"));

    jcoder = securitySystem.getUser("jcoder");
    assertNotNull(jcoder);

    // make sure jcoder is from MockUserManagerA
    assertEquals("MockUserManagerB", jcoder.getSource());
  }
}
