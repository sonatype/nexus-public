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
package org.sonatype.nexus.rapture.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.UiPluginDescriptor;
import org.sonatype.nexus.rapture.UiPluginDescriptorSupport;

import org.eclipse.sisu.Priority;

import static java.util.Arrays.asList;

/**
 * Rapture {@link UiPluginDescriptor} for {@code nexus-rapture}.
 *
 * @since 3.0
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE) // always load first
public class UiPluginDescriptorImpl
    extends UiPluginDescriptorSupport
{
  private final List<String> scripts;

  private final List<String> debugScripts;

  @Inject
  public UiPluginDescriptorImpl() {
    super("nexus-rapture");
    setConfigClassName("NX.app.PluginConfig");
    scripts = asList("/static/frontend-bundle.js");
    debugScripts = asList("/static/frontend-bundle-debug.js");
  }

  @Override
  public List<String> getScripts(final boolean isDebug) {
    return isDebug ? debugScripts : scripts;
  }
}
