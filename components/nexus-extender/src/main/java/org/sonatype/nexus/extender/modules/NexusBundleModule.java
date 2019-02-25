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
package org.sonatype.nexus.extender.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.guice.AbstractInterceptorModule;
import org.sonatype.nexus.common.guice.TypeConverterSupport;
import org.sonatype.nexus.validation.ValidationModule;

import com.google.common.base.Strings;
import com.google.inject.Module;
import org.apache.shiro.guice.aop.ShiroAopModule;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.launch.BundleModule;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Nexus specific {@link BundleModule} that uses bundle imports to decide what to install.
 *
 * @since 3.0
 */
public class NexusBundleModule
    extends BundleModule
{
  private static final ShiroAopModule shiroAopModule = new ShiroAopModule();

  private static final SecurityFilterModule securityFilterModule = new SecurityFilterModule();

  private static final MetricsRegistryModule metricsRegistryModule = new MetricsRegistryModule();

  private static final InstrumentationModule instrumentationModule = new InstrumentationModule();

  private static final ValidationModule validationModule = new ValidationModule();

  private static final WebResourcesModule webResourcesModule = new WebResourcesModule();

  private static final RankingModule rankingModule = new RankingModule();

  private final Map<?, ?> nexusProperties;

  private final ServletContextModule servletContextModule;

  private final List<AbstractInterceptorModule> interceptorModules;

  private final List<TypeConverterSupport> converterModules;

  private final String imports;

  public NexusBundleModule(final Bundle bundle,
                           final MutableBeanLocator locator,
                           final Map<?, ?> nexusProperties,
                           final ServletContextModule servletContextModule,
                           final List<AbstractInterceptorModule> interceptorModules,
                           final List<TypeConverterSupport> converterModules)
  {
    super(bundle, locator);

    this.nexusProperties = nexusProperties;
    this.servletContextModule = servletContextModule;
    this.interceptorModules = interceptorModules;
    this.converterModules = converterModules;

    imports = Strings.nullToEmpty(bundle.getHeaders().get(Constants.IMPORT_PACKAGE));
  }

  @Override
  protected List<Module> modules() {
    List<Module> modules = new ArrayList<>();

    maybeAddSecurityFilter(modules);
    maybeAddServletContext(modules);
    maybeAddMetricsRegistry(modules);
    maybeAddWebResources(modules);

    addInterceptors(modules);

    modules.addAll(super.modules());
    modules.addAll(converterModules);
    modules.add(rankingModule);

    return modules;
  }

  @Override
  protected Map<?, ?> getProperties() {
    return nexusProperties;
  }

  @Override
  protected Module spaceModule() {
    return new SpaceModule(space, BeanScanning.GLOBAL_INDEX);
  }

  private void maybeAddSecurityFilter(final List<Module> modules) {
    if (imports.contains("org.sonatype.nexus.security")) {
      modules.add(securityFilterModule);
    }
  }

  private void maybeAddServletContext(final List<Module> modules) {
    if (imports.contains("com.google.inject.servlet")) {
      modules.add(servletContextModule);
    }
  }

  private void maybeAddMetricsRegistry(final List<Module> modules) {
    if (imports.contains("com.codahale.metrics")) {
      modules.add(metricsRegistryModule);
    }
  }

  private void maybeAddWebResources(final List<Module> modules) {
    if (space.getBundle().getEntry("static") != null) {
      modules.add(webResourcesModule);
    }
  }

  private void addInterceptors(final List<Module> modules) {
    modules.add(shiroAopModule);
    modules.add(instrumentationModule);
    modules.add(validationModule);

    for (AbstractInterceptorModule aim : interceptorModules) {
      modules.add(aim);
    }
  }
}
