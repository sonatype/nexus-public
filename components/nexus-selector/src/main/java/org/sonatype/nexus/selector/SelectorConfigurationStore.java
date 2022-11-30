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
package org.sonatype.nexus.selector;

import java.util.List;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.entity.EntityId;

/**
 * {@link SelectorConfiguration} store.
 *
 * since 3.0
 */
public interface SelectorConfigurationStore
    extends Lifecycle
{
  /**
   * @return all configuration
   */
  List<SelectorConfiguration> browse();

  /**
   * @return configuration by id
   */
  SelectorConfiguration read(EntityId entityId);

  /**
   * @since 3.6
   * @return configuration by name
   */
  SelectorConfiguration getByName(String name);

  /**
   * Persist a new configuration.
   */
  void create(SelectorConfiguration configuration);

  /**
   * Persist an existing configuration.
   */
  void update(SelectorConfiguration configuration);

  /**
   * Delete an existing configuration.
   */
  void delete(SelectorConfiguration configuration);

  SelectorConfiguration newSelectorConfiguration();
}
