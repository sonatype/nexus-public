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
package org.sonatype.security.configuration;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.sonatype.sisu.litmus.testsupport.TestUtil;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.eclipse.sisu.space.BeanScanning;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class SecurityConfigurationManagerTest
    extends InjectedTestCase
{
  private final TestUtil util = new TestUtil(this);

  // FIXME: Upgrade to junit4

  private File PLEXUS_HOME = util.createTempDir("plexus-home");

  private File APP_CONF = new File(PLEXUS_HOME, "conf");

  @Override
  public void configure(Properties properties) {
    properties.put("application-conf", APP_CONF.getAbsolutePath());
    super.configure(properties);
  }

  @Override
  public BeanScanning scanning() {
    return BeanScanning.INDEX;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    // delete the plexus home dir
    FileUtils.deleteDirectory(PLEXUS_HOME);

    super.setUp();
  }

  //@Test
  public void testLoadEmptyDefaults()
      throws Exception
  {
    SecurityConfigurationManager config = this.lookup(SecurityConfigurationManager.class);

    Assert.assertNotNull(config);

    Assert.assertEquals("anonymous-pass", config.getAnonymousPassword());
    Assert.assertEquals("anonymous-user", config.getAnonymousUsername());

    Assert.assertEquals(false, config.isAnonymousAccessEnabled());

    List<String> realms = config.getRealms();
    assertThat(realms, containsInAnyOrder(
        "MockRealmA", "MockRealmB", "ExceptionThrowingMockRealm", "FakeRealm1", "FakeRealm2"));
  }

  //@Test
  public void testWrite()
      throws Exception
  {
    SecurityConfigurationManager config = this.lookup(SecurityConfigurationManager.class);

    config.setAnonymousAccessEnabled(true);
    config.setAnonymousPassword("new-pass");
    config.setAnonymousUsername("new-user");

    List<String> realms = Collections.singletonList("FakeRealm1");
    config.setRealms(realms);

    config.save();

    config.clearCache();

    Assert.assertEquals("new-pass", config.getAnonymousPassword());
    Assert.assertEquals("new-user", config.getAnonymousUsername());

    Assert.assertEquals(true, config.isAnonymousAccessEnabled());

    realms = config.getRealms();
    Assert.assertEquals(1, realms.size());
    Assert.assertEquals("FakeRealm1", realms.get(0));
  }
}
