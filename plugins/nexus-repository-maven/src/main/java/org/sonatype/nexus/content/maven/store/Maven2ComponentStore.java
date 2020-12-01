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

package org.sonatype.nexus.content.maven.store;

import javax.inject.Inject;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

/**
 * @since 3.next
 */
public class Maven2ComponentStore
    extends ComponentStore<Maven2ComponentDAO>
{
  @Inject
  public Maven2ComponentStore(final DataSessionSupplier sessionSupplier,
                              @Assisted final String storeName)
  {
    super(sessionSupplier, storeName, Maven2ComponentDAO.class);
  }

  /**
   * Updates the maven base_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateBaseVersion(final Maven2ComponentData component)
  {
    dao().updateBaseVersion(component);
  }
}
