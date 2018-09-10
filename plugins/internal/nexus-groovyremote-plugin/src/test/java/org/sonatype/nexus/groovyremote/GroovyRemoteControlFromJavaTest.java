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
package org.sonatype.nexus.groovyremote;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;

import org.sonatype.sisu.goodies.common.io.PrintBuffer;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.common.base.Throwables;
import com.sun.net.httpserver.HttpServer;
import groovy.lang.Closure;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovyx.remote.client.RemoteControl;
import groovyx.remote.server.Receiver;
import groovyx.remote.transport.http.HttpTransport;
import groovyx.remote.transport.http.RemoteControlHttpHandler;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.CompilationFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Trials of <a href="http://groovy.codehaus.org/modules/remote/">Groovy Remote Control</a> from Java.
 *
 * This simply ensures that the basics of this library are functional from Java.
 */
public class GroovyRemoteControlFromJavaTest
    extends TestSupport
{
  private GroovyShell shell;

  private File compiledClassesDir;

  private HttpServer server;

  @Before
  public void setUp() throws Exception {
    // Setup groovy script complication to specific directory so we can pass this to the groovyx.remote client
    CompilerConfiguration cc = new CompilerConfiguration();
    compiledClassesDir = util.createTempDir("groovy-classes");
    log("Groovy classes dir: {}", compiledClassesDir);
    cc.setTargetDirectory(compiledClassesDir);
    this.shell = new GroovyShell(cc);

    Receiver receiver = new Receiver();
    RemoteControlHttpHandler handler = new RemoteControlHttpHandler(receiver);
    HttpServer server = HttpServer
        .create(new InetSocketAddress(Inet4Address.getLocalHost(), 0), 0); // force use of ipv4
    server.createContext("/", handler);
    server.start();
    log("Address: {}", server.getAddress());
    this.server = server;
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop(0);
      server = null;
    }
    if (shell != null) {
      shell.resetLoadedClasses();
      shell = null;
    }
  }

  private Closure compile(final String... lines) {
    PrintBuffer buff = new PrintBuffer();
    buff.println("def task = {");
    for (String line : lines) {
      buff.println(line);
    }
    buff.println("}");
    return (Closure) shell.evaluate(buff.toString());
  }

  private Closure compile(final File file) throws IOException {
    return (Closure) shell.evaluate(file);
  }

  public Closure compile(final URL url) {
    checkNotNull(url);

    log("Compile: {}", url);

    Object result;
    try {
      result = shell.evaluate(new GroovyCodeSource(url));
    }
    catch (CompilationFailedException e) {
      throw Throwables.propagate(e);
    }

    log("Result: {}", result);

    return (Closure) result;
  }

  @Test
  public void trial() throws Exception {
    InetSocketAddress addr = server.getAddress();
    String url = String.format("http://%s:%s", addr.getHostName(), addr.getPort());
    log("URL: {}", url);

    HttpTransport transport = new HttpTransport(url);
    ClassLoader cl = new URLClassLoader(new URL[]{compiledClassesDir.toURI().toURL()}, shell.getClassLoader());
    RemoteControl remote = new RemoteControl(transport, cl);

    Closure closure;
    Object result;

    // try compiling inline
    closure = compile(
        "println \"${Thread.currentThread().name} -> holla!\"",
        "return ['cheese', 'please']"
    );
    result = remote.call(closure);
    log("Result: {}", result);

    // try loading from a script file w/ curried params
    closure = compile(util.resolveFile("src/test/resources/sup.groovy"));
    result = remote.call(closure.curry("dude"));
    log("Result: {}", result);

    // try from url
    closure = compile(getClass().getResource("test.groovy"));
    result = remote.call(closure.curry("bitch"));
    log("Result: {}", result);
  }

  @Test
  public void compileClosure() throws Exception {
    PrintBuffer buff = new PrintBuffer();
    buff.println("def task = {");
    buff.println("println \"${Thread.currentThread().name} -> sup bitch\"");
    buff.println("}");

    Object result = shell.evaluate(buff.toString());
    log(result.getClass());
    log(result);
    log(result instanceof Closure);

    assertThat(result, notNullValue());

    Closure closure = (Closure) result;

    closure.call();
  }
}
