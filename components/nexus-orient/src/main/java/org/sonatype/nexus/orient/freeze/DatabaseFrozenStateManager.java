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
package org.sonatype.nexus.orient.freeze;

import java.util.List;

/**
 * Saves the database frozen state.
 *
 * @since 3.3
 */
public interface DatabaseFrozenStateManager
{
  /**
   * @return the current state
   */
  List<FreezeRequest> getState();

  /**
   * @param request the {@link FreezeRequest} to remove from state
   * @return true if the request was removed, false otherwise
   */
  boolean remove(FreezeRequest request);

  /**
   * @param request the {@link FreezeRequest} to add to the state.
   * @return the request if added successfully, null otherwise
   */
  FreezeRequest add(FreezeRequest request);
}
