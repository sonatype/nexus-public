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
package org.sonatype.nexus.extender;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.sonatype.nexus.common.guice.AbstractInterceptorModule;
import org.sonatype.nexus.common.guice.TypeConverterSupport;
import org.sonatype.nexus.extender.modules.NexusBundleModule;
import org.sonatype.nexus.extender.modules.ServletContextModule;

import com.google.inject.Key;
import com.google.inject.Module;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.SisuBundlePlan;
import org.eclipse.sisu.wire.EntryListAdapter;
import org.eclipse.sisu.wire.ParameterKeys;
import org.osgi.framework.Bundle;

/**
 * Adapts Sisu's default plan to use {@link NexusBundleModule} for configuration.
 *
 * @since 3.0
 */
public class NexusBundlePlan
    extends SisuBundlePlan
{
  private final Map<?, ?> nexusProperties;

  private final ServletContextModule servletContextModule;

  private final List<AbstractInterceptorModule> interceptorModules;

  private final List<TypeConverterSupport> converterModules;

  public NexusBundlePlan(final MutableBeanLocator locator) {
    super(locator);

    // lookup these core elements once rather than on each call to compose
    nexusProperties = getFirstValue(locator.locate(ParameterKeys.PROPERTIES));
    servletContextModule = new ServletContextModule(getFirstValue(locator.locate(Key.get(ServletContext.class))));
    interceptorModules = new EntryListAdapter<>(locator.locate(Key.get(AbstractInterceptorModule.class)));
    converterModules = new EntryListAdapter<>(locator.locate(Key.get(TypeConverterSupport.class)));
  }

  @Override
  protected boolean appliesTo(final Bundle bundle) {
    return true; // our custom tracker pre-filters the bundles
  }

  @Override
  protected Module compose(final Bundle bundle) {
    return new NexusBundleModule(bundle, locator, nexusProperties, //
        servletContextModule, interceptorModules, converterModules);
  }

  private static <T> T getFirstValue(final Iterable<? extends BeanEntry<?, T>> entries) {
    return entries.iterator().next().getValue();
  }
}
