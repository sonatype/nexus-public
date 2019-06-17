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
package org.sonatype.nexus.repository.npm.internal;

import org.sonatype.nexus.repository.storage.Asset;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event for requesting an upgrade on the revision field of an NPM package root.
 *
 * @since 3.17
 */
public class NpmRevisionUpgradeRequestEvent
{
  private Asset packageRootAsset;

  private String revision;

  public NpmRevisionUpgradeRequestEvent(final Asset packageRootAsset, final String revision) {
    this.packageRootAsset = checkNotNull(packageRootAsset);
    this.revision = checkNotNull(revision);
  }

  public Asset getPackageRootAsset() {
    return packageRootAsset;
  }

  public String getRevision() {
    return revision;
  }
}
