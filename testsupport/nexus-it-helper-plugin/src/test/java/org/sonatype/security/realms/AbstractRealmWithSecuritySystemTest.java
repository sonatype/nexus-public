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
package org.sonatype.security.realms;

import java.io.File;

import org.sonatype.nexus.configuration.application.DefaultNexusConfiguration;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.guice.SecurityModule;

import com.google.inject.Module;
import net.sf.ehcache.CacheManager;
import org.codehaus.plexus.context.Context;

/**
 * Abstract class for "realm" related tests that uses EHCache, and that does not bring up Nexus component only Security
 * subsystem. Without Nexus (or better {@link DefaultNexusConfiguration} brought up, we have to manually manage EHCache
 * manager component, and cleanly shut it down between tests as EHCache 2.5+ yells without it (violates the
 * "one named manager per JVM").
 *
 * @author cstamas
 */
public abstract class AbstractRealmWithSecuritySystemTest
    extends AbstractRealmTest
{
  private SecuritySystem securitySystem;

  private CacheManager cacheManager;

  @Override
  protected void customizeContext(final Context ctx) {
    super.customizeContext(ctx);
    ctx.put("application-conf", getConfDir().getAbsolutePath());
    ctx.put("security-xml-file", getConfDir().getAbsolutePath() + "/security.xml");
  }

  @Override
  protected Module[] getTestCustomModules() {
    return new Module[]{new SecurityModule()};
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    securitySystem = lookup(SecuritySystem.class);
    cacheManager = lookup(CacheManager.class);
  }

  protected void tearDown()
      throws Exception
  {
    if (securitySystem != null) {
      securitySystem.stop();
    }
    if (cacheManager != null) {
      cacheManager.shutdown();
    }
    super.tearDown();
  }

  protected abstract File getConfDir();

}
