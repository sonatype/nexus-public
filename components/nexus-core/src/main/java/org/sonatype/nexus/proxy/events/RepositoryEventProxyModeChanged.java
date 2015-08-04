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
package org.sonatype.nexus.proxy.events;

import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

/**
 * The event fired when a repository's proxy mode changed. This event is fired on <b>transitions only</b>, when
 * ProxyMode of ProxyRepository is actually being changed (oldMode != newMode).
 *
 * @author cstamas
 */
public class RepositoryEventProxyModeChanged
    extends RepositoryEventProxyMode
{
  public RepositoryEventProxyModeChanged(final ProxyRepository repository, final ProxyMode oldProxyMode,
                                         final ProxyMode newProxyMode, final Throwable cause)
  {
    super(repository, oldProxyMode, newProxyMode, cause);
  }
}
