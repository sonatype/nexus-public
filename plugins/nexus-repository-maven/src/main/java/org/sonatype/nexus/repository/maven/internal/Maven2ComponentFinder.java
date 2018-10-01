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
package org.sonatype.nexus.repository.maven.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.internal.search.Maven2SearchResultComponentGenerator;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentFinder;
import org.sonatype.nexus.repository.storage.ComponentStore;
import org.sonatype.nexus.repository.storage.DefaultComponentFinder;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.14
 */
@Singleton
@Named(Maven2Format.NAME)
public class Maven2ComponentFinder
    implements ComponentFinder
{
  @Inject
  ComponentStore componentStore;

  @Inject
  DefaultComponentFinder defaultComponentFinder;

  @Override
  public List<Component> findMatchingComponents(final Repository repository,
                                                final String componentId,
                                                final String componentGroup,
                                                final String componentName,
                                                final String componentVersion)
  {
    if (Maven2SearchResultComponentGenerator.isSnapshotId(componentId)) {
      checkNotNull(componentVersion);
      return componentStore.getAllMatchingComponents(repository, componentGroup,
          componentName, ImmutableMap.of("baseVersion", componentVersion));
    }
    else {
      return defaultComponentFinder.findMatchingComponents(repository, componentId,
          componentGroup, componentName, componentVersion);
    }
  }

}
