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
package org.sonatype.nexus.internal.node;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DeploymentIdStoreImplTest
    extends TestSupport
{
  private static final String DEPLOYMENT_ID = "ipuaeycw934r";

  private static final String DEPLOYMENT_ID_NEW = "ipuaeycw934re4rgt0f34r";

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(DeploymentIdDAO.class);

  @Mock
  private EventManager eventManager;

  private DeploymentIdStoreImpl underTest;

  @Before
  public void setup() {
    underTest = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(DeploymentIdStoreImpl.class);
  }

  @Test
  public void testGet_must_be_empty() {
    Optional<String> deploymentId = underTest.get();

    assertThat(deploymentId.isPresent(), is(false));
  }

  @Test
  public void testGet_must_contain_deployment_id() {
    underTest.set(DEPLOYMENT_ID);

    Optional<String> deploymentId = underTest.get();

    assertThat(deploymentId.isPresent(), is(true));
    assertThat(deploymentId.get(), is(DEPLOYMENT_ID));
  }

  @Test
  public void testGet_must_contain_new_deployment_id() {
    underTest.set(DEPLOYMENT_ID);

    Optional<String> deploymentId = underTest.get();

    assertThat(deploymentId.isPresent(), is(true));
    assertThat(deploymentId.get(), is(DEPLOYMENT_ID));

    underTest.set(DEPLOYMENT_ID_NEW);

    deploymentId = underTest.get();

    assertThat(deploymentId.isPresent(), is(true));
    assertThat(deploymentId.get(), is(DEPLOYMENT_ID_NEW));
  }
}
