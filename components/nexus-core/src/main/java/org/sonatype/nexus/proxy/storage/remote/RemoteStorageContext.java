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
package org.sonatype.nexus.proxy.storage.remote;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.StorageContext;

/**
 * The remote storage settings and context.
 * 
 * @author cstamas
 */
public interface RemoteStorageContext
    extends StorageContext
{
  /**
   * Returns {@code true} if this context has an instance of {@link RemoteConnectionSettings}.
   */
  boolean hasRemoteConnectionSettings();

  /**
   * Returns the {@link RemoteConnectionSettings}, that comes from this or parent remote storage context. If reason for
   * retrieving remote connection settings is mutating it, caller has to be cautious to not perform this call blindly,
   * as remote connection settings coming from parent will be read only, throwing {@link IllegalStateException} in case
   * setter invocation is attempted on it.
   */
  RemoteConnectionSettings getRemoteConnectionSettings();

  /**
   * Sets the {@link RemoteConnectionSettings} in this context, parent is unchanged. The newly set value will override
   * the value from parent.
   */
  void setRemoteConnectionSettings(RemoteConnectionSettings settings);

  /**
   * Removes the {@link RemoteConnectionSettings} from this context, parent is unchanged. The removal makes the parent
   * value visible (in effect) again.
   */
  void removeRemoteConnectionSettings();

  /**
   * Returns {@code true} if this context has instance of {@link RemoteAuthenticationSettings}.
   */
  boolean hasRemoteAuthenticationSettings();

  /**
   * Returns the {@link RemoteAuthenticationSettings} from this context. or {@code null} if it does not exists in this
   * context.
   */
  RemoteAuthenticationSettings getRemoteAuthenticationSettings();

  /**
   * Sets the {@link RemoteAuthenticationSettings} in this context, parent is unchanged.
   */
  void setRemoteAuthenticationSettings(RemoteAuthenticationSettings settings);

  /**
   * Removes the {@link RemoteAuthenticationSettings} from this context, parent is unchanged.
   */
  void removeRemoteAuthenticationSettings();

  /**
   * Returns {@code true} if this context has an instance of {@link RemoteProxySettings}. Note: since NEXUS-5690 is
   * implemented (in 2.7), this method is used only on global {@link RemoteStorageContext}, see
   * {@link NexusConfiguration#getGlobalRemoteStorageContext()}.
   * 
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5690">NEXUS-5690 Remove per repository http proxy
   *      configuration</a>
   */
  boolean hasRemoteProxySettings();

  /**
   * Returns the {@link RemoteProxySettings}, that comes from this or parent remote storage context (globally defined
   * HTTP Proxy). Note: since NEXUS-5690 is implemented (in 2.7), this method is used only on global
   * {@link RemoteStorageContext}, see {@link NexusConfiguration#getGlobalRemoteStorageContext()}.
   * 
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5690">NEXUS-5690 Remove per repository http proxy
   *      configuration</a>
   */
  RemoteProxySettings getRemoteProxySettings();

  /**
   * Sets the {@link RemoteProxySettings} in this context, parent is unchanged. The newly set value will override the
   * value from parent. Note: since NEXUS-5690 is implemented (in 2.7), this method is used only on global
   * {@link RemoteStorageContext}, see {@link NexusConfiguration#getGlobalRemoteStorageContext()}.
   * 
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5690">NEXUS-5690 Remove per repository http proxy
   *      configuration</a>
   */
  void setRemoteProxySettings(RemoteProxySettings settings);

  /**
   * Removes the {@link RemoteProxySettings} from this context, parent is unchanged. Note: since NEXUS-5690 is
   * implemented (in 2.7), this method is used only on global {@link RemoteStorageContext},
   * see {@link NexusConfiguration#getGlobalRemoteStorageContext()}.
   * 
   * @see <a href="https://issues.sonatype.org/browse/NEXUS-5690">NEXUS-5690 Remove per repository http proxy
   *      configuration</a>
   */
  void removeRemoteProxySettings();

}
