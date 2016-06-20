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
package org.sonatype.nexus.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionScheme;
import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.guice.AbstractInterceptorModule;
import org.sonatype.nexus.guice.NexusModules.PluginModule;
import org.sonatype.nexus.guice.NexusTypeBinder;
import org.sonatype.nexus.plugins.events.PluginActivatedEvent;
import org.sonatype.nexus.plugins.events.PluginRejectedEvent;
import org.sonatype.nexus.plugins.repository.NexusPluginRepository;
import org.sonatype.nexus.plugins.repository.NoSuchPluginRepositoryArtifactException;
import org.sonatype.nexus.plugins.repository.PluginRepositoryArtifact;
import org.sonatype.nexus.util.AlphanumComparator;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.ClasspathDependency;
import org.sonatype.plugins.model.PluginDependency;
import org.sonatype.plugins.model.PluginMetadata;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import org.eclipse.sisu.Parameters;
import org.eclipse.sisu.bean.BeanManager;
import org.eclipse.sisu.inject.DefaultRankingFunction;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.eclipse.sisu.inject.RankingFunction;
import org.eclipse.sisu.plexus.DefaultPlexusBeanLocator;
import org.eclipse.sisu.plexus.PlexusAnnotatedBeanModule;
import org.eclipse.sisu.plexus.PlexusBeanConverter;
import org.eclipse.sisu.plexus.PlexusBeanLocator;
import org.eclipse.sisu.plexus.PlexusBeanModule;
import org.eclipse.sisu.plexus.PlexusBindingModule;
import org.eclipse.sisu.plexus.PlexusXmlBeanConverter;
import org.eclipse.sisu.plexus.PlexusXmlBeanModule;
import org.eclipse.sisu.space.BundleClassSpace;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
@Deprecated
public class DefaultNexusPluginManager
    implements NexusPluginManager
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final EventBus eventBus;

  private final NexusPluginRepository repositoryManager;

  private final Map<String, String> variables;

  private final List<AbstractInterceptorModule> interceptorModules;

  private final MutableBeanLocator beanLocator;

  private final BeanManager beanManager;

  private final Provider<Bundle> systemBundleProvider;

  private final Map<GAVCoordinate, PluginDescriptor> activePlugins = new HashMap<GAVCoordinate, PluginDescriptor>();

  private final Map<GAVCoordinate, PluginResponse> pluginResponses = new HashMap<GAVCoordinate, PluginResponse>();

  private final VersionScheme versionParser = new GenericVersionScheme();

  private final AtomicInteger pluginRank = new AtomicInteger(1);

  @Inject
  public DefaultNexusPluginManager(final EventBus eventBus,
                                   final NexusPluginRepository repositoryManager,
                                   final @Parameters Map<String, String> variables,
                                   final List<AbstractInterceptorModule> interceptorModules,
                                   final MutableBeanLocator beanLocator,
                                   final BeanManager beanManager,
                                   final Provider<Bundle> systemBundleProvider)
  {
    this.eventBus = checkNotNull(eventBus);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.variables = checkNotNull(variables);
    this.interceptorModules = checkNotNull(interceptorModules);
    this.beanLocator = checkNotNull(beanLocator);
    this.beanManager = checkNotNull(beanManager);
    this.systemBundleProvider = checkNotNull(systemBundleProvider);
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public Map<GAVCoordinate, PluginResponse> getPluginResponses() {
    return new HashMap<GAVCoordinate, PluginResponse>(pluginResponses);
  }

  @Timed
  public Collection<PluginManagerResponse> activateInstalledPlugins() {
    final List<PluginManagerResponse> result = new ArrayList<PluginManagerResponse>();

    // if multiple V's for GAs are found, choose the one with biggest version (and pray that plugins has sane
    // versioning)
    Map<GAVCoordinate, PluginMetadata> filteredPlugins =
        filterInstalledPlugins(repositoryManager.findAvailablePlugins());

    for (final GAVCoordinate gav : filteredPlugins.keySet()) {
      // activate what we found in reposes
      result.add(activatePlugin(gav, true, filteredPlugins.keySet()));
    }
    return result;
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  /**
   * Filters a map of GAVCoordinates by "max" version. Hence, in the result Map, it is guaranteed that only one GA
   * combination will exists, and if input contained multiple V's for same GA, the one GAV contained in result with
   * have max V.
   */
  protected Map<GAVCoordinate, PluginMetadata> filterInstalledPlugins(
      final Map<GAVCoordinate, PluginMetadata> installedPlugins)
  {
    final HashMap<GAVCoordinate, PluginMetadata> result =
        new HashMap<GAVCoordinate, PluginMetadata>(installedPlugins.size());

    nextInstalledEntry:
    for (Map.Entry<GAVCoordinate, PluginMetadata> installedEntry : installedPlugins.entrySet()) {
      for (Iterator<Map.Entry<GAVCoordinate, PluginMetadata>> resultItr = result.entrySet().iterator();
           resultItr.hasNext(); ) {
        final Map.Entry<GAVCoordinate, PluginMetadata> resultEntry = resultItr.next();
        if (resultEntry.getKey().matchesByGA(installedEntry.getKey())) {
          if (compareVersionStrings(resultEntry.getKey().getVersion(), installedEntry.getKey().getVersion()) < 0) {
            resultItr.remove(); // result contains smaller version than installedOne, remove it
          }
          else {
            continue nextInstalledEntry;
          }
        }
      }
      result.put(installedEntry.getKey(), installedEntry.getValue());
    }

    return result;
  }

  protected int compareVersionStrings(final String v1str, final String v2str) {
    try {
      final Version v1 = versionParser.parseVersion(v1str);
      final Version v2 = versionParser.parseVersion(v2str);

      return v1.compareTo(v2);
    }
    catch (InvalidVersionSpecificationException e) {
      // fall back to "sane" human alike sorting of strings
      return new AlphanumComparator().compare(v1str, v2str);
    }
  }

  protected GAVCoordinate getActivatedPluginGav(final GAVCoordinate gav, final boolean strict) {
    // try exact match 1st
    if (activePlugins.containsKey(gav)) {
      return gav;
    }

    // if we are lax, try by GA
    if (!strict) {
      for (GAVCoordinate coord : activePlugins.keySet()) {
        if (coord.matchesByGA(gav)) {
          return coord;
        }
      }
    }

    // sad face here
    return null;
  }

  protected PluginManagerResponse activatePlugin(final GAVCoordinate gav, final boolean strict,
                                                 final Set<GAVCoordinate> installedPluginsFilteredByGA)
  {
    final GAVCoordinate activatedGav = getActivatedPluginGav(gav, strict);
    if (activatedGav == null) {
      GAVCoordinate actualGAV = null;
      if (!strict) {
        actualGAV = findInstalledPluginByGA(installedPluginsFilteredByGA, gav);
      }
      if (actualGAV == null) {
        actualGAV = gav;
      }
      final PluginManagerResponse response = new PluginManagerResponse(actualGAV, PluginActivationRequest.ACTIVATE);
      try {
        activatePlugin(
            repositoryManager.resolveArtifact(actualGAV), response, installedPluginsFilteredByGA
        );
      }
      catch (final NoSuchPluginRepositoryArtifactException e) {
        reportMissingPlugin(response, e);
      }
      return response;
    }
    else {
      return new PluginManagerResponse(activatedGav, PluginActivationRequest.ACTIVATE);
    }
  }

  private GAVCoordinate findInstalledPluginByGA(final Set<GAVCoordinate> installedPluginsFilteredByGA,
                                                final GAVCoordinate gav)
  {
    if (installedPluginsFilteredByGA != null) {
      for (GAVCoordinate coord : installedPluginsFilteredByGA) {
        if (coord.matchesByGA(gav)) {
          return coord;
        }
      }
    }
    return null;
  }

  private void activatePlugin(final PluginRepositoryArtifact plugin,
                              final PluginManagerResponse response,
                              final Set<GAVCoordinate> installedPluginsFilteredByGA)
      throws NoSuchPluginRepositoryArtifactException
  {
    final GAVCoordinate pluginGAV = plugin.getCoordinate();
    final PluginMetadata metadata = plugin.getPluginMetadata();

    final PluginDescriptor descriptor = new PluginDescriptor(pluginGAV);
    descriptor.setPluginMetadata(metadata);

    final PluginResponse result = new PluginResponse(pluginGAV, PluginActivationRequest.ACTIVATE);
    result.setPluginDescriptor(descriptor);

    activePlugins.put(pluginGAV, descriptor);

    final List<GAVCoordinate> importList = new ArrayList<GAVCoordinate>();
    final List<GAVCoordinate> resolvedList = new ArrayList<GAVCoordinate>();
    for (final PluginDependency pd : metadata.getPluginDependencies()) {
      // here, a plugin might express a need for GAV1, but GAV2 might be already activated
      // since today we just "play" dependency resolution, we support GA resolution only
      // so, we say "relax version matching" and rely on luck for now it will work
      final GAVCoordinate gav = new GAVCoordinate(pd.getGroupId(), pd.getArtifactId(), pd.getVersion());
      final PluginManagerResponse dependencyActivationResponse = activatePlugin(
          gav, false, installedPluginsFilteredByGA
      );
      if (pd.isOptional() && !dependencyActivationResponse.isSuccessful()) {
        continue; // ignore optional plugin dependency when missing
      }
      response.addPluginManagerResponse(dependencyActivationResponse);
      importList.add(dependencyActivationResponse.getOriginator());
      resolvedList.add(dependencyActivationResponse.getOriginator());
    }
    descriptor.setImportedPlugins(importList);
    descriptor.setResolvedPlugins(resolvedList);

    if (!response.isSuccessful()) {
      result.setAchievedGoal(PluginActivationResult.BROKEN);
    }
    else {
      try {
        createPluginInjector(plugin, descriptor);
        result.setAchievedGoal(PluginActivationResult.ACTIVATED);
      }
      catch (final Throwable e) {
        result.setThrowable(e);
      }
    }

    reportActivationResult(response, result);
  }

  void createPluginInjector(final PluginRepositoryArtifact plugin, final PluginDescriptor descriptor)
      throws NoSuchPluginRepositoryArtifactException
  {
    final String location = "reference:"+plugin.getFile().getParentFile().toURI();

    final Bundle pluginBundle;
    try {
      pluginBundle = systemBundleProvider.get().getBundleContext().installBundle(location);
      pluginBundle.start();
    }
    catch (BundleException e) {
      throw new IllegalStateException("Problem installing: "+location, e);
    }

    final List<URL> scanList = new ArrayList<URL>();

    final URL pluginURL = toURL(plugin);
    if (null != pluginURL) {
      scanList.add(pluginURL);
    }

    for (final ClasspathDependency d : descriptor.getPluginMetadata().getClasspathDependencies()) {
      final GAVCoordinate gav =
          new GAVCoordinate(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getClassifier(), d.getType());

      final URL url = toURL(repositoryManager.resolveDependencyArtifact(plugin, gav));
      if (null != url) {
        if (d.isHasComponents() || d.isShared() || hasComponents(url)) {
          scanList.add(url);
        }
      }
    }

    final List<PlexusBeanModule> beanModules = new ArrayList<PlexusBeanModule>();

    // Scan for Plexus XML components
    final ClassSpace pluginSpace = new BundleClassSpace(pluginBundle);
    beanModules.add(new PlexusXmlBeanModule(pluginSpace, variables));

    // Scan for annotated components
    final ClassLoader pluginLoader = pluginBundle.adapt(BundleWiring.class).getClassLoader();
    final ClassSpace scanSpace = new URLClassSpace(pluginLoader, scanList.toArray(new URL[scanList.size()]));
    beanModules.add(new PlexusAnnotatedBeanModule(scanSpace, variables).with(NexusTypeBinder.STRATEGY));

    // Assemble plugin components and resources
    final List<Module> modules = new ArrayList<Module>();
    modules.add(new PluginModule());
    modules.addAll(interceptorModules);
    modules.add(new PlexusBindingModule(beanManager, beanModules));
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        // TEMP: extender bundle will handle this in next step
        bind(MutableBeanLocator.class).toInstance(beanLocator);
        bind(RankingFunction.class).toInstance(new DefaultRankingFunction(pluginRank.incrementAndGet()));
        bind(PlexusBeanLocator.class).to(DefaultPlexusBeanLocator.class);
        bind(PlexusBeanConverter.class).to(PlexusXmlBeanConverter.class);
        bind(ParameterKeys.PROPERTIES).toInstance(variables);
      }
    });

    Guice.createInjector(new WireModule(modules));
  }

  private static URL toURL(final PluginRepositoryArtifact artifact) {
    try {
      return artifact.getFile().toURI().toURL();
    }
    catch (final MalformedURLException e) {
      return null; // should never happen
    }
  }

  private static boolean hasComponents(final URL url) {
    // this has to happen in generic way, as for example Nexus IDE may provide
    // various URLs using XmlNexusPluginRepository for example
    try {
      final URL sisuIndexUrl = url.toURI().resolve("META-INF/sisu/" + Named.class.getName()).toURL();
      if (exists(sisuIndexUrl)) {
        return true;
      }
      // no need for plx XML discovery, as that will be picked up
      // even without scanning
    }
    catch (Exception e) {
      // just neglect any URISyntaxEx or MalformedUrlEx
    }
    return false;
  }

  private static boolean exists(final URL url) {
    try (final InputStream content = url.openStream()) {
      return true;
    }
    catch (IOException e) {
      return false;
    }
  }

  private void reportMissingPlugin(final PluginManagerResponse response,
                                   final NoSuchPluginRepositoryArtifactException cause)
  {
    final GAVCoordinate gav = cause.getCoordinate();
    final PluginResponse result = new PluginResponse(gav, response.getRequest());
    result.setThrowable(cause);
    result.setAchievedGoal(PluginActivationResult.MISSING);

    response.addPluginResponse(result);
    pluginResponses.put(gav, result);
  }

  private void reportActivationResult(final PluginManagerResponse response, final PluginResponse result) {
    final Event<NexusPluginManager> pluginEvent;
    final GAVCoordinate gav = result.getPluginCoordinates();
    if (result.isSuccessful()) {
      pluginEvent = new PluginActivatedEvent(this, result.getPluginDescriptor());
    }
    else {
      pluginEvent = new PluginRejectedEvent(this, gav, result.getThrowable());
      activePlugins.remove(gav);
    }

    response.addPluginResponse(result);
    pluginResponses.put(gav, result);

    eventBus.post(pluginEvent);
  }
}
