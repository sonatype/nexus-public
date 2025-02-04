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

import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Interceptor that checks the database schema version before allowing a method to be invoked.
 *
 */
public class AvailabilityVersionCheckerInterceptor
    extends ComponentSupport
    implements MethodInterceptor
{
  private final Provider<DatabaseCheck> databaseCheck;

  public AvailabilityVersionCheckerInterceptor(Provider<DatabaseCheck> databaseCheck) {
    this.databaseCheck = checkNotNull(databaseCheck);
  }

  @Override
  public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
    AvailabilityVersion annotation = getAnnotation(methodInvocation);
    if (!databaseCheck.get().isAtLeast(annotation.from())) {
      throw new IllegalStateException(
          "The database schema version is lower than the minimum required version (%s) to enable this feature "
              .formatted(annotation.from()));
    }
    return methodInvocation.proceed();
  }

  private static AvailabilityVersion getAnnotation(final MethodInvocation methodInvocation) {
    return methodInvocation.getMethod().getAnnotation(AvailabilityVersion.class);
  }
}
