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
package org.sonatype.nexus.coreui;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import org.sonatype.goodies.testsupport.inject.InjectedTestSupport;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.inject.Binder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SelectorComponent}.
 */
public class SelectorComponentTest
    extends InjectedTestSupport
{
  @Inject
  private SelectorComponent component;

  private Path path = mock(Path.class);

  @Override
  public void configure(Binder binder) {
    ConstraintViolationFactory constraintViolationFactory = mock(ConstraintViolationFactory.class);
    ConstraintViolation constraintViolation = mock(ConstraintViolation.class);
    binder.bind(ConstraintViolationFactory.class).toInstance(constraintViolationFactory);
    binder.bind(SelectorManager.class).toInstance(mock(SelectorManager.class));
    when(constraintViolationFactory.createViolation(eq("expression"), anyString())).thenReturn(constraintViolation);
    when(constraintViolation.getPropertyPath()).thenReturn(path);
  }

  @Test
  public void testCreate_invalidExpression() {
    SelectorXO xo = new SelectorXO();
    xo.setExpression("a ==== b");

    try {
      component.create(xo);
      fail();
    }
    catch (ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size(), is(1));
      assertThat(e.getConstraintViolations().iterator().next().getPropertyPath(), is(path));
    }
  }
}
