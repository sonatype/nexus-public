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
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.content.Component;

/**
 * Fluent API for components.
 *
 * @since 3.21
 */
public interface FluentComponents
    extends FluentQuery<FluentComponent>
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
   * Query components that have the given kind.
   *
   * @since 3.26
   */
  FluentQuery<FluentComponent> byKind(String kind);

  /**
   * Query components that match the given filter.
   * <p>
   * A filter parameter of {@code foo} should be referred to in the filter string as <code>#{filterParams.foo}</code>
   * <p>
   * <b>WARNING</b> the filter string is appended to the query and should only contain trusted content!
   *
   * @since 3.26
   */
  FluentQuery<FluentComponent> byFilter(String filter, Map<String, Object> filterParams);

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

  /**
   * Find if a component exists that has the given external id.
   *
   * @since 3.26
   */
  Optional<FluentComponent> find(EntityId externalId);
}
