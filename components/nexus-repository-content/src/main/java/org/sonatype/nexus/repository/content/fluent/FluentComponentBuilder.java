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

import java.util.Optional;

/**
 * Fluent API to create/find a component; at this point we already know the component name.
 *
 * @since 3.21
 */
public interface FluentComponentBuilder
{
  /**
   * Continue building the component using the given namespace.
   */
  FluentComponentBuilder namespace(String namespace);

  /**
   * Continue building the component using the given kind.
   *
   * @since 3.25.0
   */
  FluentComponentBuilder kind(String kind);

  /**
   * Continue building the component using the given version.
   */
  FluentComponentBuilder version(String version);

  /**
   * Gets the full component using the details built so far; if it doesn't exist then it is created.
   */
  FluentComponent getOrCreate();

  /**
   * Find if a component exists using the details built so far.
   */
  Optional<FluentComponent> find();
}
