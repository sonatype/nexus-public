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
package org.sonatype.nexus.common.upgrade;

import java.lang.reflect.Method;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpgradeVersionCheckerInterceptorTest
    extends TestSupport
{
  @Mock
  private DatabaseCheck databaseCheck;

  private final Provider<DatabaseCheck> databaseCheckProvider = () -> databaseCheck;

  private AvailabilityVersionCheckerInterceptor underTest;

  @Before
  public void setUp() {
    underTest = new AvailabilityVersionCheckerInterceptor(databaseCheckProvider);
  }

  @Test
  public void invokeShouldFailWhenDatabaseVersionIsNotAtLeastVersion() throws Throwable {
    MethodInvocation methodInvocation = setupMethodInvocation(TestInterface.class.getMethod("annotatedMethod"), false);
    assertThrows(IllegalStateException.class, () -> underTest.invoke(methodInvocation));
  }

  @Test
  public void invokeShouldSucceedWhenDatabaseVersionIsAtLeastVersion() throws Throwable {
    MethodInvocation methodInvocation = setupMethodInvocation(TestInterface.class.getMethod("annotatedMethod"), true);
    underTest.invoke(methodInvocation);
    verify(methodInvocation).proceed();
  }

  private MethodInvocation setupMethodInvocation(Method method, boolean isAtLeast) {
    MethodInvocation methodInvocation = mock(MethodInvocation.class);
    when(methodInvocation.getMethod()).thenReturn(method);
    when(databaseCheck.isAtLeast("1.0.0")).thenReturn(isAtLeast);
    return methodInvocation;
  }

  private interface TestInterface
  {
    @AvailabilityVersion(from = "1.0.0")
    void annotatedMethod();
  }
}
