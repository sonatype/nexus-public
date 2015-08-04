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
package com.sonatype.nexus.oss;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.AbstractApplicationStatusSource;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;

import org.codehaus.plexus.util.StringUtils;

@Singleton
@Named
public class OSSApplicationStatusSource
    extends AbstractApplicationStatusSource
    implements ApplicationStatusSource
{
  private static final String FORMATTED_APP_NAME_BASE = "Sonatype Nexus&trade;";

  public OSSApplicationStatusSource() {
    super();
    getSystemStatusInternal().setVersion(discoverApplicationVersion());
    getSystemStatusInternal().setApiVersion(getSystemStatusInternal().getVersion());
    getSystemStatusInternal().setFormattedAppName(
        FORMATTED_APP_NAME_BASE + " "
            + (StringUtils.isEmpty(getSystemStatusInternal().getEditionLong())
            ? ""
            : getSystemStatusInternal().getEditionLong() + " Edition ")
            + getSystemStatusInternal().getVersion());
  }

  @Override
  protected void renewSystemStatus(SystemStatus systemStatus) {
    // nothing changes in OSS yet
  }

  @Override
  protected String discoverApplicationVersion() {
    return readVersion("/META-INF/maven/org.sonatype.nexus/nexus-oss-edition/pom.properties");
  }
}
