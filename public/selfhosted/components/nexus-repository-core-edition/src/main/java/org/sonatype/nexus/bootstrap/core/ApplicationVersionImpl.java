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
package org.sonatype.nexus.bootstrap.core;

import org.sonatype.nexus.bootstrap.entrypoint.edition.core.CoreNexusEdition;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.ApplicationVersionSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CORE {@link ApplicationVersion}.
 */
@Component
@ConditionalOnProperty(value = "nexus.edition", havingValue = "CORE", matchIfMissing = true)
public class ApplicationVersionImpl
    extends ApplicationVersionSupport
{
  private final CoreNexusEdition coreNexusEdition;

  @Autowired
  public ApplicationVersionImpl(final CoreNexusEdition coreNexusEdition) {
    this.coreNexusEdition = coreNexusEdition;
  }

  @Override
  public String getEdition() {
    return coreNexusEdition.getShortName();
  }
}
