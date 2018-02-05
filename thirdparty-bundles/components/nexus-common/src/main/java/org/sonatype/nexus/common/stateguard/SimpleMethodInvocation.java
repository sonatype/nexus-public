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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Simple {@link MethodInvocation} wrapper.
 * 
 * @since 3.0
 */
public class SimpleMethodInvocation
    implements MethodInvocation
{
  private final Object instance;
  private final Method method;
  private final Object[] args;

  public SimpleMethodInvocation(final Object instance, final Method method, final Object[] args) {
    this.instance = instance;
    this.method = method;
    this.args = args;
  }

  public Method getMethod() {
    return method;
  }

  public Object getThis() {
    return instance;
  }

  public Object[] getArguments() {
    return args;
  }

  public Object proceed() throws Throwable {
    try {
      return method.invoke(instance, args);
    }
    catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  public AccessibleObject getStaticPart() {
    throw new UnsupportedOperationException();
  }
}
