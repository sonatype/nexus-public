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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;

public abstract class AbstractNexusTestEnvironment
    extends NexusAppTestSupport
{

  private RepositoryItemUidFactory repositoryItemUidFactory;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    repositoryItemUidFactory = lookup(RepositoryItemUidFactory.class);
    // rebuild cache
    lookup(RepositoryItemUidAttributeManager.class).reset();
  }

  protected RepositoryItemUidFactory getRepositoryItemUidFactory() {
    return repositoryItemUidFactory;
  }

}
