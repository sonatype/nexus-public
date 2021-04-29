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
package org.sonatype.nexus.internal.orient;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class OrientDeploymentAccessImplTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private NodeAccess nodeAccess;

  private OrientDeploymentAccessImpl service;

  @Before
  public void setup() throws Exception {
    when(nodeAccess.getId()).thenReturn("test-id");
    service = new OrientDeploymentAccessImpl(database.getInstanceProvider(), nodeAccess);
    service.start();
  }

  @Test
  public void testInitialState() {
    assertThat(service.getId(), is("test-id"));
    assertThat(service.getAlias(), nullValue());
  }

  @Test
  public void testStart_isIdempotent() throws Exception {
    String before = service.getId();

    service.start();

    assertThat(before, is(service.getId()));
  }

  @Test
  public void testSetAlias() {
    assertThat(service.getAlias(), nullValue());

    service.setAlias("some-new-alias");

    assertThat(service.getId(), not(nullValue()));
    assertThat(service.getAlias(), is("some-new-alias"));

    // accept null values
    service.setAlias(null);

    assertThat(service.getAlias(), nullValue());
  }
}
