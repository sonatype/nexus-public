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

import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteAuthenticationSettings;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.AbstractStorageContext;
import org.sonatype.nexus.proxy.storage.StorageContext;

/**
 * The default remote storage context.
 * 
 * @author cstamas
 */
public class DefaultRemoteStorageContext
    extends AbstractStorageContext
    implements RemoteStorageContext
{
  public DefaultRemoteStorageContext(final StorageContext parent) {
    super(parent);
  }

  @Override
  public boolean hasRemoteAuthenticationSettings() {
    return hasContextObject(RemoteAuthenticationSettings.class.getName());
  }

  @Override
  public RemoteAuthenticationSettings getRemoteAuthenticationSettings() {
    return (RemoteAuthenticationSettings) getContextObject(RemoteAuthenticationSettings.class.getName());
  }

  @Override
  public void setRemoteAuthenticationSettings(RemoteAuthenticationSettings settings) {
    putContextObject(RemoteAuthenticationSettings.class.getName(), settings);
  }

  @Override
  public void removeRemoteAuthenticationSettings() {
    removeContextObject(RemoteAuthenticationSettings.class.getName());
  }

  @Override
  public boolean hasRemoteConnectionSettings() {
    return hasContextObject(RemoteConnectionSettings.class.getName());
  }

  @Override
  public RemoteConnectionSettings getRemoteConnectionSettings() {
    final RemoteConnectionSettings remoteConnectionSettings = (RemoteConnectionSettings) getContextObject(RemoteConnectionSettings.class.getName());
    if (hasContextObject(RemoteConnectionSettings.class.getName())) {
      return remoteConnectionSettings;
    }
    else {
      return DefaultRemoteConnectionSettings.asReadOnly(remoteConnectionSettings);
    }
  }

  @Override
  public void setRemoteConnectionSettings(RemoteConnectionSettings settings) {
    putContextObject(RemoteConnectionSettings.class.getName(), settings);
  }

  @Override
  public void removeRemoteConnectionSettings() {
    removeContextObject(RemoteConnectionSettings.class.getName());
  }

  @Override
  public boolean hasRemoteProxySettings() {
    return hasContextObject(RemoteProxySettings.class.getName());
  }

  @Override
  public RemoteProxySettings getRemoteProxySettings() {
    return (RemoteProxySettings) getContextObject(RemoteProxySettings.class.getName());
  }

  @Override
  public void setRemoteProxySettings(RemoteProxySettings settings) {
    putContextObject(RemoteProxySettings.class.getName(), settings);
  }

  @Override
  public void removeRemoteProxySettings() {
    removeContextObject(RemoteProxySettings.class.getName());
  }

  // ==

  /**
   * Simple helper class to have boolean stored in context and not disturbing the update of it.
   */
  public static class BooleanFlagHolder
  {
    private Boolean flag = null;

    /**
     * Returns true only and if only flag is not null and has value Boolean.TRUE.
     */
    public boolean isFlag() {
      if (flag != null) {
        return flag;
      }
      else {
        return false;
      }
    }

    public boolean isNull() {
      return flag == null;
    }

    public void setFlag(Boolean flag) {
      this.flag = flag;
    }
  }
}
