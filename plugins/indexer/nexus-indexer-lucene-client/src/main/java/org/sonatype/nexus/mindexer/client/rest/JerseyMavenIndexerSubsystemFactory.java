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
package org.sonatype.nexus.mindexer.client.rest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.mindexer.client.MavenIndexer;
import org.sonatype.nexus.mindexer.client.internal.JerseyMavenIndexer;

@Named
@Singleton
public class JerseyMavenIndexerSubsystemFactory
    implements SubsystemFactory<MavenIndexer, JerseyNexusClient>
{

  @Override
  public Condition availableWhen() {
    return NexusStatusConditions.anyModern();
  }

  @Override
  public Class<MavenIndexer> getType() {
    return MavenIndexer.class;
  }

  @Override
  public MavenIndexer create(final JerseyNexusClient nexusClient) {
    return new JerseyMavenIndexer(nexusClient);
  }
}
