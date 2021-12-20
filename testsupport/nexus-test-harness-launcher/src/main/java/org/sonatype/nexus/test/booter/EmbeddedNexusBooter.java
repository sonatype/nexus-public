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
package org.sonatype.nexus.test.booter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.nexus.proxy.maven.routing.internal.ConfigImpl;

import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.io.filefilter.FileFilterUtils.filter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;

/**
 * Embedded Nexus server booter.
 *
 * @since 2.8
 */
public class EmbeddedNexusBooter
    implements NexusBooter
{
  private static Logger log = LoggerFactory.getLogger(EmbeddedNexusBooter.class);

  private static final String IT_REALM_ID = "it-realm";

  private final File installDir;

  private final Map<String,String> overrides;

  private final ClassWorld world;

  private final ClassRealm bootRealm;

  private final Class<?> launcherClass;

  private final Constructor launcherFactory;

  private ClassRealm testRealm;

  private Object launcher;

  public EmbeddedNexusBooter(final File installDir, final int port) throws Exception {
    this.installDir = checkNotNull(installDir).getCanonicalFile();
    log.info("Install directory: {}", installDir);

    // HACK: This is non-standard setup by the test-enviroinment (AbstractEnvironmentMojo)
    File workDir = new File(installDir, "../sonatype-work").getCanonicalFile();
    log.info("Work directory: {}", workDir);

    checkArgument(port > 1024);
    log.info("Port: {}", port);

    overrides = new HashMap<>();
    overrides.put("application-port", String.valueOf(port));
    overrides.put("bundleBasedir", installDir.getPath());
    overrides.put("nexus-work", workDir.getPath());

    // force bootstrap logback configuration
    overrides.put("logback.configurationFile", new File(installDir, "conf/logback.xml").getPath());

    // guice finalizer
    overrides.put("guice.executor.class", "NONE");

    // Making MI integration in Nexus behave in-sync
    overrides.put("org.sonatype.nexus.events.IndexerManagerEventInspector.async", Boolean.FALSE.toString());

    // Disable autorouting initialization prevented
    overrides.put(ConfigImpl.FEATURE_ACTIVE_KEY, Boolean.FALSE.toString());

    log.info("Overrides:");
    for (Entry<String,String> entry : overrides.entrySet()) {
      log.info("  {}='{}'", entry.getKey(), entry.getValue());
    }

    tamperJettyConfiguration();

    world = new ClassWorld();
    bootRealm = createBootRealm();

    launcherClass = bootRealm.loadClass("org.sonatype.nexus.bootstrap.Launcher");
    log.info("Launcher class: {}", launcherClass);

    launcherFactory = launcherClass.getConstructor(ClassLoader.class, Map.class, String[].class);
    log.info("Launcher factory: {}", launcherFactory);
  }

  private void tamperJettyConfiguration() throws IOException {
    final File file = new File(installDir, "conf/jetty.xml");
    String xml = FileUtils.readFileToString(file, "UTF-8");

    // Disable the shutdown hook, since it disturbs the embedded work
    // In Jetty8, any invocation of server.stopAtShutdown(boolean) will create a thread in a class static member.
    // Hence, we simply want to make sure, that there is NO invocation happening of that method.
    // FIXME: These can be avoided by using a <Property> configuration with default value in jetty.xml
    xml = xml.replace(
        "<Set name=\"stopAtShutdown\">true</Set>",
        "<!-- Set name=\"stopAtShutdown\">true</Set -->"
    );

    FileUtils.writeStringToFile(file, xml, "UTF-8");
  }

  private ClassRealm createBootRealm() throws Exception {
    List<URL> classpath = new ArrayList<>() ;

    File confDir = new File(installDir, "conf");
    log.info("Boot conf dir: {}", confDir);
    classpath.add(confDir.toURI().toURL());

    File libDir = new File(installDir, "lib");
    log.info("Boot lib dir: {}", libDir);
    File[] jars = filter(suffixFileFilter(".jar"), libDir.listFiles());
    for (File jar : jars) {
      classpath.add(jar.toURI().toURL());
    }

    ClassRealm realm = world.newRealm("it-boot", null);
    log.info("Boot classpath:");
    for (URL url : classpath) {
      log.info("  {}", url);
      realm.addURL(url);
    }

    return realm;
  }

  @Override
  public void startNexus(final String testId) throws Exception {
    checkState(launcher == null, "Nexus already started");

    testRealm = world.newRealm(IT_REALM_ID + "-" + testId, bootRealm);

    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(bootRealm);

    try {
      log.info("Starting Nexus[{}]", testId);

      launcher = launcherFactory.newInstance(
          testRealm,
          overrides,
          new String[] { new File(installDir, "conf/jetty.xml").getAbsolutePath() }
      );
    }
    finally {
      Thread.currentThread().setContextClassLoader(cl);
    }

    launcherClass.getMethod("start").invoke(launcher);
  }

  @Override
  public void stopNexus() throws Exception {
    try {
      log.info("Stopping Nexus");

      if (launcher != null) {
        launcherClass.getMethod("stop").invoke(launcher);
      }
      launcher = null;
    }
    catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IllegalStateException) {
        log.debug("Ignoring", cause);
      }
      else {
        log.error("Stop failed", cause);
        throw Throwables.propagate(cause);
      }
    }
    finally {
      if (testRealm != null) {
        try {
          world.disposeRealm(testRealm.getId());
        }
        catch (NoSuchRealmException e) {
          log.warn("Unexpected; ignoring", e);
        }
      }
      testRealm = null;

      try {
        // The JVM caches URLs along with their current URL handler in a couple of static maps.
        // This causes unexpected issues when restarting legacy tests (even when using isolated
        // classloaders) because the cached handler persists across the restart and still refers
        // to the now shutdown framework. Felix has a few tricks to workaround this, but these
        // are defeated by the isolated legacy test classloader as the new framework cannot see
        // the old handler classes to reflectively update them.

        // (the other solution would be to not shutdown the framework when running legacy tests,
        // this would keep the old URL handlers alive at the cost of a few additional resources)

        Class<?> jarFileFactoryClass = Class.forName("sun.net.www.protocol.jar.JarFileFactory");
        Field fileCacheField = jarFileFactoryClass.getDeclaredField("fileCache");
        Field urlCacheField = jarFileFactoryClass.getDeclaredField("urlCache");
        fileCacheField.setAccessible(true);
        urlCacheField.setAccessible(true);
        ((Map<?, ?>) fileCacheField.get(null)).clear();
        ((Map<?, ?>) urlCacheField.get(null)).clear();
      } catch (Exception e) {
        log.warn("Unable to clear URL cache", e);
      }

      Thread.yield();
      System.gc();
    }
  }
}
