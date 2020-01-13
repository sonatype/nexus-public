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

package org.sonatype.nexus.internal.orient

import org.sonatype.nexus.common.node.NodeAccess
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import static org.mockito.Mockito.when

/**
 * Unit tests for {@link DeploymentAccessImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
class OrientDeploymentAccessImplTest
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory('test')

  @Mock
  NodeAccess nodeAccess

  private OrientDeploymentAccessImpl service

  @Before
  void setup() {
    when(nodeAccess.getId()).thenReturn('test-id')
    service = new OrientDeploymentAccessImpl(database.instanceProvider, nodeAccess)
    service.start()
  }

  @Test
  void 'confirm id is present and alias is null in initial state'() {
    assert service.getId() == 'test-id'
    assert service.getAlias() == null
  }

  @Test
  void 'start is idempotent'() {
    def before = service.getId()

    service.start()

    assert before == service.getId()
  }

  @Test
  void 'setAlias is successful'() {
    assert service.getAlias() == null

    service.setAlias('some-new-alias')

    assert service.getId() != null
    assert service.getAlias() == 'some-new-alias'

    // accept null values
    service.setAlias(null)

    assert service.getAlias() == null

  }
}
