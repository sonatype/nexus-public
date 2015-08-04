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

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.spi.SubsystemProvider;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SubsystemProvider} that creates subsystems using Guice {@link Injector}.
 *
 * @since 2.7
 */
@Named
@Singleton
public class GuiceSubsystemProvider
    implements SubsystemProvider
{

  private final Injector injector;

  @Inject
  public GuiceSubsystemProvider(final Injector injector) {
    this.injector = injector;
  }

  @Override
  public Object get(final Class type, final Map<Object, Object> context) {
    checkNotNull(type, "type cannot be null");
    checkNotNull(context, "context cannot be null");

    try {
      return injector.createChildInjector(
          new AbstractModule()
          {
            @Override
            protected void configure() {
              for (final Entry<Object, Object> contextEntry : context.entrySet()) {
                if (contextEntry.getKey() instanceof Class) {
                  final Object contextValue = contextEntry.getValue();
                  if (contextValue instanceof Provider) {
                    bind((Class) contextEntry.getKey()).toProvider(new com.google.inject.Provider()
                    {
                      @Override
                      public Object get() {
                        return ((Provider) contextValue).get();
                      }
                    });
                  }
                  else {
                    bind((Class) contextEntry.getKey()).toInstance(contextValue);
                  }
                }
              }
            }
          }
      ).getInstance(type);
    }
    catch (ConfigurationException e) {
      // do noting TODO log
    }
    return null;
  }

}
