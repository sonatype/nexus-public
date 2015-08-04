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
package org.sonatype.nexus.groovyremote.client;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.sonatype.sisu.goodies.common.io.PrintBuffer;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovyx.remote.client.RemoteControl;
import groovyx.remote.transport.http.HttpTransport;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Groovy remote control client.
 *
 * @since 2.6
 */
public class GroovyRemoteClient
{
  private static final Logger log = LoggerFactory.getLogger(GroovyRemoteClient.class);

  private final GroovyShell shell;

  private final RemoteControl remote;

  public GroovyRemoteClient(final ClassLoader classLoader,
                            final File classesDir,
                            final List<File> extraDirs,
                            final URL url)
      throws IOException
  {
    checkNotNull(classLoader);
    log.debug("Class loader: {}", classLoader);

    checkNotNull(classesDir);
    log.debug("Classes dir: {}", classesDir);

    checkNotNull(url);
    log.debug("URL: {}", url);

    Binding binding = new Binding();

    CompilerConfiguration cc = new CompilerConfiguration();
    cc.setTargetDirectory(classesDir);
    this.shell = new GroovyShell(classLoader, binding, cc);

    HttpTransport transport = new HttpTransport(url.toExternalForm());

    // provide transport with custom class-loader which includes generated classes
    List<URL> urls = Lists.newArrayList();
    urls.add(classesDir.toURI().toURL());
    for (File dir : extraDirs) {
      urls.add(dir.toURI().toURL());
    }

    // TODO: Could probably use GCL here instead, would be simpler
    ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), shell.getClassLoader());

    this.remote = new RemoteControl(transport, cl);
  }

  public GroovyShell getShell() {
    return shell;
  }

  public RemoteControl getRemote() {
    return remote;
  }

  //
  // Compilation
  //

  @SuppressWarnings("ConstantConditions")
  private Closure asClosure(final Object obj) {
    checkState(obj instanceof Closure);
    return (Closure) obj;
  }

  public Closure compile(final String... lines) {
    checkNotNull(lines);

    PrintBuffer buff = new PrintBuffer();
    buff.println("def task = {");
    for (String line : lines) {
      buff.println(line);
    }
    buff.println("}");

    String script = buff.toString();
    log.debug("Script: {}", script);

    Object result = shell.evaluate(buff.toString());

    log.trace("Result: {}", result);

    return asClosure(result);
  }

  public Closure compile(final URL url) {
    checkNotNull(url);

    log.debug("Compile: {}", url);

    Object result;
    try {
      result = shell.evaluate(new GroovyCodeSource(url));
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }

    log.trace("Result: {}", result);

    return asClosure(result);
  }

  public Closure compile(final File file) {
    checkNotNull(file);

    try {
      return compile(file.toURI().toURL());
    }
    catch (MalformedURLException e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // Helpers
  //

  public URL resource(final Class owner, final String name) {
    URL url = owner.getResource(name);
    checkNotNull(url, "Missing resource: %s owner: %s", name, owner);
    return url;
  }

  public URL resource(final Object owner, final String name) {
    checkNotNull(owner);
    return resource(owner.getClass(), name);
  }

  @SuppressWarnings("unchecked")
  public <T> T call(final Closure... commands) {
    checkNotNull(commands);
    return (T) remote.call(commands);
  }

  public <T> T call(final String... script) {
    return call(compile(script));
  }

  public <T> T call(final URL script) {
    return call(compile(script));
  }

  public <T> T call(final File script) {
    return call(compile(script));
  }

  //
  // Builder
  //

  public static class Builder
  {
    private ClassLoader classLoader;

    private File classesDir;

    private List<File> extraDirs = Lists.newArrayList();

    private URL url;

    public Builder setClassLoader(final ClassLoader classLoader) {
      this.classLoader = classLoader;
      return this;
    }

    public Builder setClassesDir(final File classesDir) {
      this.classesDir = classesDir;
      return this;
    }

    public Builder addExtraDir(final File dir) {
      checkNotNull(dir);
      this.extraDirs.add(dir);
      return this;
    }

    public Builder setUrl(final URL url) {
      this.url = url;
      return this;
    }

    public GroovyRemoteClient build() {
      try {
        return new GroovyRemoteClient(classLoader, classesDir, extraDirs, url);
      }
      catch (IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
