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
package org.sonatype.nexus.client.rest.jersey;

import java.lang.reflect.Method;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.client.core.spi.SubsystemProvider;
import org.sonatype.sisu.siesta.client.ClientBuilder.Target.Factory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SubsystemProvider} that automatically creates implementation of Siesta based subsystems.
 * A Siesta based subsystem is an interface with at least one method, interface or any of extended interfaces is
 * annotated with {@link Path}.
 *
 * @since 2.7
 */
@Named
@Singleton
public class SiestaClientSubsystemProvider
    implements SubsystemProvider
{

  /**
   * Creates an implementation of specified type if type is an interface and at least on method, the interface or any
   * of extended interfaces are annotated with {@link Path}.
   */
  @Override
  public Object get(final Class type, final Map<Object, Object> context) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(context, "context cannot be null");

    if (!type.isInterface()) {
      return null;
    }
    final Factory factory = (Factory) context.get(Factory.class);
    if (factory == null) {
      return null;
    }
    if (hasPathAnnotation(type)) {
      return factory.build(type);
    }
    for (final Method method : type.getMethods()) {
      if (method.getAnnotation(Path.class) != null) {
        return factory.build(type);
      }
    }
    return null;
  }

  private boolean hasPathAnnotation(final Class type) {
    if (type.getAnnotation(Path.class) != null) {
      return true;
    }
    for (final Class extended : type.getInterfaces()) {
      if (hasPathAnnotation(extended)) {
        return true;
      }
    }
    return false;
  }

}
