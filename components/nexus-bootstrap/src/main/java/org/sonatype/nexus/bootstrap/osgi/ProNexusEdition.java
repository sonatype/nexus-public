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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProNexusEdition
    extends NexusEdition
{
  private static final Logger log = LoggerFactory.getLogger(ProNexusEdition.class);

  @Override
  public NexusEditionType getEdition() {
    return NexusEditionType.PRO;
  }

  @Override
  public NexusEditionFeature getEditionFeature() {
    return NexusEditionFeature.PRO_FEATURE;
  }

  @Override
  protected boolean doesApply(final Properties properties, final Path workDirPath) {
    return !properties.getProperty(NEXUS_FEATURES, "").contains(NexusEditionFeature.OSS_FEATURE.featureString) &&
        !properties.getProperty(NEXUS_FEATURES, "").contains(NexusEditionFeature.PRO_STARTER_FEATURE.featureString) &&
        !shouldSwitchToOss(workDirPath);
  }

  @Override
  protected void doApply(final Properties properties, final Path workDirPath) {
    log.info("Loading Pro Edition");
    createEditionMarker(workDirPath, getEdition());
  }

  @Override
  protected boolean shouldSwitchToOss(final Path workDirPath) {
    File proEditionMarker = getEditionMarker(workDirPath, NexusEditionType.PRO);
    boolean switchToOss;
    if (hasNexusLoadAs(NEXUS_LOAD_AS_OSS_PROP_NAME)) {
      switchToOss = isNexusLoadAs(NEXUS_LOAD_AS_OSS_PROP_NAME);
    } else
    if (hasNexusLoadAs(NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME)) {
      switchToOss = isNexusLoadAs(NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME);
    }
    else if (proEditionMarker.exists()) {
      switchToOss = false;
    }
    else {
      switchToOss = isNullNexusLicenseFile() && isNullJavaPrefLicensePath(PRO_LICENSE_LOCATION);
    }
    return switchToOss;
  }
}
