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
package org.sonatype.nexus.extender.modules;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.sonatype.nexus.common.app.FeatureFlag;

import org.eclipse.sisu.space.ClassFinder;
import org.eclipse.sisu.space.ClassSpace;
import org.eclipse.sisu.space.IndexedClassFinder;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterators.asEnumeration;
import static java.util.stream.StreamSupport.stream;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;

/**
 * Filters out components whose type or package is annotated with {@link FeatureFlag}
 * when the associated system property indicates that feature is currently disabled.
 *
 * @since 3.19
 */
public class FeatureFlaggedIndex
    implements ClassFinder
{
  private static final Logger log = LoggerFactory.getLogger(FeatureFlaggedIndex.class);

  private static final String PACKAGE_INFO = "package-info";

  private static final String NAMED_INDEX = "META-INF/sisu/javax.inject.Named";

  private static final IndexedClassFinder GLOBAL_INDEX = new IndexedClassFinder(NAMED_INDEX, true);

  private final Predicate<String> allowedFilter;

  /**
   * Filter out components in the given bundle whose feature-flag is currently disabled.
   */
  public static ClassFinder filterByFeatureFlag(final Bundle bundle) {
    String featureFlaggedClasses = bundle.getHeaders().get("Feature-Flagged");
    if (!isNullOrEmpty(featureFlaggedClasses)) {

      List<String> disabledPackages = new ArrayList<>();
      Set<String> disabledComponents = new HashSet<>();

      boolean needsFiltering = false;
      for (String clazzName : featureFlaggedClasses.split(",")) {
        if (isFeatureFlagDisabled(bundle, clazzName)) {
          int packageInfoIndex = clazzName.indexOf(PACKAGE_INFO);
          if (packageInfoIndex > 0) {
            disabledPackages.add(clazzName.substring(0, packageInfoIndex));
            needsFiltering = true;
          }
          else {
            disabledComponents.add(clazzName);
            needsFiltering = true;
          }
        }
      }

      if (needsFiltering) {
        Predicate<String> allowedFilter = clazzName -> !disabledComponents.contains(clazzName);
        for (String packageName : disabledPackages) {
          allowedFilter = allowedFilter.and(clazzName -> !clazzName.startsWith(packageName));
        }
        return new FeatureFlaggedIndex(allowedFilter);
      }
    }

    return GLOBAL_INDEX;
  }

  private static boolean isFeatureFlagDisabled(final Bundle bundle, final String clazzName) {
    try {
      Class<?> clazz = bundle.loadClass(clazzName);
      FeatureFlag flag = clazz.getAnnotation(FeatureFlag.class);
      return !getBoolean(flag.name(), flag.enabledByDefault());
    }
    catch (Exception | LinkageError e) {
      log.warn("Problem checking feature-flag for {}", clazzName, e);
      return false;
    }
  }

  private FeatureFlaggedIndex(final Predicate<String> allowedFilter) {
    this.allowedFilter = checkNotNull(allowedFilter);
  }

  @Override
  public Enumeration<URL> findClasses(final ClassSpace space) {
    return asEnumeration(
        stream(GLOBAL_INDEX.indexedNames(space).spliterator(), false)
            .filter(allowedFilter)
            .map(name -> space.getResource(name.replace('.', '/') + ".class"))
            .filter(Objects::nonNull)
            .iterator());
  }
}
