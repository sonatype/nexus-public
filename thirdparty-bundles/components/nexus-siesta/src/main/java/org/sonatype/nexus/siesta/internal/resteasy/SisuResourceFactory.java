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
package org.sonatype.nexus.siesta.internal.resteasy;

import org.eclipse.sisu.BeanEntry;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sisu {@link ResourceFactory}.
 *
 * @since 3.0
 */
public class SisuResourceFactory
    implements ResourceFactory
{
  private final BeanEntry<?,?> entry;

  private PropertyInjector propertyInjector;

  public SisuResourceFactory(final BeanEntry<?, ?> entry) {
    this.entry = checkNotNull(entry);
  }

  @Override
  public Class<?> getScannableClass() {
    return entry.getImplementationClass();
  }

  @Override
  public void registered(final ResteasyProviderFactory factory) {
    checkNotNull(factory);
    propertyInjector = factory.getInjectorFactory().createPropertyInjector(getScannableClass(), factory);
  }

  @Override
  public Object createResource(final HttpRequest request,
                               final HttpResponse response,
                               final ResteasyProviderFactory factory)
  {
    final Object resource = entry.getValue();
    propertyInjector.inject(request, response, resource);
    return resource;
  }

  @Override
  public void requestFinished(final HttpRequest request, final HttpResponse response, final Object resource) {
    // ignore
  }

  @Override
  public void unregistered() {
    // ignore
  }
}
