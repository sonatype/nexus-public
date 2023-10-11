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
package org.sonatype.nexus.common.oss.circuit;

import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;

import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.FREE_TIER;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.HARD_LIMIT;
import static org.sonatype.nexus.common.oss.circuit.ContentUsageLevel.UNLIMITED;

@Named
@Singleton
public class ContentUsageCircuitBreaker
    extends ComponentSupport
{
  private static final String PRO_EDITION = "PRO";

  private static final String OSS_EDITION = "OSS";

  private final AtomicReference<ContentUsageLevel> usageLevel = new AtomicReference<>();

  private final ApplicationVersion applicationVersion;

  @Inject
  public ContentUsageCircuitBreaker(ApplicationVersion applicationVersion) {
    this.applicationVersion = applicationVersion;
    if (PRO_EDITION.equals(applicationVersion.getEdition())) {
      setUsageLevel(UNLIMITED);
    }
    else {
      setUsageLevel(FREE_TIER);
    }
  }

  public ContentUsageLevel getUsageLevel() {
    return usageLevel.get();
  }

  public void setUsageLevel(final ContentUsageLevel usageLevel) {
    if (PRO_EDITION.equals(applicationVersion.getEdition()) && usageLevel == UNLIMITED ||
        OSS_EDITION.equals(applicationVersion.getEdition()) && usageLevel != UNLIMITED) {
      log.debug("Setting content usage level as {}", usageLevel);
      this.usageLevel.set(usageLevel);
    }
    else {
      log.debug("Impossible to set {} level for {} edition", usageLevel, applicationVersion.getEdition());
    }
  }

  public boolean isClosed() {
    return usageLevel.get() != HARD_LIMIT;
  }
}
