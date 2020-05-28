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

import org.sonatype.nexus.repository.content.Component;

/**
 * Fluent API to create/find an asset; at this point we already know the asset path.
 *
 * @since 3.21
 */
public interface FluentAssetBuilder
{
  /**
   * Continue building the asset using the given kind.
   *
   * @since 3.24
   */
  FluentAssetBuilder kind(String kind);

  /**
   * Continue building the asset using the given owning component.
   */
  FluentAssetBuilder component(Component component);

  /**
   * Gets the full asset using the details built so far; if it doesn't exist then it is created.
   */
  FluentAsset getOrCreate();

  /**
   * Find if an asset exists using the details built so far.
   */
  Optional<FluentAsset> find();
}
