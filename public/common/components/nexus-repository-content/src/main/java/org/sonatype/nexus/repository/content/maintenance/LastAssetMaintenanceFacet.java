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

import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * {@link ContentMaintenanceFacet} for formats where components should be deleted along with their last asset.
 *
 * @since 3.26
 */
@Primary
@Scope(SCOPE_PROTOTYPE)
@org.springframework.stereotype.Component
public class LastAssetMaintenanceFacet
    extends DefaultMaintenanceFacet
{
  @Override
  public Set<String> deleteAsset(final Asset asset) {
    Optional<Component> component = asset.component();
    if (component.isPresent()) {
      FluentComponent componentToDelete = contentFacet().components().with(component.get());
      if (componentToDelete.assets().size() <= 1) {
        return deleteComponent(componentToDelete);
      }
    }

    return super.deleteAsset(asset);
  }

  @Override
  public boolean cascadeSoftDeleteToComponent(final Asset asset, final boolean lastRemainingAsset) {
    // Delete the component only along with its last asset (see deleteAsset).
    return lastRemainingAsset;
  }
}
