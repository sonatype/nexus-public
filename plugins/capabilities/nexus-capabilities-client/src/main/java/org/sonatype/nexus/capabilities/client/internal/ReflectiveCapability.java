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
package org.sonatype.nexus.capabilities.client.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.spi.CapabilityClient;
import org.sonatype.nexus.capabilities.client.spi.CapabilityProperty;
import org.sonatype.nexus.capabilities.client.support.CapabilityImpl;
import org.sonatype.nexus.capabilities.client.support.ReflectiveCapabilityImplementationException;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.capabilities.model.CapabilityStatusXO;
import org.sonatype.nexus.client.core.exception.NexusClientException;

import com.google.common.primitives.Primitives;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflection based capability implementation.
 *
 * @since capabilities 2.2
 */
public class ReflectiveCapability
    implements InvocationHandler
{

  private final Class<? extends Capability> capabilityType;

  private final CapabilityImpl delegate;

  public ReflectiveCapability(final Class<? extends Capability> capabilityType,
                              final CapabilityClient client,
                              final String type)
  {
    this.capabilityType = checkNotNull(capabilityType);
    delegate = new CapabilityImpl(client, type);
  }

  public ReflectiveCapability(final Class<? extends Capability> capabilityType,
                              final CapabilityClient client,
                              final CapabilityStatusXO settings)
  {
    this.capabilityType = checkNotNull(capabilityType);
    delegate = new CapabilityImpl(client, settings);
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args)
      throws Throwable
  {
    final CapabilityProperty capabilityProperty = method.getAnnotation(CapabilityProperty.class);
    if (capabilityProperty != null) {
      if (args == null || args.length == 0) {
        // we have a getter
        final String value = delegate.property(capabilityProperty.value());
        if (value == null || Void.TYPE.equals(method.getReturnType())) {
          return null;
        }
        if (method.getReturnType().isAssignableFrom(capabilityType)) {
          return proxy;
        }
        try {
          return Primitives.wrap(method.getReturnType())
              .getConstructor(String.class)
              .newInstance(value);
        }
        catch (final Exception e) {
          throw new ReflectiveCapabilityImplementationException(
              "Could not convert '" + value + "' to a " + method.getReturnType()
          );
        }
      }
      else if (args.length == 1) {
        delegate.withProperty(capabilityProperty.value(), args[0] == null ? null : args[0].toString());
        if (Void.TYPE.equals(method.getReturnType())) {
          return null;
        }
        if (method.getReturnType().isAssignableFrom(capabilityType)) {
          return proxy;
        }
        throw new ReflectiveCapabilityImplementationException("Could not reflectively implement " + method);
      }
      else {
        throw new ReflectiveCapabilityImplementationException(
            CapabilityProperty.class.getName() + " annotations are only allowed on setters and getters"
        );
      }
    }
    try {
      final Method actual = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
      final Object result = actual.invoke(delegate, args);
      if (method.getReturnType() != null && method.getReturnType().isAssignableFrom(capabilityType)) {
        return proxy;
      }
      return result;
    }
    catch (NoSuchMethodException e) {
      throw new ReflectiveCapabilityImplementationException("Could not reflectively implement " + method);
    }
    catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
    catch (IllegalAccessException e) {
      throw new NexusClientException(
          "Could not reflectively implement " + proxy.getClass(), e
      )
      {
      };
    }
  }

}
