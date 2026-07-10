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
package org.sonatype.nexus.bootstrap.entrypoint.jetty;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

import org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusProperties;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.PropertyMap;
import org.sonatype.nexus.bootstrap.entrypoint.jvm.ShutdownDelegate;
import org.sonatype.nexus.bootstrap.jetty.ConnectorConfiguration;
import org.sonatype.nexus.bootstrap.jetty.ConnectorManager;
import org.sonatype.nexus.bootstrap.jetty.InstrumentedHandler;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.text.Strings2;

import org.springframework.beans.factory.annotation.Autowired;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.CustomErrorHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Jetty server.
 */
@Component
public class JettyServer
{

  private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

  public static final String ERROR_PAGE_PATH = "/error.html";

  private final NexusProperties nexusProperties;

  private final ShutdownDelegate shutdownDelegate;

  private JettyMainThread thread;

  private ConnectorManager connectorManager;

  @Autowired
  public JettyServer(
      final NexusProperties nexusPropeties,
      final ShutdownDelegate shutdownDelegate)
  {
    this.nexusProperties = nexusPropeties;
    this.shutdownDelegate = shutdownDelegate;
  }

  private Exception propagateThrowable(final Throwable e) throws Exception {
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    }
    else if (e instanceof Exception) {
      throw (Exception) e;
    }
    else if (e instanceof Error) {
      throw (Error) e;
    }
    throw new Error(e);
  }

  public List<ConnectorConfiguration> defaultConnectors() {
    return connectorManager.defaultConnectors();
  }

  public void addCustomConnector(final ConnectorConfiguration connectorConfiguration) {
    connectorManager.addConnector(connectorConfiguration);
  }

  public void removeCustomConnector(final ConnectorConfiguration connectorConfiguration) {
    connectorManager.removeConnector(connectorConfiguration);
  }

  /**
   * Starts Jetty, in sync or async mode, depending on value of {@code waitForServer} parameter.
   *
   * @param waitForServer if {@code true}, method will block until Jetty is fully started, otherwise will return
   *          immediately.
   * @param callback optional, callback executed immediately after Jetty is fully started up.
   */
  public synchronized void start(
      final ApplicationContext applicationContextfinal,
      final boolean waitForServer) throws Exception
  {
    try {
      doStart(applicationContextfinal, waitForServer);
    }
    catch (Exception e) {
      LOG.error("Start failed", e);
      throw propagateThrowable(e);
    }
  }

  private void doStart(
      final ApplicationContext applicationContext,
      final boolean waitForServer) throws Exception
  { // NOSONAR
    if (thread != null) {
      throw new IllegalStateException("Already started");
    }

    LOG.info("Starting jetty");

    List<LifeCycle> components = new ArrayList<>();

    PropertyMap props = new PropertyMap();
    props.putAll(nexusProperties.get());

    String[] args = props.get("nexus-args").split(",");

    // For all arguments, load properties or parse XMLs
    XmlConfiguration last = null;
    for (String arg : args) {
      Resource resource = ResourceFactory.root().newResource(arg);
      URL url = resource.getURI().toURL();
      if (url.getFile().toLowerCase(Locale.ENGLISH).endsWith(".properties")) {
        LOG.info("Loading properties: {}", url);

        props.load(url);
      }
      else {
        LOG.info("Applying configuration: {}", url);

        XmlConfiguration configuration = new XmlConfiguration(resource);
        if (last != null) {
          configuration.getIdMap().putAll(last.getIdMap());
        }
        if (!props.isEmpty()) {
          configuration.getProperties().putAll(props);
        }
        Object component = configuration.configure();
        if (component instanceof LifeCycle) {
          components.add((LifeCycle) component);
        }
        last = configuration;
      }
    }

    // complain if no components configured
    if (components.isEmpty()) {
      throw new Exception("Failed to configure any components");
    }

    Server server = null;
    for (Object object : components) {
      if (object instanceof Server) {
        server = (Server) object;
        server.setErrorHandler(new CustomErrorHandler());
        break;
      }
    }

    connectorManager = new ConnectorManager(server, last.getIdMap());

    registerServlets(applicationContext, server);

    thread = new JettyMainThread(components, shutdownDelegate, applicationContext);
    thread.startComponents(waitForServer);
  }

  public synchronized void stop() throws Exception {
    try {
      doStop();
    }
    catch (Exception e) {
      LOG.error("Stop failed", e);
      throw propagateThrowable(e);
    }
  }

  private static void registerServlets(final ApplicationContext context, final Server server) {
    InstrumentedHandler defaultHandler = server.getBean(InstrumentedHandler.class);
    if (defaultHandler == null) {
      throw new IllegalStateException("Missing default handler");
    }

    ServletContextHandler servletContext = defaultHandler.getDelegate();
    context.getBeansOfType(HttpServlet.class)
        .values()
        .stream()
        .sorted(QualifierUtil::compareByOrder)
        .forEach(createRegistration(servletContext));
    context.getBeansOfType(Filter.class)
        .values()
        .stream()
        .sorted(QualifierUtil::compareByOrder)
        .forEach(createFilterRegistration(servletContext));

    context.getBeansOfType(ServletContextListener.class)
        .values()
        .stream()
        .filter(EventListener.class::isInstance)
        .map(EventListener.class::cast)
        .forEach(servletContext::addEventListener);
  }

  private static Consumer<HttpServlet> createRegistration(final ServletContextHandler servletContext) {
    return servlet -> {
      WebServlet annotation = servlet.getClass().getAnnotation(WebServlet.class);
      if (annotation == null) {
        LOG.debug("Skipping servlet {}", servlet);
        return;
      }
      LOG.debug("Registering servlet {}", servlet);
      ServletHolder holder = new ServletHolder(servlet);

      Stream.of(annotation.initParams()).forEach(param -> holder.setInitParameter(param.name(), param.value()));

      holder.setAsyncSupported(annotation.asyncSupported());
      if (!Strings2.isBlank(annotation.displayName())) {
        holder.setDisplayName(annotation.displayName());
      }
      holder.setClassName(servlet.getClass().getName());

      for (String pattern : annotation.urlPatterns()) {
        LOG.debug("Adding pattern {}", pattern);
        servletContext.addServlet(holder, pattern);
      }
      for (String pattern : annotation.value()) {
        LOG.debug("Adding pattern {}", pattern);
        servletContext.addServlet(holder, pattern);
      }
    };
  }

  private static Consumer<Filter> createFilterRegistration(final ServletContextHandler servletContext) {
    return filter -> {
      WebFilter annotation = filter.getClass().getAnnotation(WebFilter.class);
      if (annotation == null) {
        LOG.debug("Skipping filter {}", filter);
        return;
      }
      LOG.debug("Registering filter {}", filter);
      FilterHolder holder = new FilterHolder(filter);
      if (!Strings2.isBlank(annotation.filterName())) {
        holder.setName(annotation.filterName());
      }
      holder.setClassName(filter.getClass().getName());

      EnumSet<DispatcherType> dispatcherTypes = EnumSet.copyOf(List.of(annotation.dispatcherTypes()));
      for (String pattern : annotation.urlPatterns()) {
        LOG.debug("Adding pattern {}", pattern);
        servletContext.addFilter(holder, pattern, dispatcherTypes);
      }
      for (String pattern : annotation.value()) {
        LOG.debug("Adding pattern {}", pattern);
        servletContext.addFilter(holder, pattern, dispatcherTypes);
      }
    };
  }

  /**
   * This implementation is tightly coupled to the jetty.xml provided in nexus-base-overlay.
   *
   * @param server
   * @return
   */
  static ContextHandler getContextHandler(final Server server) {
    ContextHandler context = null;
    Handler handler = server.getDefaultHandler();
    if (handler instanceof Handler.Wrapper wrapped) {
      handler = wrapped.getHandler();
      if (handler instanceof ContextHandler contextHandler) {
        context = contextHandler;
      }
    }
    return context;
  }

  private void doStop() throws Exception {
    if (thread == null) {
      throw new IllegalStateException("Not started");
    }

    LOG.info("Stopping jetty");

    thread.stopComponents();
    thread = null;

    LOG.info("Stopped jetty");
  }

  /**
   * Jetty thread used to start components, wait for the server's threads to join and stop components.
   * <p>
   * Needed so that once {@link JettyServer#stop()} returns that we know that the server has actually stopped, which is
   * required for embedding.
   */
  private static class JettyMainThread
      extends Thread
  {
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(1);

    private final List<LifeCycle> components;

    private final CountDownLatch started;

    private final CountDownLatch stopped;

    private final ShutdownDelegate shutdownDelegate;

    private final ApplicationContext applicationContext;

    private volatile Exception exception;

    public JettyMainThread(
        final List<LifeCycle> components,
        final ShutdownDelegate shutdownDelegate,
        final ApplicationContext applicationContext)
    {
      super("jetty-main-" + INSTANCE_COUNTER.getAndIncrement());
      this.components = components;
      this.shutdownDelegate = shutdownDelegate;
      this.applicationContext = applicationContext;
      this.started = new CountDownLatch(1);
      this.stopped = new CountDownLatch(1);
    }

    @Override
    public void run() {
      Server server = null;
      try {
        try {
          for (LifeCycle component : components) {
            if (!component.isRunning()) {
              LOG.info("Starting: {}", component);
              component.start();
            }

            // capture the server reference
            if (component instanceof Server) {
              server = (Server) component;
            }
          }
        }
        catch (Exception e) {
          exception = e;
        }
        finally {
          started.countDown();
        }

        if (server != null) {
          logStartupBanner(server);
          server.join();
        }
      }
      catch (InterruptedException e) {
        // nothing
        LOG.info("caught interrupt signal, shutting down");
      }
      finally {
        stopped.countDown();
      }

      if (server == null) {
        LOG.error("Failed to start", exception);
        shutdownDelegate.exit(-1);
      }
    }

    public void startComponents(final boolean waitForServer) throws Exception {
      start();

      if (waitForServer) {
        started.await();
      }

      if (exception != null) {
        throw exception;
      }
    }

    public void stopComponents() throws Exception {
      Collections.reverse(components);

      // if Jetty thread is still waiting for a component to start, this should unblock it
      interrupt();

      for (LifeCycle component : components) {
        if (component.isRunning()) {
          LOG.info("Stopping: {}", component);
          component.stop();
        }
      }

      components.clear();
      stopped.await();
    }

    private void logStartupBanner(final Server server) {
      String banner = "Nexus Repository Manager - Unknown Version and Edition";

      try {
        ApplicationVersion applicationVersion = applicationContext.getBean(ApplicationVersion.class);
        String revision = Optional.ofNullable(applicationVersion.getBuildRevision())
            .filter(rev -> rev.length() >= 8)
            .map(rev -> rev.substring(0, 8))
            .orElse("");
        banner = "Sonatype Nexus %s %s (%s)".formatted(
            applicationVersion.getEdition(),
            applicationVersion.getVersion(),
            revision);
      }
      catch (Exception e) {
        LOG.error("ApplicationVersion not available for banner", e);
      }

      StringBuilder buf = new StringBuilder();
      buf.append("\n-------------------------------------------------\n\n");
      buf.append("Started ").append(banner);
      buf.append("\n\n-------------------------------------------------");
      LOG.info(buf.toString());
    }
  }
}
