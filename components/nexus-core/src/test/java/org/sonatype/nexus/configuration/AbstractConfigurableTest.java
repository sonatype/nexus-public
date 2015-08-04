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
package org.sonatype.nexus.configuration;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link AbstractConfigurable}
 *
 * @since 2.2
 */
public class AbstractConfigurableTest
    extends TestSupport
{
  @Mock
  private EventBus eventBus;
  
  @Mock
  private ApplicationConfiguration applicationConfiguration;

  @Test
  public void isDirtyNullConfigShouldReturnFalse() {
    AbstractConfigurable configurable = new AbstractConfigurable(eventBus, applicationConfiguration)
    {
      @Override
      protected CoreConfiguration wrapConfiguration(final Object configuration)
          throws ConfigurationException
      {
        return null;
      }

      @Override
      public String getName() {
        return "name";
      }
    };

    assertThat(configurable.getCurrentCoreConfiguration(), nullValue());
    assertThat(configurable.isDirty(), is(false));
  }
}
