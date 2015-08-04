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

import java.util.List;

import org.sonatype.security.SecuritySystem;

import junit.framework.Assert;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.CachingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;

public class WebRealmSecurityTest
    extends AbstractWebSecurityTest
{

  public void testCacheManagerInit()
      throws Exception
  {
    // Start up security
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);
    RealmSecurityManager plexusSecurityManager = this.lookup(RealmSecurityManager.class, "default");

    List<String> realms = securitySystem.getRealms();
    realms.clear();
    realms.add(SimpleAccountRealm.class.getName());
    securitySystem.setRealms(realms);

    // now if we grab one of the realms from the Realm locator, it should have its cache set
    CachingRealm cRealm1 = (CachingRealm) plexusSecurityManager.getRealms().iterator().next();
    Assert.assertNotNull("Realm has null cacheManager", cRealm1.getCacheManager());

    // // so far so good, the cacheManager should be set on all the child realms, but what if we add one after the
    // init method?
    realms.add(SimpleAccountRealm.class.getName());
    securitySystem.setRealms(realms);

    // this list should have exactly 2 elements
    Assert.assertEquals(2, plexusSecurityManager.getRealms().size());

    for (Realm realm : plexusSecurityManager.getRealms()) {
      Assert.assertNotNull("Realm has null cacheManager", ((CachingRealm) realm).getCacheManager());
    }
  }

}
