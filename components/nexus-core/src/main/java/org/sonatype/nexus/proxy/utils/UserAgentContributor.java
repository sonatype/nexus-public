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
package org.sonatype.nexus.proxy.utils;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * Classes implementing this interface are injected into the {@link UserAgentBuilder} and asked
 * for a custom string to append to the user agent.
 *
 * @since 2.1
 */
public interface UserAgentContributor
{

  /**
   * Return a string to append to the user agent.
   *
   * @param ctx        The remote storage settings.
   * @param repository The proxy repository that the user agent will be used for. May be null if the user agent is
   *                   built in a global context.
   * @return The string to append to the user agent.
   */
  String getUserAgent(RemoteStorageContext ctx, @Nullable ProxyRepository repository);

}
