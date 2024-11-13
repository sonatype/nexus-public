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

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;

/**
 * Fluent API for a particular component.
 *
 * @since 3.21
 */
public interface FluentComponent
    extends Component, FluentAttributes<FluentComponent>
{
  /**
   * The repository containing this component.
   *
   * @since 3.24
   */
  Repository repository();

  /**
   * Start building an asset for this component, beginning with its path.
   */
  FluentAssetBuilder asset(String path);

  /**
   * List the assets under this component; returns an immutable collection.
   */
  Collection<FluentAsset> assets();

  /**
   * List the assets under this component; returns an immutable collection.
   */
  Collection<FluentAsset> assets(boolean useCache);

  /**
   * Update this component to have the given kind.
   *
   * @since 3.25
   */
  FluentComponent kind(String kind);

  /**
   * Deletes this component.
   */
  boolean delete();
}
