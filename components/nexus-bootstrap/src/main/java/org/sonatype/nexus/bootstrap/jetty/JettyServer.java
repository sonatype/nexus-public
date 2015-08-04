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
package org.sonatype.nexus.bootstrap.jetty;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.nexus.bootstrap.PropertyMap;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty server.
 *
 * @since 2.8
 */
public class JettyServer
{
  private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

  private final ClassLoader classLoader;

  private final Map<String, String> properties;

  private final String[] args;

  private JettyMainThread thread;

  public JettyServer(final ClassLoader classLoader, final Map<String, String> properties, final String[] args) {
    if (classLoader == null) {
      throw new NullPointerException();
    }
    this.classLoader = classLoader;

    if (properties == null) {
      throw new NullPointerException();
    }
    this.properties = properties;

    if (args == null) {
      throw new NullPointerException();
    }
    this.args = args;
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

  public synchronized void start() throws Exception {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);

    try {
      final AtomicReference<Throwable> exception = new AtomicReference<>();
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
        public Object run() {
          try {
            doStart();
          }
          catch (Exception e) {
            exception.set(e);
          }
          return null;
        }
      });

      Throwable e = exception.get();
      if (e != null) {
        log.error("Start failed", e);
        throw propagateThrowable(e);
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  private void doStart() throws Exception {
    if (thread != null) {
      throw new IllegalStateException("Already started");
    }

    log.info("Starting");

    List<LifeCycle> components = new ArrayList<>();

    PropertyMap props = new PropertyMap();
    props.putAll(JettyServer.this.properties);

    // For all arguments, load properties or parse XMLs
    XmlConfiguration last = null;
    for (String arg : args) {
      URL url = Resource.newResource(arg).getURL();

      if (url.getFile().toLowerCase(Locale.ENGLISH).endsWith(".properties")) {
        log.info("Loading properties: {}", url);

        props.load(url);
      }
      else {
        log.info("Applying configuration: {}", url);

        XmlConfiguration configuration = new XmlConfiguration(url);
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

    thread = new JettyMainThread(components);
    thread.setContextClassLoader(classLoader);
    thread.startComponents();

    log.info("Started");
  }

  public synchronized void stop() throws Exception {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);

    try {
      final AtomicReference<Throwable> exception = new AtomicReference<>();
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
        public Object run() {
          try {
            doStop();
          }
          catch (Exception e) {
            exception.set(e);
          }
          return null;
        }
      });

      Throwable e = exception.get();
      if (e != null) {
        log.error("Stop failed", e);
        throw propagateThrowable(e);
      }
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
  }

  private void doStop() throws Exception {
    if (thread == null) {
      throw new IllegalStateException("Not started");
    }

    log.info("Stopping");

    thread.stopComponents();
    thread = null;

    log.info("Stopped");
  }

  /**
   * Jetty thread used to start components, wait for the server's threads to join and stop components.
   *
   * Needed so that once {@link JettyServer#stop()} returns that we know that the server has actually stopped,
   * which is required for embedding.
   */
  private static class JettyMainThread
      extends Thread
  {
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(1);

    private final List<LifeCycle> components;

    private final CountDownLatch started;

    private final CountDownLatch stopped;

    private volatile Exception exception;

    public JettyMainThread(final List<LifeCycle> components) {
      super("jetty-main-" + INSTANCE_COUNTER.getAndIncrement());
      this.components = components;
      this.started = new CountDownLatch(1);
      this.stopped = new CountDownLatch(1);
    }

    @Override
    public void run() {
      try {
        Server server = null;
        try {
          for (LifeCycle component : components) {
            // capture the server reference
            if (component instanceof Server) {
              server = (Server)component;
            }

            if (!component.isRunning()) {
              log.info("Starting: {}", component);
              component.start();
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
          log.info("Running");
          server.join();
        }
      }
      catch (InterruptedException e) {
        // nothing
      }
      finally {
        stopped.countDown();
      }
    }

    public void startComponents() throws Exception {
      start();
      started.await();

      if (exception != null) {
        throw exception;
      }
    }

    public void stopComponents() throws Exception {
      Collections.reverse(components);

      for (LifeCycle component : components) {
        if (component.isRunning()) {
          log.info("Stopping: {}", component);
          component.stop();
        }
      }

      components.clear();
      stopped.await();
    }
  }
}