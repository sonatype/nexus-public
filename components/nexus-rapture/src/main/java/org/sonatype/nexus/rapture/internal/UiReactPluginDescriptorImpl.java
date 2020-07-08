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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ui.UiPluginDescriptor;
import org.sonatype.nexus.ui.UiUtil;

import org.eclipse.sisu.Priority;
import org.eclipse.sisu.space.ClassSpace;

import static java.util.Arrays.asList;

/**
 * {@link UiPluginDescriptor} for {@code nexus-rapture} react code.
 *
 * @since 3.25
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE) // always load first
public class UiReactPluginDescriptorImpl
    implements UiPluginDescriptor
{
  private final List<String> scripts;

  private final List<String> debugScripts;

  private final List<String> styles;

  @Inject
  public UiReactPluginDescriptorImpl(final ClassSpace space) {
    scripts = asList(UiUtil.getPathForFile("nexus-rapture-bundle.js", space));
    debugScripts = asList(UiUtil.getPathForFile("nexus-rapture-bundle.debug.js", space));
    styles = asList(UiUtil.getPathForFile("nexus-rapture-bundle.css", space));
  }

  @Override
  public String getName() {
    return "nexus-rapture";
  }

  @Nullable
  @Override
  public List<String> getScripts(final boolean isDebug) {
    return isDebug ? debugScripts : scripts;
  }

  @Nullable
  @Override
  public List<String> getStyles() {
    return styles;
  }
}
