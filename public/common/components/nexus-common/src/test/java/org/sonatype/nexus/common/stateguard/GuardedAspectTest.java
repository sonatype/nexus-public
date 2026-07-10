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
package org.sonatype.nexus.common.stateguard;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GuardedAspectTest
{
  private GuardedAspect guardedAspect;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  @Mock
  private StateGuardAware target;

  @Mock
  private StateGuard stateGuard;

  @Mock
  private Guard guard;

  @Before
  public void setUp() {
    guardedAspect = new GuardedAspect();
  }

  @Test
  public void testAround() throws Throwable {
    // Mock method and annotation
    Method method = TestClass.class.getMethod("testMethod");
    Guarded guarded = method.getAnnotation(Guarded.class);

    // Mock joinPoint behavior
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(target);
    when(target.getStateGuard()).thenReturn(stateGuard);
    when(stateGuard.guard(guarded.by())).thenReturn(guard);

    // Mock guard behavior
    when(guard.run(any())).thenAnswer(invocation -> {
      Action<?> action = invocation.getArgument(0);
      return action.run();
    });

    // Execute the aspect
    guardedAspect.around(joinPoint, guarded);

    // Verify interactions
    verify(stateGuard).guard(guarded.by());
    verify(joinPoint).proceed();
  }

  // Test class with a method annotated with @Guarded
  static class TestClass
  {
    @Guarded(by = "NEW")
    public void testMethod() {
    }
  }
}
