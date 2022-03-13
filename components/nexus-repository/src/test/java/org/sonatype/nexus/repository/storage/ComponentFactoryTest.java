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
package org.sonatype.nexus.repository.storage;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComponentFactoryTest
    extends TestSupport
{
  @Mock
  private ComponentDecorator componentDecorator;

  private ComponentFactory underTest;

  @Before
  public void setup() {
    underTest = new ComponentFactory(ImmutableSet.of(componentDecorator));

    Component component = new DefaultComponent();
    when(componentDecorator.decorate(any(Component.class))).thenReturn(new TestComponent(component));
  }

  @Test
  public void testComponent() {
    Component component = underTest.createComponent();
    assertThat(component, notNullValue());
    assertThat(component, instanceOf(TestComponent.class));
    verify(componentDecorator).decorate(any(Component.class));
    TestComponent testComponent = (TestComponent) component;
    assertThat(testComponent.getWrappedObject(), instanceOf(DefaultComponent.class));
  }

  private class TestComponent
      extends DecoratedComponent
      implements Component
  {
    TestComponent(final Component component) {
      super(component);
    }
  }
}
