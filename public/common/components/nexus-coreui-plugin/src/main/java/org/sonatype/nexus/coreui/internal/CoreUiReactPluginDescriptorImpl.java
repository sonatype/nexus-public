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
package org.sonatype.nexus.coreui.internal;

import java.util.List;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.ui.UiPluginDescriptor;
import org.sonatype.nexus.ui.UiUtil;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * {@link UiPluginDescriptor} for {@code nexus-coreui-plugin} react code.
 *
 * @since 3.22
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100) // after nexus-rapture
public class CoreUiReactPluginDescriptorImpl
    implements UiPluginDescriptor
{
  private final List<String> scripts;

  private final List<String> debugScripts;

  private final List<String> styles;

  @Autowired
  public CoreUiReactPluginDescriptorImpl(final UiUtil uiUtil) {
    scripts = List.of(uiUtil.getPathForFile("nexus-coreui-bundle.js"));
    debugScripts = List.of(uiUtil.getPathForFile("nexus-coreui-bundle.debug.js"));
    styles = List.of(uiUtil.getPathForFile("nexus-coreui-bundle.css"));
  }

  @Override
  public String getName() {
    return "nexus-coreui-plugin";
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
