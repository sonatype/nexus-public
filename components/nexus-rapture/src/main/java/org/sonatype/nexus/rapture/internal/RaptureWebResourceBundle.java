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
package org.sonatype.nexus.rapture.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateAccessible;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.rapture.internal.state.StateComponent;
import org.sonatype.nexus.servlet.ServletHelper;
import org.sonatype.nexus.ui.UiPluginDescriptor;
import org.sonatype.nexus.webresources.GeneratedWebResource;
import org.sonatype.nexus.webresources.WebResource;
import org.sonatype.nexus.webresources.WebResourceBundle;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Rapture {@link WebResourceBundle}.
 *
 * Provides resources:
 * <ul>
 * <li>{@code /index.html}</li>
 * <li>{@code /static/rapture/bootstrap.js}</li>
 * <li>{@code /static/rapture/resources/baseapp.css}</li>
 * <li>{@code /static/rapture/app.js}</li>
 * </ul>
 *
 * @since 3.0
 */
@Named
@Singleton
public class RaptureWebResourceBundle
    extends ComponentSupport
    implements WebResourceBundle
{
  private final ApplicationVersion applicationVersion;

  private final Provider<HttpServletRequest> servletRequestProvider;

  private final Provider<StateComponent> stateComponentProvider;

  private final TemplateHelper templateHelper;

  private final List<UiPluginDescriptor> pluginDescriptors;

  private final List<org.sonatype.nexus.rapture.UiPluginDescriptor> extJsPluginDescriptors;

  private final Gson gson;

  private final String cacheBuster;

  private final boolean analyticsEnabled;

  public final static String PROPERTY_WEBRESOURCES_CACHEBUSTER = "nexus.webresources.cachebuster";

  @Inject
  public RaptureWebResourceBundle(
      final ApplicationVersion applicationVersion,
      final Provider<HttpServletRequest> servletRequestProvider,
      final Provider<StateComponent> stateComponentProvider,
      final TemplateHelper templateHelper,
      final List<UiPluginDescriptor> pluginDescriptors,
      final List<org.sonatype.nexus.rapture.UiPluginDescriptor> extJsPluginDescriptors,
      @Nullable @Named("${" + PROPERTY_WEBRESOURCES_CACHEBUSTER + "}") final String cacheBuster,
      @Named("${nexus.analytics.enabled:-true}") final boolean analyticsEnabled)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.servletRequestProvider = checkNotNull(servletRequestProvider);
    this.stateComponentProvider = checkNotNull(stateComponentProvider);
    this.templateHelper = checkNotNull(templateHelper);
    this.pluginDescriptors = checkNotNull(pluginDescriptors);
    this.extJsPluginDescriptors = checkNotNull(extJsPluginDescriptors);
    this.analyticsEnabled = analyticsEnabled;
    if (cacheBuster == null) {
      this.cacheBuster = applicationVersion.getBuildTimestamp();
    }
    else {
      log.info("Setting web resources cache buster value to {} from property {}", cacheBuster,
          PROPERTY_WEBRESOURCES_CACHEBUSTER);
      this.cacheBuster = cacheBuster;
    }
    log.info("UI plugin descriptors:");
    for (UiPluginDescriptor descriptor : pluginDescriptors) {
      log.info("  {}", descriptor.getName());
    }

    log.info("ExtJS UI plugin descriptors:");
    for (org.sonatype.nexus.rapture.UiPluginDescriptor descriptor : extJsPluginDescriptors) {
      log.info("  {}", descriptor.getPluginId());
    }

    gson = new GsonBuilder().setPrettyPrinting().create();
  }

  //
  // FIXME: optimize so that we dont duplicate things like isDebug() over and over each request
  // FIXME: for now we simply do a bit more work than is needed :-(
  //

  @Override
  public List<WebResource> getResources() {
    return ImmutableList.of(
        index_html(),
        bootstrap_js(),
        baseapp_css(),
        app_js(),
        copyright_html());
  }

  private abstract class TemplateWebResource
      extends GeneratedWebResource
  {
    protected byte[] render(final String template, final TemplateParameters parameters) throws IOException {
      log.trace("Rendering template: {}, with params: {}", template, parameters);
      URL url = getClass().getResource(template);
      return templateHelper.render(url, parameters).getBytes();
    }
  }

  @TemplateAccessible
  public static class TemplateUtil
  {
    /**
     * Helper to return the filename for a URI.
     */
    public String fileName(final URI uri) {
      String path = uri.getPath();
      int i = path.lastIndexOf('/');
      return path.substring(i + 1, path.length());
    }
  }

  /**
   * The index.html resource.
   */
  private WebResource index_html() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/index.html";
      }

      @Override
      public String getContentType() {
        return HTML;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("index.vm", new TemplateParameters()
            .set("baseUrl", BaseUrlHolder.get())
            .set("relativePath", BaseUrlHolder.getRelativePath())
            .set("debug", isDebug())
            .set("urlSuffix", generateUrlSuffix())
            .set("styles", getStyles())
            .set("scripts", getScripts())
            .set("util", new TemplateUtil())
            .set("analyticsEnabled", analyticsEnabled));
      }
    };
  }

  /**
   * The bootstrap.js resource.
   */
  private WebResource bootstrap_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/bootstrap.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("bootstrap.vm", new TemplateParameters()
            .set("baseUrl", BaseUrlHolder.get())
            .set("relativePath", BaseUrlHolder.getRelativePath())
            .set("debug", isDebug())
            .set("urlSuffix", generateUrlSuffix())
            .set("namespaces", getExtJsNamespaces()));
      }
    };
  }

  /**
   * The baseapp css file.
   */
  private WebResource baseapp_css() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/resources/baseapp.css";
      }

      @Override
      public String getContentType() {
        return CSS;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("baseapp_css.vm", new TemplateParameters()
            .set("debug", isDebug())
            .set("urlSuffix", generateUrlSuffix()));
      }
    };
  }

  /**
   * The baseapp css file.
   */
  private WebResource copyright_html() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/COPYRIGHT.html";
      }

      @Override
      public String getContentType() {
        return HTML;
      }

      @Override
      protected byte[] generate() throws IOException {

        return render("COPYRIGHT.vm", new TemplateParameters()
            .set("edition", applicationVersion.getEdition()));
      }
    };
  }

  /**
   * The app.js resource.
   */
  private WebResource app_js() {
    return new TemplateWebResource()
    {
      @Override
      public String getPath() {
        return "/static/rapture/app.js";
      }

      @Override
      public String getContentType() {
        return JAVASCRIPT;
      }

      @Override
      protected byte[] generate() throws IOException {
        return render("app.vm", new TemplateParameters()
            .set("baseUrl", BaseUrlHolder.getRelativePath())
            .set("debug", isDebug())
            .set("state", gson.toJson(getState()))
            .set("pluginConfigs", getExtJsPluginConfigs()));
      }
    };
  }

  /**
   * Generate URL suffix to use on all requests when loading the index.
   */
  private String generateUrlSuffix() {
    StringBuilder buff = new StringBuilder();
    String version = applicationVersion.getVersion();
    String edition = applicationVersion.getEdition();
    buff.append("_v=").append(version);
    buff.append("&_e=").append(edition);
    buff.append("&_c=").append(this.cacheBuster);

    // if version is a SNAPSHOT, then append additional timestamp to disable cache
    if (version.endsWith("SNAPSHOT")) {
      buff.append("&_dc=").append(System.currentTimeMillis());
    }

    // when debug, add parameter
    if (isDebug()) {
      buff.append("&debug=true");
    }

    return buff.toString();
  }

  /**
   * Check if ?debug parameter is given on the request.
   */
  private boolean isDebug() {
    HttpServletRequest request = servletRequestProvider.get();
    return ServletHelper.isDebug(request);
  }

  /**
   * Returns the initial state for the application.
   */
  private Map<String, Object> getState() {
    return stateComponentProvider.get().getState(Maps.<String, String>newHashMap());
  }

  /**
   * Find all plugin configs.
   */
  @VisibleForTesting
  List<String> getExtJsPluginConfigs() {
    List<String> classNames = Lists.newArrayList();
    for (org.sonatype.nexus.rapture.UiPluginDescriptor descriptor : extJsPluginDescriptors) {
      String className = descriptor.getConfigClassName();
      if (className != null) {
        classNames.add(className);
      }
    }
    return classNames;
  }

  /**
   * Determine all plugin namespaces.
   */
  @VisibleForTesting
  List<String> getExtJsNamespaces() {
    List<String> namespaces = Lists.newArrayList();
    for (org.sonatype.nexus.rapture.UiPluginDescriptor descriptor : extJsPluginDescriptors) {
      String ns = descriptor.getNamespace();
      if (ns != null) {
        namespaces.add(ns);
      }
    }
    return namespaces;
  }

  /**
   * Replaces "{mode}" in given path with either "prod" or "debug".
   */
  private String mode(final String path) {
    String mode = isDebug() ? "debug" : "prod";
    return path.replaceAll("\\{mode\\}", mode);
  }

  /**
   * Generate a URI for the given path.
   */
  private URI uri(final String path) {
    try {
      return new URI(String.format("%s/static/rapture/%s", BaseUrlHolder.getRelativePath(), path));
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param path
   * @return BaseUrlHolder.get() + /static/ + path
   */
  private URI relativeToAbsoluteUri(final String path) {
    try {
      return new URI(String.format("%s%s", BaseUrlHolder.getRelativePath(), path));
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generate the list of CSS styles to include in the index.html.
   */
  @VisibleForTesting
  List<URI> getStyles() {
    List<URI> styles = Lists.newArrayList();
    styles.add(uri(mode("resources/loading-{mode}.css")));
    styles.add(uri(mode("resources/baseapp.css")));

    // add extjs descriptor styles
    styles.addAll(getExtJsStyles());

    List<URI> resources = pluginDescriptors.stream()
        .map(UiPluginDescriptor::getStyles)
        .flatMap(Collection::stream)
        .map(this::relativeToAbsoluteUri)
        .collect(toList());
    styles.addAll(resources);

    return styles;
  }

  private List<URI> getExtJsStyles() {
    List<URI> styles = Lists.newArrayList();
    for (org.sonatype.nexus.rapture.UiPluginDescriptor descriptor : extJsPluginDescriptors) {
      if (descriptor.hasStyle()) {
        String path = String.format("resources/%s-{mode}.css", descriptor.getPluginId());
        styles.add(uri(mode(path)));
      }
    }
    return styles;
  }

  /**
   * Generate the list of javascript sources to include in the index.html.
   */
  @VisibleForTesting
  List<URI> getScripts() {
    boolean debug = isDebug();

    List<URI> scripts = Lists.newArrayList();

    scripts.add(uri(mode("baseapp-{mode}.js")));
    scripts.add(uri(mode("extdirect-{mode}.js")));
    scripts.add(uri("bootstrap.js"));

    scripts.addAll(
        extJsPluginDescriptors.stream()
            .map(descriptor -> descriptor.getScripts(debug))
            .flatMap(Collection::stream)
            .map(this::relativeToAbsoluteUri)
            .collect(toList()));

    List<URI> resources = pluginDescriptors.stream()
        .map(descriptor -> descriptor.getScripts(debug))
        .flatMap(Collection::stream)
        .map(this::relativeToAbsoluteUri)
        .collect(toList());
    scripts.addAll(resources);

    if (!debug) {
      // add all extjs scripts
      scripts.addAll(getExtJsScripts());
    }

    scripts.add(uri("app.js"));
    return scripts;
  }

  private List<URI> getExtJsScripts() {
    List<URI> scripts = Lists.newArrayList();
    for (org.sonatype.nexus.rapture.UiPluginDescriptor descriptor : extJsPluginDescriptors) {
      if (descriptor.hasScript()) {
        String path = String.format("%s-prod.js", descriptor.getPluginId());
        scripts.add(uri(path));
      }
    }

    return scripts;
  }
}
