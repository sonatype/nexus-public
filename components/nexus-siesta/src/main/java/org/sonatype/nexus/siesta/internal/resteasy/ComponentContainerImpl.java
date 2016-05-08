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

import javax.annotation.Nullable;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.ext.RuntimeDelegate;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.siesta.ComponentContainer;

import org.eclipse.sisu.BeanEntry;
import org.jboss.resteasy.logging.Logger.LoggerType;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * RESTEasy {@link ComponentContainer}.
 *
 * @since 3.0
 */
public class ComponentContainerImpl
  extends HttpServletDispatcher
  implements ComponentContainer
{
  private static final Logger log = LoggerFactory.getLogger(ComponentContainerImpl.class);

  private transient final ResteasyDeployment deployment = new SisuResteasyDeployment();

  public ComponentContainerImpl() {
    // Configure RESTEasy to use SLF4j
    org.jboss.resteasy.logging.Logger.setLoggerType(LoggerType.SLF4J);

    // Register RESTEasy with JAX-RS as early as possible
    RuntimeDelegate.setInstance(checkNotNull(deployment.getProviderFactory()));
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
  }

  private void doInit(final ServletConfig servletConfig) throws ServletException {
    servletConfig.getServletContext().setAttribute(ResteasyDeployment.class.getName(), deployment);

    super.init(servletConfig);

    if (log.isDebugEnabled()) {
      ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
      log.debug("Provider factory: {}", providerFactory);
      log.debug("Configuration: {}", providerFactory.getConfiguration());
      log.debug("Runtime type: {}", providerFactory.getRuntimeType());
      log.debug("Built-ins registered: {}", providerFactory.isBuiltinsRegistered());
      log.debug("Properties: {}", providerFactory.getProperties());
      log.debug("Dynamic features: {}", providerFactory.getServerDynamicFeatures());
      log.debug("Enabled features: {}", providerFactory.getEnabledFeatures());
      log.debug("Class contracts: {}", providerFactory.getClassContracts());
      log.debug("Reader interceptor registry: {}", providerFactory.getServerReaderInterceptorRegistry());
      log.debug("Writer interceptor registry: {}", providerFactory.getServerWriterInterceptorRegistry());
      log.debug("Injector factory: {}", providerFactory.getInjectorFactory());
      log.debug("Instances: {}", providerFactory.getInstances());
      log.debug("Exception mappers: {}", providerFactory.getExceptionMappers());
    }
  }

  /**
   * Promotes {@link HttpServletDispatcher#service(HttpServletRequest, HttpServletResponse)} to public access.
   */
  @Override
  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
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

  @Override
  public void addComponent(final BeanEntry<?, ?> entry) throws Exception {
    Class<?> type = entry.getImplementationClass();
    if (isResource(type)) {
      getDispatcher().getRegistry().addResourceFactory(new SisuResourceFactory(entry));
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
      getDispatcher().getProviderFactory().register(entry.getValue());
      log.debug("Added component: {}", type.getName());
    }
  }

  @Override
  public void removeComponent(final BeanEntry<?, ?> entry) throws Exception {
    Class<?> type = entry.getImplementationClass();
    if (isResource(type)) {
      getDispatcher().getRegistry().removeRegistrations(type);
      String path = resourcePath(type);
      log.debug("Removed resource: {} with path: {}", type.getName(), path);
    }
    else {
      ResteasyProviderFactory providerFactory = getDispatcher().getProviderFactory();
      if (providerFactory instanceof SisuResteasyProviderFactory) {
        ((SisuResteasyProviderFactory) providerFactory).removeRegistrations(type);
        log.debug("Removed component: {}", type.getName());
      }
      else {
        log.warn("Component removal not supported; Unable to remove component: {}", type.getName());
      }
    }
  }
}
