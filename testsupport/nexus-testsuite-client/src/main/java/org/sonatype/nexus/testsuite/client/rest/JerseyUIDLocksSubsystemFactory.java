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
package org.sonatype.nexus.testsuite.client.rest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.client.UIDLocks;
import org.sonatype.nexus.testsuite.client.internal.JerseyUIDLocks;

/**
 * Jersey based {@link UIDLocks} Nexus Client Subsystem factory.
 *
 * @since 2.2
 */
@Named
@Singleton
public class JerseyUIDLocksSubsystemFactory
    implements SubsystemFactory<UIDLocks, JerseyNexusClient>
{

  @Override
  public Condition availableWhen() {
    return NexusStatusConditions.any20AndLater();
  }

  @Override
  public Class<UIDLocks> getType() {
    return UIDLocks.class;
  }

  @Override
  public UIDLocks create(final JerseyNexusClient nexusClient) {
    return new JerseyUIDLocks(nexusClient);
  }

}
