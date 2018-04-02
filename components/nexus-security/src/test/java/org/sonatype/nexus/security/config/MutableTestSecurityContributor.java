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

public class MutableTestSecurityContributor
    extends MutableSecurityContributor
{
  private boolean configRequested = false;

  private static int INSTANCE_COUNT = 1;

  private String privId = "priv-" + INSTANCE_COUNT++;

  @Override
  protected void initial(final SecurityConfiguration model) {
    model.addPrivilege(WildcardPrivilegeDescriptor.privilege("foo:bar:" + privId + ":read"));
  }

  public String getId() {
    return privId;
  }

  public void setConfigRequested(boolean configRequested) {
    this.configRequested = configRequested;
  }

  public boolean wasConfigRequested() {
    return configRequested;
  }

  @Override
  public SecurityConfiguration getContribution() {
    setConfigRequested(true);
    return super.getContribution();
  }

  public void setDirty(boolean dirty) {
    if (dirty) {
      setConfigRequested(false);
      apply((model, configurationManager) -> {
        // marks model as dirty
      });
    }
  }
}
