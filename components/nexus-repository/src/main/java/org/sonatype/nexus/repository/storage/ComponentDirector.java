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
package org.sonatype.nexus.repository.storage;

import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.Repository;

/**
 * Component management director.
 *
 * @since 3.10
 */
public interface ComponentDirector
{
  /**
   * The calling algorithm only moves the {@code component} and the directly attached {@code assets}.
   *
   * This is a hook that allows format implementers to handle situations where additional work needs to be done before
   * a component gets moved. The caller will provide the {@code component} being moved along with any {@code assets}
   * attached to that {@code component}. The {@code source} and {@code destination} repositories are also provided for
   * context.
   *
   * This hook may be required in situations where there are unattached assets that may also need to be moved or copied.
   *
   * @since 3.11
   */
  default Component beforeMove(final Component component,
                               final List<Asset> assets,
                               final Repository source,
                               final Repository destination)
  {
    return component;
  }

  /**
   * This is a hook that allows format implementors to handle situations where additional work is required after each
   * individual {@code component} is moved to the {@code destination} repository.
   *
   * One example of this is when some repository-spanning metadata needs to be updated after an
   * individual component is moved.
   */
  default Component afterMove(final Component component, final Repository destination) {
    return component;
  }

  /**
   * This is a hook that allows format implementors to decide if a given {@code destination} repository is an
   * acceptable target for a move operation.
   */
  default boolean allowMoveTo(final Repository destination) {
    return false;
  }

  /**
   * This is a hook that allows format implementors to decide if a given {@code component} is allowed to be moved to
   * the {@code destination} repository.
   */
  default boolean allowMoveTo(final Component component, final Repository destination) {
    return false;
  }

  /**
   * This is a hook that allows format implementors to decide if a given {@code source} repository is an
   * acceptable source for a move operation.
   */
  default boolean allowMoveFrom(final Repository source) {
    return false;
  }

  /**
   * This is a hook that allows format implementors to handle situations where additional work is required all of
   * the {@code components} have been moved to the {@code destination} repository. Each component is represented by
   * a map that contains entries for the group, name, and version if they are available.
   *
   * One example of this is when some repository-spanning metadata can be updated for a set of components after
   * they are moved.
   */
  default void afterMove(final List<Map<String, String>> components, final Repository destination) {
    // no-op
  }
}
