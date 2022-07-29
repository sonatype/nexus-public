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
package org.sonatype.nexus.common.cooperation2;

import java.io.IOException;
import java.util.Map;

/**
 * @since 3.41
 */
public interface Cooperation2
{
  /**
   * Sets the function that is used to perform the work. Depending on the thread, or the node chosen to perform the
   * co-operation there is no guarantee that the function will be called and the {@link Cooperation2} built may return
   * the result from another thread.
   *
   * @param workFunction a function which can be used to perform the operation
   * @param <RET> the type of the return value
   * @return the resulting builder
   */
  <RET> Builder<RET> on(IOCall<RET> workFunction);

  /**
   * @return number of threads cooperating per request-key.
   */
  Map<String, Integer> getThreadCountPerKey();

  interface Builder<RET>
  {
    /**
     * Set an {@link IOCheck} that can be used to verify that the work of this cooperation has already been performed.
     * If this check is not provided it is assumed the work is not complete.
     *
     * This function is not guaranteed to be called (if another thread, or node is the lead).
     *
     * Exceptions thrown by this supplier will terminate the co-operation.
     *
     * @param checkFunction a supplier which can be used to determine whether the work required for a co-operation key
     *          has been completed.
     * @return the resulting builder
     */
    Builder<RET> checkFunction(IOCheck<RET> checkFunction);

    /**
     * The co-operation may (depending on implementation) perform the work if concurrency controls timeout.
     */
    Builder<RET> performWorkOnFail(final boolean performWorkOnFail);

    /**
     * Perform the co-operation. (Note implementations may not execute the work asynchronously)
     */
    RET cooperate(String action, String... scopes) throws IOException;
  }
}
