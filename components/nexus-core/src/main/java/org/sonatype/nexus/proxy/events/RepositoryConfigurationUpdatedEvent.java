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

import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Fired when a repository configuration changed and is applied (not rollbacked).
 *
 * @author cstamas
 */
public class RepositoryConfigurationUpdatedEvent
    extends RepositoryEvent
{
  private boolean localUrlChanged = false;

  private boolean remoteUrlChanged = false;

  private boolean downloadRemoteIndexEnabled = false;

  private boolean madeSearchable = false;

  private boolean localStatusChanged = false;

  public RepositoryConfigurationUpdatedEvent(Repository repository) {
    super(repository);
  }

  public boolean isLocalUrlChanged() {
    return localUrlChanged;
  }

  public boolean isRemoteUrlChanged() {
    return remoteUrlChanged;
  }

  public boolean isDownloadRemoteIndexEnabled() {
    return downloadRemoteIndexEnabled;
  }

  public boolean isLocalStatusChanged() {
    return localStatusChanged;
  }

  public void setLocalUrlChanged(boolean localUrlChanged) {
    this.localUrlChanged = localUrlChanged;
  }

  public void setRemoteUrlChanged(boolean remoteUrlChanged) {
    this.remoteUrlChanged = remoteUrlChanged;
  }

  public void setDownloadRemoteIndexEnabled(boolean downloadRemoteIndexEnabled) {
    this.downloadRemoteIndexEnabled = downloadRemoteIndexEnabled;
  }

  public boolean isMadeSearchable() {
    return madeSearchable;
  }

  public void setMadeSearchable(boolean madeSearchable) {
    this.madeSearchable = madeSearchable;
  }

  public void setLocalStatusChanged(boolean localStatusChanged) {
    this.localStatusChanged = localStatusChanged;
  }

}
