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
package org.sonatype.nexus.internal.node.datastore;

import org.sonatype.nexus.datastore.api.SingletonDataAccess;

/**
 * DAO for accessing the single stored node id. Not appropriate for clustered environments.
 *
 * @since 3.37
 */
public interface NodeIdDAO
    extends SingletonDataAccess<String>
{
  /**
   * Attempts to create the singleton nodeId, may fail with constraint violation if this has already been set.
   */
  void create(String nodeId);
}
