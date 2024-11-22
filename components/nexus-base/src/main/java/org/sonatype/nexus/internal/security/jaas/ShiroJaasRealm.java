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
package org.sonatype.nexus.internal.security.jaas;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.AppConfigurationEntry;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.eclipse.sisu.EagerSingleton;
import org.osgi.framework.BundleContext;

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
import static org.apache.karaf.jaas.boot.ProxyLoginModule.PROPERTY_BUNDLE;
import static org.apache.karaf.jaas.boot.ProxyLoginModule.PROPERTY_MODULE;

/**
 * {@link JaasRealm} that configures {@link ShiroLoginModule} for use with Karaf.
 *
 * @since 3.0
 */
@Named
@EagerSingleton
public class ShiroJaasRealm
    extends ComponentSupport
    implements JaasRealm
{
  private final AppConfigurationEntry[] entries;

  @Inject
  public ShiroJaasRealm(
      final BundleContext bundleContext,
      final SecurityHelper securityHelper,
      final SecuritySystem securitySystem)
  {
    Map<String, Object> options = new HashMap<>();

    options.put(BundleContext.class.getName(), bundleContext);
    options.put(SecurityHelper.class.getName(), securityHelper);
    options.put(SecuritySystem.class.getName(), securitySystem);

    options.put(PROPERTY_MODULE, ShiroLoginModule.class.getName());
    options.put(PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));

    entries = new AppConfigurationEntry[]{
        new AppConfigurationEntry(ProxyLoginModule.class.getName(), SUFFICIENT, options)
    };

    bundleContext.registerService(JaasRealm.class, this, null);
  }

  @Override
  public String getName() {
    return "shiro";
  }

  @Override
  public int getRank() {
    return 0;
  }

  @Override
  public AppConfigurationEntry[] getEntries() {
    return entries.clone();
  }
}
