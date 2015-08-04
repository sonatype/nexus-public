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
package org.sonatype.nexus.yum.client.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.condition.NexusStatusConditions;
import org.sonatype.nexus.client.core.spi.SubsystemFactory;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.client.rest.jersey.subsystem.JerseyRepositoriesFactory;
import org.sonatype.nexus.yum.client.Repodata;
import org.sonatype.nexus.yum.client.internal.JerseyRepodata;

/**
 * @since yum 3.0
 */
@Named
@Singleton
public class JerseyRepodataSubsystemFactory
    implements SubsystemFactory<Repodata, JerseyNexusClient>
{

  @Inject
  private JerseyRepositoriesFactory repositoriesFactory;

  @Override
  public Condition availableWhen() {
    return NexusStatusConditions.anyModern();
  }

  @Override
  public Class<Repodata> getType() {
    return Repodata.class;
  }

  @Override
  public Repodata create(JerseyNexusClient nexusClient) {
    return new JerseyRepodata(nexusClient, repositoriesFactory.create(nexusClient));
  }

}
