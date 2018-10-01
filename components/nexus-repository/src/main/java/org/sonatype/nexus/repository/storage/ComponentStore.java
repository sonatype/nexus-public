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

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;

/**
 * Store providing access to components.
 *
 * @since 3.6
 */
public interface ComponentStore
{
  /**
   * @param id
   * @return the component for the id
   */
  Component read(EntityId id);

  /**
   * Finds and returns all the components that match the specified parameters.
   * @param repository
   * @param group
   * @param name
   * @param formatAttributes
   * @return All the components that match the specified parameters
   *
   * @since 3.14
   */
  List<Component> getAllMatchingComponents(final Repository repository,
                                           final String group,
                                           final String name,
                                           final Map<String, String> formatAttributes);
}
