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
package org.sonatype.nexus.repository.config.internal;

import java.util.Collection;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.config.Configuration;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigurationStoreImplTest
    extends TestSupport
{
  @Mock
  private DataSessionSupplier sessionSupplier;

  private ConfigurationStoreImpl underTest;

  @Before
  public void setup() {
    underTest = new ConfigurationStoreImpl(sessionSupplier);
  }

  @Test
  public void readByNamesShouldBeEmptyWhenRepositoriesIsEmpty() {
    Collection<Configuration> configurations = underTest.readByNames(emptySet());

    assertThat(configurations.isEmpty(), Is.is(true));
  }
}
