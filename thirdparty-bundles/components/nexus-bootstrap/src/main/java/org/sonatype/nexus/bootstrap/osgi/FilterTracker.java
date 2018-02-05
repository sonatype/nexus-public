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
package org.sonatype.nexus.bootstrap.osgi;

import javax.servlet.Filter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Watches for a {@link Filter} with the given name and registers it with {@link DelegatingFilter}.
 * 
 * @since 3.0
 */
public final class FilterTracker
    extends ServiceTracker<Filter, Filter>
{
  private static final String QUERY = "(&(objectClass=" + Filter.class.getName() + ")(name=%s))";

  public FilterTracker(BundleContext bundleContext, String name) throws InvalidSyntaxException {
    super(bundleContext, FrameworkUtil.createFilter(String.format(QUERY, name)), null);
  }

  @Override
  public Filter addingService(ServiceReference<Filter> reference) {
    Filter service = super.addingService(reference);
    DelegatingFilter.set(service);
    return service;
  }

  @Override
  public void removedService(ServiceReference<Filter> reference, Filter service) {
    DelegatingFilter.unset(service);
    super.removedService(reference, service);
  }
}
