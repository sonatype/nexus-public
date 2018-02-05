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
package org.sonatype.nexus.security;

import javax.annotation.Nullable;

/**
 * Manages and provides {@link ClientInfo} instances.
 *
 * @author cstamas
 * @since 2.1
 */
public interface ClientInfoProvider
{
  /**
   * Returns the {@link ClientInfo} for current thread. It will be non-null if this thread is a REST (or better HTTP)
   * Request processing thread, and {@code null} if this is a non REST Request processing thread (like a scheduled
   * task threads are).
   *
   * @return the current thread's {@link ClientInfo} or {@code null} if none available.
   */
  @Nullable
  ClientInfo getCurrentThreadClientInfo();
}
