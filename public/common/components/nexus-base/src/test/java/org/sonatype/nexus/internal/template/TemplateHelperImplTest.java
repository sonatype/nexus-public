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
package org.sonatype.nexus.internal.template;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;

import org.sonatype.nexus.common.app.ApplicationVersionSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the classpath-URL handling on {@link TemplateHelperImpl}'s flag-on template cache.
 *
 * <p>
 * The flag-on path caches template source keyed by {@code URL.toExternalForm()} in a
 * {@code ConcurrentHashMap}. To keep that map bounded, only {@code jar:} / {@code file:} URLs are
 * cached. FIRE-105/NEXUS-52802 (I2): any other protocol ({@code nested:}, OSGi, request-scoped) is NOT
 * rejected — it falls through to the uncached streaming render, so an unusual classloader cannot
 * break unrelated (non-PyPI) callers of {@code render()}. These tests pin that behaviour and verify
 * it is scoped to the PyPI perf flag.
 */
public class TemplateHelperImplTest
{
  // A real classpath resource — getResource() yields a file: URL under target/test-classes, which
  // the guard admits. Packaged inside a jar at runtime it would be a jar: URL, also admitted.
  private static final String CLASSPATH_TEMPLATE = "test-template.vm";

  private TemplateHelper templateHelper;

  @Before
  public void setUp() {
    BaseUrlHolder.set("http://localhost:8081", "/");
    // Flag-on (optimised path) by default; the flag-off test constructs its own helper with false.
    templateHelper = newHelper(true);
  }

  @After
  public void tearDown() {
    BaseUrlHolder.unset();
  }

  private static TemplateHelper newHelper(final boolean pypiPerfEnabled) {
    return new TemplateHelperImpl(mock(ApplicationVersionSupport.class),
        new VelocityEngineProvider(20).getObject(), pypiPerfEnabled);
  }

  /**
   * FIRE-105/NEXUS-52802 (I2): Flag on + a non-jar/file (here: in-memory {@code mem:}) URL must NOT throw.
   * It is not admitted to the bounded cache; instead {@code render} falls through to the uncached
   * streaming path and renders identically. This protects non-PyPI callers whose classloader may
   * hand back {@code nested:} / OSGi URLs on the default-on path. Uses a hermetic in-memory URL
   * stream handler so the test does no network I/O.
   */
  @Test
  public void render_flagOn_nonClasspathUrl_fallsBackToUncachedRenderNoThrow() throws Exception {
    URL memUrl = inMemoryTemplateUrl("hello ${name}");

    String result = templateHelper.render(memUrl, new TemplateParameters().set("name", "world"));

    assertTrue("Non-jar/file URL should render via the uncached fallback, not throw",
        result.contains("world"));
  }

  /**
   * The uncached fallback must not populate the bounded cache for a non-jar/file URL — rendering the
   * same {@code mem:} URL twice still works and does not depend on caching.
   */
  @Test
  public void render_flagOn_nonClasspathUrl_rendersRepeatably() throws Exception {
    URL memUrl = inMemoryTemplateUrl("v=${name}");

    String first = templateHelper.render(memUrl, new TemplateParameters().set("name", "1"));
    String second = templateHelper.render(memUrl, new TemplateParameters().set("name", "2"));

    assertTrue(first.contains("v=1"));
    assertTrue(second.contains("v=2"));
  }

  /**
   * Flag on + a real classpath (file:) URL: the guard admits it and the template renders normally.
   */
  @Test
  public void render_flagOn_acceptsClasspathUrl() {
    URL template = getClass().getResource(CLASSPATH_TEMPLATE);
    assertTrue("Test resource " + CLASSPATH_TEMPLATE + " must be on the classpath", template != null);
    assertTrue("Test resource should resolve to a file:/jar: URL",
        "file".equals(template.getProtocol()) || "jar".equals(template.getProtocol()));

    String result = templateHelper.render(template, new TemplateParameters().set("name", "world"));

    assertTrue("Rendered output should contain the interpolated parameter", result.contains("world"));
  }

  /**
   * Flag OFF: the guard does not apply — the legacy stream-per-render path handles the URL, proving
   * the guard is scoped to the PyPI perf flag and cannot break the pre-NEXUS-52802 behaviour.
   */
  @Test
  public void render_flagOff_guardDoesNotApply() {
    TemplateHelper legacyHelper = newHelper(false);

    // A classpath URL renders on the legacy path without touching the cache or the guard.
    URL template = getClass().getResource(CLASSPATH_TEMPLATE);
    String result = legacyHelper.render(template, new TemplateParameters().set("name", "legacy"));

    assertTrue("Legacy path should render normally", result.contains("legacy"));
  }

  /**
   * Builds a {@code mem:} URL backed by in-memory bytes via a per-instance {@link URLStreamHandler}.
   * Its protocol is neither {@code jar:} nor {@code file:}, so it exercises the I2 uncached fallback,
   * and {@code openStream()} reads from memory — no network, no filesystem, fully hermetic.
   */
  private static URL inMemoryTemplateUrl(final String content) throws Exception {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    URLStreamHandler handler = new URLStreamHandler()
    {
      @Override
      protected URLConnection openConnection(final URL u) {
        return new URLConnection(u)
        {
          @Override
          public void connect() {
            // no-op — nothing to connect to
          }

          @Override
          public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
          }
        };
      }
    };
    return new URL("mem", "localhost", -1, "/template.vm", handler);
  }
}
