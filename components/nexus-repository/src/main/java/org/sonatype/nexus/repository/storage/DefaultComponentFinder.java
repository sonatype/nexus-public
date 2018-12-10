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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @since 3.14
 */
@Singleton
@Named
public class DefaultComponentFinder
    implements ComponentFinder
{
  public static final String DEFAULT_COMPONENT_FINDER_KEY = "default";

  @Override
  public List<Component> findMatchingComponents(final Repository repository,
                                                final String componentId,
                                                final String componentGroup,
                                                final String componentName,
                                                final String componentVersion)
  {
    if (null == repository || null == componentId || componentId.isEmpty()) {
      return emptyList();
    } else {
      try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
        storageTx.begin();
        Component component = storageTx.findComponent(new DetachedEntityId(componentId));
        if (component != null) {
          return singletonList(component);
        }
        else {
          return emptyList();
        }
      }
    }
  }
}
