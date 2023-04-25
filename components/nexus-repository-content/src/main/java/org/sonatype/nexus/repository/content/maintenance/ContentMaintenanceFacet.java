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
package org.sonatype.nexus.repository.content.maintenance;

import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

/**
 * @since 3.24
 */
@Facet.Exposed
public interface ContentMaintenanceFacet
    extends Facet
{
  /**
   * Deletes a component from the repository; the format may decide to trigger additional deletes/updates.
   *
   * @return paths(s) of deleted asset(s)
   */
  Set<String> deleteComponent(Component component);

  /**
   * Deletes an asset from the repository; the format may decide to trigger additional deletes/updates.
   *
   * @return path(s) of deleted asset(s)
   */
  Set<String> deleteAsset(Asset asset);

  /**
   * Delete a batch of components.
   *
   * @param components the components to delete
   * @return number of components purged
   * @since 3.29
   */
  int deleteComponents(Stream<FluentComponent> components);
}
