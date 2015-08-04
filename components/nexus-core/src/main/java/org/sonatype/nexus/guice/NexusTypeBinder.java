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
package org.sonatype.nexus.guice;

import org.sonatype.gossip.Level;
import org.sonatype.nexus.plugins.DefaultNexusPluginManager;
import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.repository.Repository;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.sisu.inject.DeferredClass;
import org.eclipse.sisu.plexus.ComponentImpl;
import org.eclipse.sisu.plexus.PlexusTypeBinder;
import org.eclipse.sisu.plexus.PlexusTypeListener;
import org.eclipse.sisu.plexus.PlexusTypeVisitor;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.SpaceVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts the usual Plexus binding process to handle Nexus {@link RepositoryType} semantics.
 * 
 * @since 2.8
 */
public final class NexusTypeBinder
    implements PlexusTypeListener
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  public static final SpaceModule.Strategy STRATEGY = new SpaceModule.Strategy()
  {
    public SpaceVisitor visitor(final Binder binder) {
      return new PlexusTypeVisitor(new NexusTypeBinder(binder, new PlexusTypeBinder(binder)));
    }
  };

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private static final Logger log = LoggerFactory.getLogger(NexusTypeBinder.class);

  private final Binder binder;

  private final PlexusTypeListener delegate;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  /**
   * @param binder Guice binder
   * @param delegate Original Plexus listener
   */
  NexusTypeBinder(final Binder binder, final PlexusTypeListener delegate) {
    this.binder = binder;
    this.delegate = delegate;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  /**
   * Adds {@link RepositoryType} semantics on top of JSR330 semantics.
   * 
   * @param qualifier always null, removed in refactored eclipse codebase; do not rely on it
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void hear(final Class<?> implementation, final Object source) {
    Class role = getRepositoryRole(implementation);
    if (role != null) {
      String hint = getRepositoryHint(implementation);

      // short-circuit the usual JSR330 binding to just use role+hint
      binder.bind(Key.get(role, Names.named(hint))).to(implementation);

      addRepositoryTypeDescriptor(role, hint);
    }
    else {
      delegate.hear(implementation, source);
    }
  }

  /**
   * Adds {@link RepositoryType} semantics on top of Plexus semantics.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void hear(Component component, final DeferredClass<?> implementation, final Object source) {
    Class role = getRepositoryRole(implementation.load());
    if (role != null) {
      if (StringUtils.isBlank(component.hint())) {
        // if someone forgot to set the repository's hint then use the fully-qualified classname
        component = new ComponentImpl(component.role(), getRepositoryHint(implementation.load()),
            component.instantiationStrategy(), component.description());
      }
      addRepositoryTypeDescriptor(role, component.hint());
    }

    // log a warning if a plexus component is found. special handling for org.apache.maven.{index|model}
    // ... which are required plexus components we can not effectively port to jsr-330 ATM.
    // log these as debug to avoid warnings which users can not do anything about.
    String implName = implementation.getName();
    Level level = Level.WARN;
    if (implName.startsWith("org.apache.maven.model") || implName.startsWith("org.apache.maven.index")) {
      level = Level.DEBUG;
    }
    level.log(log, "Found legacy plexus component: {}", log.isDebugEnabled() ? implementation : implName);

    delegate.hear(component, implementation, source);
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  /**
   * Scans the implementation's declared interfaces for one annotated with {@link RepositoryType}.
   */
  private static Class<?> getRepositoryRole(final Class<?> implementation) {
    for (Class<?> api : implementation.getInterfaces()) {
      if (api.isAnnotationPresent(RepositoryType.class)) {
        return api;
      }
    }
    return null;
  }

  /**
   * Checks for both kinds of @Named; uses fully-qualified classname if name is missing or blank.
   */
  private static String getRepositoryHint(final Class<?> implementation) {
    String name = null;
    if (implementation.isAnnotationPresent(javax.inject.Named.class)) {
      name = implementation.getAnnotation(javax.inject.Named.class).value();
    }
    else if (implementation.isAnnotationPresent(com.google.inject.name.Named.class)) {
      name = implementation.getAnnotation(com.google.inject.name.Named.class).value();
    }
    return StringUtils.isNotBlank(name) ? name : implementation.getName();
  }

  /**
   * Records a descriptor for the given repository role+hint.
   * 
   * @see {@link DefaultNexusPluginManager#createPluginInjector}
   */
  private void addRepositoryTypeDescriptor(final Class<? extends Repository> role, final String hint) {
    RepositoryType rt = role.getAnnotation(RepositoryType.class);
    binder.bind(RepositoryTypeDescriptor.class).annotatedWith(Names.named(role + ":" + hint))
        .toInstance(new RepositoryTypeDescriptor(role, hint, rt.pathPrefix(), rt.repositoryMaxInstanceCount()));
  }
}
