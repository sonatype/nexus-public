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
package org.sonatype.nexus.security.config;

import org.sonatype.nexus.security.privilege.WildcardPrivilegeDescriptor;

public class UnitTestDynamicSecurityConfigurationResource
    extends AbstractDynamicSecurityConfigurationResource
{
  private boolean configCalledAfterSetDirty = false;

  private static int INSTANCE_COUNT = 1;

  private String privId = "priv-" + INSTANCE_COUNT++;

  public String getId() {
    return privId;
  }

  @Override
  protected SecurityConfiguration doGetConfiguration() {
    configCalledAfterSetDirty = true;

    setConfigCalledAfterSetDirty(true);
    MemorySecurityConfiguration config = new MemorySecurityConfiguration();

    CPrivilege priv = WildcardPrivilegeDescriptor.privilege("foo:bar:" + privId + ":read");
    config.addPrivilege(priv);

    return config;
  }

  public void setConfigCalledAfterSetDirty(boolean configCalledAfterSetDirty) {
    this.configCalledAfterSetDirty = configCalledAfterSetDirty;
  }

  public boolean isConfigCalledAfterSetDirty() {
    return configCalledAfterSetDirty;
  }

  @Override
  protected void setDirty(boolean dirty) {
    if (dirty) {
      this.configCalledAfterSetDirty = false;
    }
    super.setDirty(dirty);
  }
}
