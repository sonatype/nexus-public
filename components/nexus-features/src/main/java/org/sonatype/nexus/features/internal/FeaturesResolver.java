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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedSet;

/**
 * Resolves one or more Karaf {@link Feature}s into a flat sequence of {@link BundleInfo}s.
 *
 * Elements such as configuration snippets, conditionals, version ranges, etc. are ignored.
 * This is an acceptable simplification because our NXRM features don't use those elements.
 *
 * @since 3.19
 */
public class FeaturesResolver
{
  private static final Logger log = LoggerFactory.getLogger(FeaturesResolver.class);

  private static final String VERSION_WILDCARD = "/0.0.0";

  private final Map<String, Feature> featuresById = new HashMap<>();

  private final Set<String> installedFeatures = synchronizedSet(new HashSet<>());

  private final Set<String> excludedFeatures = findExcludedFeatures();

  public FeaturesResolver(final FeaturesService delegate) {
    try {
      for (Feature f : delegate.listFeatures()) {
        // record each feature under its version (ie. the NXRM version)
        this.featuresById.putIfAbsent(f.getId(), f);
        // ... and also the wildcard for fast no-version matching
        this.featuresById.putIfAbsent(f.getName() + VERSION_WILDCARD, f);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Problem listing available features", e);
    }
  }

  public Stream<BundleInfo> resolve(final Set<String> names) {
    return names.stream().flatMap(this::resolve);
  }

  public Stream<BundleInfo> resolve(final String id) {
    // use wildcard version if no version is given (callers may also use wildcard)
    Feature feature = featuresById.get(normalize(id));
    if (feature != null) {
      return resolve(feature);
    }
    else {
      log.warn("Missing feature {}", id);
      return Stream.of();
    }
  }

  public Stream<BundleInfo> resolve(final Feature feature) {
    if (feature != null) {
      String id = feature.getId();
      if (excludedFeatures.contains(id) || excludedFeatures.contains(feature.getName())) {
        log.info("Excluding feature {}", id);
      }
      else if (installedFeatures.add(id)) {
        log.debug("Resolving feature {}", id);
        return Stream.concat(
            feature.getDependencies()
                .stream()
                .flatMap(d -> resolve(d.getName() + '/' + d.getVersion())),
            feature.getBundles().stream());
      }
    }
    return Stream.of();
  }

  public boolean isInstalled(final String id) {
    return installedFeatures.contains(normalize(id));
  }

  private static String normalize(final String id) {
    return id.indexOf('/') < 0 ? id + VERSION_WILDCARD : id;
  }

  private static Set<String> findExcludedFeatures() {
    String excludedFeatures = getProperty("nexus-exclude-features", "");
    return new HashSet<>(asList(excludedFeatures.split("[\\s,]+")));
  }
}
