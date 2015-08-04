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
package org.sonatype.nexus.testsuite.capabilities.client.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.spi.CapabilityClient;
import org.sonatype.nexus.capabilities.client.spi.CapabilityFactory;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.testsuite.capabilities.client.CapabilityA;

/**
 * @since 2.2
 */
@Named
@Singleton
public class JerseyCapabilityAFactory
    implements CapabilityFactory<CapabilityA>
{

  public CapabilityA create(final CapabilityClient client) {
    return new JerseyCapabilityA(client);
  }

  @Override
  public CapabilityA create(final CapabilityClient client, final CapabilityStatusXO settings) {
    return new JerseyCapabilityA(client, settings);
  }

  public boolean canCreate(final String type) {
    return "[a]".equals(type);
  }

  public boolean canCreate(final Class<Capability> type) {
    return CapabilityA.class.equals(type);
  }

}
