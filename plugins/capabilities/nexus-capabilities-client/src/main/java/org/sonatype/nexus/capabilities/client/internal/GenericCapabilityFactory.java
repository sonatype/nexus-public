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
package org.sonatype.nexus.capabilities.client.internal;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.spi.CapabilityClient;
import org.sonatype.nexus.capabilities.client.spi.CapabilityFactory;
import org.sonatype.nexus.capabilities.client.support.CapabilityImpl;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link CapabilityFactory} that allows working with capabilities in generic ways. Used when no capability
 * type
 * specific factory is available.
 *
 * @since capabilities 2.2
 */
public class GenericCapabilityFactory
    implements CapabilityFactory<Capability>
{

  private final String type;

  public GenericCapabilityFactory(final String type) {
    this.type = checkNotNull(type);
  }

  @Override
  public Capability create(final CapabilityClient client) {
    return new CapabilityImpl(client, type);
  }

  @Override
  public Capability create(final CapabilityClient client, final CapabilityStatusXO settings) {
    return new CapabilityImpl(client, settings);
  }

  @Override
  public boolean canCreate(final String type) {
    return true;
  }

  @Override
  public boolean canCreate(final Class<Capability> type) {
    return false;
  }

}
