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
package org.sonatype.nexus.blobstore.s3.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.UiPluginDescriptor;
import org.sonatype.nexus.rapture.UiPluginDescriptorSupport;

import org.eclipse.sisu.Priority;

/**
 * Rapture {@link UiPluginDescriptor} for {@code nexus-blobstore-s3}.
 *
 * @since 3.17
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE) // load early, no scripts to resolve only providing css/fonts
public class UiPluginDescriptorImpl
    extends UiPluginDescriptorSupport
{
  @Inject
  public UiPluginDescriptorImpl() {
    super("nexus-blobstore-s3");
    setHasStyle(false);
    setNamespace("NX.s3blobstore");
    setConfigClassName("NX.s3blobstore.app.PluginConfig");
  }
}
