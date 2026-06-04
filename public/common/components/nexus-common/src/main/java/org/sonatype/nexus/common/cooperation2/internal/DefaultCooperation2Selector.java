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
package org.sonatype.nexus.common.cooperation2.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.Cooperation2Selector;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Cooperation2Selector implementation that simply returns the default impl from the dependency injector.
 */
@Primary
@Component
public class DefaultCooperation2Selector
    implements Cooperation2Selector
{
  private Cooperation2Factory cooperation2Factory;

  @Autowired
  public DefaultCooperation2Selector(final Cooperation2Factory cooperation2Factory) {
    this.cooperation2Factory = cooperation2Factory;
  }

  @Override
  public Cooperation2Factory select() {
    return cooperation2Factory;
  }
}
