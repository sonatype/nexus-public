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
package org.sonatype.nexus.common.groovy;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.thread.TcclBlock;

import org.apache.groovy.json.FastStringService;
import org.apache.groovy.json.internal.FastStringUtils;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SERVICES;

/**
 * Sets the FastStringService context-class-loader to the current thread class-loader
 */
@Named
@Singleton
@ManagedLifecycle(phase = SERVICES)
public class FastStringServiceLoader
    extends StateGuardLifecycleSupport
{
  @Override
  protected void doStart() throws Exception {
    // HACK: workaround to 'Unable to load FastStringService' groovy issue
    try (TcclBlock ignored = TcclBlock.begin(FastStringService.class)) {
      FastStringUtils.toCharArray("ab");
    }
  }
}
