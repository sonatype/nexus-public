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

import java.io.IOException;

// NEXUS-46395: javax.servlet → jakarta.servlet for RESTEasy 7. javax.annotation.Nullable
// (JSR-305) stays as-is.
import javax.annotation.Nullable;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.sonatype.nexus.rest.Component;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.siesta.ComponentContainer;
import org.sonatype.nexus.siesta.SiestaResourceMethodFinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * RESTEasy {@link ComponentContainer}.
 *
 * @since 3.0
 */
@org.springframework.stereotype.Component
public class ComponentContainerImpl
    extends HttpServletDispatcher
    implements ComponentContainer
{
  private static final Logger log = LoggerFactory.getLogger(ComponentContainerImpl.class);

  private final transient ResteasyDeployment deployment;

  private final ApplicationContext context;

  @Autowired
  public ComponentContainerImpl(final ResteasyDeployment deployment, final ApplicationContext context) {
    this.deployment = deployment;
    // Register RESTEasy with JAX-RS as early as possible
    RuntimeDelegate.setInstance(checkNotNull(deployment.getProviderFactory()));
    this.context = context;
  }

  @Override
  public void init(final ServletConfig servletConfig) throws ServletException {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(ResteasyProviderFactory.class.getClassLoader());
      doInit(servletConfig);
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
    // register components
    context.getBeansOfType(Component.class)
        .forEach(this::addComponent);
  }

  private void doInit(final ServletConfig servletConfig) throws ServletException {
    deployment.start();

    servletConfig.getServletContext().setAttribute(ResteasyDeployment.class.getName(), deployment);
    servletConfig.getServletContext()
        .setAttribute(
            SiestaResourceMethodFinder.class.getName(), new SiestaResourceMethodFinder(this, deployment));

    super.init(servletConfig);

    ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
    // NEXUS-46395: in RESTEasy 7 the registry methods moved off ResteasyProviderFactory
    // onto the Configuration/ResteasyProviderFactoryImpl. Filters are now registered via
    // the standard JAX-RS provider registration; the deprecated registerClass paths still
    // work via the impl class.
    providerFactory.getContainerResponseFilterRegistry().registerClass(NotCacheableResponseFilter.class);

    if (log.isDebugEnabled()) {
      log.debug("Provider factory: {}", providerFactory);
      log.debug("Configuration: {}", providerFactory.getConfiguration());
      log.debug("Runtime type: {}", providerFactory.getRuntimeType());
      log.debug("Built-ins registered: {}", providerFactory.isBuiltinsRegistered());
      log.debug("Properties: {}", providerFactory.getProperties());
      log.debug("Dynamic features: {}", providerFactory.getServerDynamicFeatures());
      log.debug("Enabled features: {}", providerFactory.getEnabledFeatures());
      log.debug("Reader interceptor registry: {}", providerFactory.getServerReaderInterceptorRegistry());
      log.debug("Writer interceptor registry: {}", providerFactory.getServerWriterInterceptorRegistry());
      log.debug("Injector factory: {}", providerFactory.getInjectorFactory());
      log.debug("Instances: {}", providerFactory.getInstances());
      // NEXUS-46395: getClassContracts() and getExceptionMappers() were removed from the
      // ResteasyProviderFactory interface in RESTEasy 7. Drop the debug logs; the impl class
      // still has these but going through the impl creates a tighter coupling.
    }
  }

  @Override
  public void destroy() {
    super.destroy();

    deployment.stop();
    RuntimeDelegate.setInstance(null);
  }

  /**
   * Promotes {@link HttpServletDispatcher#service(HttpServletRequest, HttpServletResponse)} to public access.
   */
  @Override
  public void service(
      final HttpServletRequest request,
      final HttpServletResponse response) throws ServletException, IOException
  {
    super.service(request, response);
  }

  private static boolean isResource(final Class<?> type) {
    return Resource.class.isAssignableFrom(type);
  }

  @Nullable
  private static String resourcePath(final Class<?> type) {
    Path path = type.getAnnotation(Path.class);
    if (path != null) {
      return path.value();
    }
    return null;
  }

  private void addComponent(final String beanName, final Component component) {
    Class<?> type = component.getClass();
    if (isResource(type)) {
      getDispatcher().getRegistry().addResourceFactory(new SpringResourceFactory(context, beanName));
      String path = resourcePath(type);
      if (path == null) {
        log.warn("Found resource implementation missing @Path: {}", type.getName());
      }
      else {
        log.debug("Added resource: {} with path: {}", type.getName(), path);
      }
    }
    else {
      // TODO: Doesn't seem to be a late-biding/factory here so we create the object early
      getDispatcher().getProviderFactory().register(component);
      log.debug("Added component: {}", type.getName());
    }
  }
}
