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
package org.sonatype.nexus.features.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.BundleException.DUPLICATE_BUNDLE_ERROR;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.SERVICE_RANKING;

/**
 * Fast {@link FeaturesService} wrapper that installs bundles directly using 'reference:' URLs where possible.
 *
 * It also avoids a lot of the complexity of the standard Karaf features service which has additional checks to
 * decide when to refresh bundles, and a more complex resolution process. Since we only use simple features that
 * just list bundles we can make this optimization. It also gives us better stability in terms of when a bundle
 * is refreshed - the standard Karaf behaviour can often end up rebooting the entire application even though it
 * doesn't strictly need to do that.
 *
 * @since 3.19
 */
public class FeaturesWrapper
    extends ServiceTracker<FeaturesService, FeaturesService>
    implements InvocationHandler
{
  private static final Logger log = LoggerFactory.getLogger(FeaturesWrapper.class);

  private static final Dictionary<String, ?> MAX_RANKING =
      new Hashtable<>(singletonMap(SERVICE_RANKING, MAX_VALUE)); // NOSONAR: registerService API wants a Dictionary

  private final FeaturesService wrapper = (FeaturesService) newProxyInstance(FeaturesService.class.getClassLoader(),
      new Class<?>[]{FeaturesService.class}, this);

  private volatile FeaturesService delegate;

  private ServiceRegistration<FeaturesService> trampoline;

  private volatile LocationResolver locationResolver;

  private volatile FeaturesResolver featuresResolver;

  public FeaturesWrapper(final BundleContext context) {
    super(context, FeaturesService.class, null);
  }

  /**
   * Install our wrapper as soon as the stock {@link FeaturesService} appears.
   */
  @Override
  public FeaturesService addingService(final ServiceReference<FeaturesService> reference) {
    if (delegate == null) {
      synchronized (wrapper) {
        if (delegate == null) {
          log.info("Fast FeaturesService starting");
          delegate = super.addingService(reference);
          // maximum ranking to make sure our wrapper is seen before the standard service
          trampoline = context.registerService(FeaturesService.class, wrapper, MAX_RANKING);
          // can't initialize our resolver here as delegate hasn't loaded all its data yet
        }
      }
      return delegate;
    }
    return null;
  }

  /**
   * Uninstall our wrapper as soon as the stock {@link FeaturesService} disappears.
   */
  @Override
  public void removedService(final ServiceReference<FeaturesService> reference, final FeaturesService service) {
    if (delegate == service) { // NOSONAR: we want to check exact instance
      synchronized (wrapper) {
        if (delegate == service) { // NOSONAR: we want to check exact instance
          log.info("Fast FeaturesService stopping");
          trampoline.unregister();
          super.removedService(reference, service);
          locationResolver = null;
          featuresResolver = null;
          delegate = null;
        }
      }
    }
  }

  /**
   * Intercept any 'installFeature' requests and redirect them to our 'fast' alternative.
   */
  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    String methodName = method.getName();
    if (methodName.startsWith("installFeature")) {
      return fastInstallFeature(method, args);
    }
    else if (methodName.startsWith("uninstallFeature")) {
      log.info("uninstalling is not supported in 'fastFeatures' mode");
      return null;
    }
    else if ("listInstalledFeatures".equals(methodName)) {
      return listInstalledFeatures();
    }
    else if ("isInstalled".equals(methodName)) {
      return isInstalled((Feature) args[0]);
    }
    else if ("getState".equals(methodName)) {
      return getState((String) args[0]);
    }
    try {
      return method.invoke(delegate, args);
    }
    catch (InvocationTargetException e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  /**
   * Fast version of 'installFeature' that installs all bundles in a feature and its dependencies then starts them.
   */
  @SuppressWarnings("unchecked")
  private Void fastInstallFeature(final Method method, final Object[] args) throws Exception {
    Stream<BundleInfo> bundles;

    // resolve the given feature and any dependencies
    String signature = stream(method.getParameterTypes()).map(Class::getName).collect(joining(", "));
    switch (signature) {
      case "java.lang.String":
      case "java.lang.String, java.util.EnumSet":
        bundles = featuresResolver().resolve((String) args[0]);
        break;
      case "java.lang.String, java.lang.String":
      case "java.lang.String, java.lang.String, java.util.EnumSet":
        bundles = featuresResolver().resolve(args[0] + "/" + args[1]);
        break;
      case "org.apache.karaf.features.Feature, java.util.EnumSet":
        bundles = featuresResolver().resolve((Feature) args[0]);
        break;
      case "java.util.Set, java.util.EnumSet":
      case "java.util.Set, java.lang.String, java.util.EnumSet":
        bundles = featuresResolver().resolve((Set<String>) args[0]);
        break;
      default:
        throw new IllegalArgumentException("Unexpected method: " + method);
    }

    // install all the bundles in a single sweep and then start them
    bundles.map(this::installBundle)
        .filter(Objects::nonNull)
        .collect(toList())
        .forEach(this::startBundle);

    return null;
  }

  public Feature[] listInstalledFeatures() throws Exception {
    return stream(delegate.listFeatures())
        .filter(this::isInstalled)
        .toArray(Feature[]::new);
  }

  public boolean isInstalled(final Feature feature) {
    return featuresResolver().isInstalled(feature.getId());
  }

  public FeatureState getState(final String featureId) {
    if (featuresResolver().isInstalled(featureId)) {
      return FeatureState.Installed;
    }
    else {
      return delegate.getState(featureId);
    }
  }

  /**
   * Initializes the feature resolver using the current state of the delegate.
   */
  private FeaturesResolver featuresResolver() {
    if (featuresResolver == null) {
      synchronized (wrapper) {
        if (featuresResolver == null) {
          locationResolver = new LocationResolver();
          featuresResolver = new FeaturesResolver(delegate);
        }
      }
    }
    return featuresResolver;
  }

  /**
   * Install the specified bundle, using a direct 'reference:' URL where possible.
   */
  private Bundle installBundle(final BundleInfo info) {
    try {
      // find the most direct URL to the bundle JAR
      String location = locationResolver.resolve(info.getLocation());
      if (context.getBundle(location) == null) {
        // not yet installed, go ahead and install it
        log.debug("Installing {}", location);
        Bundle bundle = context.installBundle(location);
        log.debug("Installed {}/{} from {}", bundle.getSymbolicName(), bundle.getVersion(), location);
        if (info.getStartLevel() > 0) {
          bundle.adapt(BundleStartLevel.class).setStartLevel(info.getStartLevel());
        }
        // fragments cannot be started, so remove them from the final result
        if (info.isStart() && bundle.getHeaders().get(FRAGMENT_HOST) == null) {
          return bundle;
        }
      }
    }
    catch (BundleException e) {
      // ignore duplicates with different locations
      if (e.getType() != DUPLICATE_BUNDLE_ERROR) {
        log.warn("Problem installing {}", info.getLocation(), e);
      }
    }
    return null; // we only return bundles that need to be started
  }

  /**
   * Start the installed bundle.
   */
  private void startBundle(final Bundle bundle) {
    // ignore if bundle is already in the starting state or later
    if ((bundle.getState() & (Bundle.INSTALLED | Bundle.RESOLVED)) != 0) {
      try {
        log.debug("Starting {}/{}", bundle.getSymbolicName(), bundle.getVersion());
        bundle.start();
        log.debug("Started {}/{}", bundle.getSymbolicName(), bundle.getVersion());
      }
      catch (BundleException e) {
        log.warn("Problem starting {}", bundle.getLocation(), e);
      }
    }
  }
}
