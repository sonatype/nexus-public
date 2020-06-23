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
package org.sonatype.nexus.repository.content.fluent;

import java.util.Collection;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.content.Component;

/**
 * Fluent API for components.
 *
 * @since 3.21
 */
public interface FluentComponents
{
  /**
   * Start building a component, beginning with its name.
   */
  FluentComponentBuilder name(String name);

  /**
   * Interact with an existing component.
   */
  FluentComponent with(Component component);

  /**
   * Count all components in the repository.
   */
  int count();

  /**
   * Browse through all components in the repository.
   */
  default Continuation<FluentComponent> browse(int limit, @Nullable String continuationToken) {
    return browse(null, limit, continuationToken);
  }

  /**
   * Browse through all components in the repository by kind.
   */
  Continuation<FluentComponent> browse(@Nullable String kind, int limit, @Nullable String continuationToken);

  /**
   * List all namespaces of components in the repository.
   */
  Collection<String> namespaces();

  /**
   * List all names of components under the given namespace in the repository.
   */
  Collection<String> names(String namespace);

  /**
   * List all versions of components with the given namespace and name in the repository.
   */
  Collection<String> versions(String namespace, String name);
}
