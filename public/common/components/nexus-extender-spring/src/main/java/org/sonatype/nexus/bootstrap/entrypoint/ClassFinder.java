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
package org.sonatype.nexus.bootstrap.entrypoint;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.bootstrap.entrypoint.ClassFilter.ClassType.CLASS;
import static org.sonatype.nexus.bootstrap.entrypoint.ClassFilter.ClassType.INTERFACE;

/**
 * Finds classes in the classpath based on a given Classfilter. This is NOT
 * using dependency injection, and should only be used when DI is not an option
 * (Like when you need to load a bunch of interface classes dynamically, where interfaces
 * aren't something generally managed by DI
 */
@Component
public class ClassFinder
{
  private static final Logger LOG = LoggerFactory.getLogger(ClassFinder.class);

  public Set<Class<?>> find(final ClassFilter classFilter) {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);

    // Add a filter to find all classes.
    TypeFilter interfaceFilter = (metadataReader, metadataReaderFactory) -> true;
    scanner.addIncludeFilter(interfaceFilter);

    Set<BeanDefinition> candidates = scanner.findCandidateComponents(classFilter.getBasePackage());

    return candidates
        .stream()
        .map(candidate -> toClassIfMatches(candidate, classFilter))
        .filter(Objects::nonNull)
        .collect(toSet());
  }

  private Class<?> toClassIfMatches(final BeanDefinition beanDefinition, final ClassFilter classFilter) {
    if (beanDefinition.getBeanClassName() == null) {
      LOG.error("Cannot lookup null classname");
      return null;
    }

    if (classFilter.getClassNameRegex() != null &&
        !beanDefinition.getBeanClassName().matches(classFilter.getClassNameRegex())) {
      LOG.trace(
          "Class {} does not match the regex {}",
          beanDefinition.getBeanClassName(),
          classFilter.getClassNameRegex());
      return null;
    }
    try {
      Class<?> clazz = Class.forName(beanDefinition.getBeanClassName());
      if (classFilter.getClassType() != null &&
          ((CLASS.equals(classFilter.getClassType()) && clazz.isInterface()) ||
              (INTERFACE.equals(classFilter.getClassType()) && clazz.isInterface()))) {
        LOG.trace(
            "Class {} does not match the requested class type {}",
            beanDefinition.getBeanClassName(),
            classFilter.getClassType());
        return null;
      }
      return clazz;
    }
    catch (ClassNotFoundException e) {
      LOG.error("Failed to load class {}", beanDefinition.getBeanClassName(), e);
      return null;
    }
  }
}
