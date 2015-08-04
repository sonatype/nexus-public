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
package org.sonatype.security.realms.kenai;

import java.util.Collections;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.security.SecuritySystem;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.junit.Test;

public class KenaiClearCacheTest
    extends AbstractKenaiRealmTest
{
  protected SecuritySystem securitySystem;

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    getKenaiRealmConfiguration();

    // to start the hobelevanc and make it use Kenai realm
    startNx();
    lookup(NexusConfiguration.class).setRealms(Collections.singletonList("kenai"));
    lookup(NexusConfiguration.class).saveConfiguration();

    securitySystem = lookup(SecuritySystem.class);
  }


  @Test
  public void testClearCache()
      throws Exception
  {
    // so here is the problem, we clear the authz cache when ever config changes happen

    // now log the user in
    Subject subject1 = securitySystem.login(new UsernamePasswordToken(username, password));
    // check authz
    subject1.checkRole(DEFAULT_ROLE);

    // clear the cache
    KenaiRealm realm = (KenaiRealm) this.lookup(Realm.class, "kenai");
    realm.getAuthorizationCache().clear();

    // user should still have the role
    subject1.checkRole(DEFAULT_ROLE);

    // the user should be able to login again as well
    Subject subject2 = securitySystem.login(new UsernamePasswordToken(username, password));
    subject2.checkRole(DEFAULT_ROLE);
  }
}
