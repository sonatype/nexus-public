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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.template.EscapeHelper;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link TemplateHelper}.
 *
 * @since 3.0
 */
@Component
public class TemplateHelperImpl
    implements TemplateHelper
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ApplicationVersion applicationVersion;

  private final VelocityEngine velocityEngine;

  /**
   * Cache of template external-form → source text (flag-on path only).
   * Keyed by {@code URL.toExternalForm()} (not the {@code URL} object — {@code URL.equals/hashCode}
   * can do DNS lookups, and {@code getResource()} returns a fresh instance per call).
   * Only {@code jar:} and {@code file:} URLs are admitted, keeping the map bounded to the tiny fixed
   * set of classpath-constant templates. FIRE-105/NEXUS-52802 (I2): any other protocol (Spring Boot nested:, OSGi,
   * request-scoped) is
   * not cached and not rejected — render() falls through to the uncached path, so an unusual
   * classloader cannot break unrelated callers.
   */
  private final ConcurrentHashMap<String, String> templateCache = new ConcurrentHashMap<>();

  /**
   * Stateless EscapeHelper instance, reused across all renders.
   */
  private final EscapeHelper escapeHelper = new EscapeHelper();

  // FIRE-105/NEXUS-52802: PyPI perf path (template-content cache + reused EscapeHelper) is gated on
  // the PyPI flag. Injected as a @Value here rather than via the PyPI bean because nexus-base is
  // upstream of the PyPI module and cannot depend on it. NEXUS-53854: make unconditional + remove.
  private final boolean pypiPerfEnabled;

  @Autowired
  public TemplateHelperImpl(
      final ApplicationVersion applicationVersion,
      final VelocityEngine velocityEngine,
      @Value(FeatureFlags.NEXUS_PCCS_PERF_PYPI_ENABLED_NAMED_VALUE) final boolean pypiPerfEnabled)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.velocityEngine = checkNotNull(velocityEngine);
    this.pypiPerfEnabled = pypiPerfEnabled;
  }

  @Override
  public TemplateParameters parameters() {
    TemplateParameters params = new TemplateParameters();
    params.set("nexusVersion", applicationVersion.getVersion());
    params.set("nexusEdition", applicationVersion.getEdition());
    params.set("nexusBrandedEditionAndVersion", applicationVersion.getBrandedEditionAndVersion());
    params.set("relativePath", BaseUrlHolder.getRelativePath());
    params.set("urlSuffix", applicationVersion.getVersion()); // for cache busting
    // Scoped to PyPI to reduce blast radius. NEXUS-53854: remove else-branch.
    if (pypiPerfEnabled) {
      params.set("esc", escapeHelper);
    }
    else {
      params.set("esc", new EscapeHelper());
    }
    return params;
  }

  @Override
  public String render(final URL template, final TemplateParameters parameters) {
    checkNotNull(template);
    checkNotNull(parameters);

    log.trace("Rendering template: {} w/params: {}", template, parameters);

    // Scoped to PyPI to reduce blast radius. NEXUS-53854: delete the flag-off block.
    if (!pypiPerfEnabled) {
      return renderUncached(template, parameters);
    }

    // FIRE-105/NEXUS-52802 (I2): this render path is default-on for EVERY caller (Maven POM,
    // capability/browse pages, task notifications), not just PyPI. Non-jar/file protocols
    // (nested:, OSGi, request-scoped) must NOT throw — fall through to the uncached render.
    String protocol = template.getProtocol();
    if (!"jar".equals(protocol) && !"file".equals(protocol)) {
      log.trace("Uncached render for non-jar/file template protocol '{}': {}", protocol, template);
      return renderUncached(template, parameters);
    }

    try {
      // Key by URL.toExternalForm() (see templateCache Javadoc) — a value-stable String, so callers
      // that resolve their template via getClass().getResource(name) per render (fresh, non-identical
      // URL instances) still hit the cache. URL.equals()/hashCode() are avoided (can do DNS lookups).
      // FIRE-105/NEXUS-52802 (M2): get-first; computeIfAbsent (which can lock the bin on hit in
      // some JDKs) only on miss. Hit is the steady-state case — a tiny fixed template set.
      String key = template.toExternalForm();
      String templateContent = templateCache.get(key);
      if (templateContent == null) {
        templateContent = templateCache.computeIfAbsent(key, k -> {
          try (Reader r = new InputStreamReader(template.openStream(), StandardCharsets.UTF_8)) {
            return CharStreams.toString(r);
          }
          catch (IOException e) {
            throw new UncheckedIOException("Failed to load template: " + template, e);
          }
        });
      }

      // Render using cached content instead of opening a new stream each time
      StringWriter buff = new StringWriter();
      velocityEngine.evaluate(new VelocityContext(parameters.get()), buff, template.getFile(),
          new StringReader(templateContent));

      String result = buff.toString();
      log.trace("Result: {}", result);

      return result;
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Uncached render: stream the template straight into Velocity's {@code evaluate}. Used on the
   * flag-off legacy path and, FIRE-105/NEXUS-52802 (I2), as the flag-on fallback for template URLs whose
   * protocol is not {@code jar:}/{@code file:} (so they are not admitted to the bounded cache).
   * Produces output identical to the cached path.
   */
  private String renderUncached(final URL template, final TemplateParameters parameters) {
    try (Reader input = new InputStreamReader(template.openStream(), StandardCharsets.UTF_8)) {
      StringWriter buff = new StringWriter();
      velocityEngine.evaluate(new VelocityContext(parameters.get()), buff, template.getFile(), input);
      String result = buff.toString();
      log.trace("Result: {}", result);
      return result;
    }
    catch (IOException e) {
      // Template I/O failures surface as UncheckedIOException (not a bare RuntimeException) so callers
      // see a consistent exception type regardless of flag state or cache path.
      throw new UncheckedIOException("Failed to load template: " + template, e);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }
}
