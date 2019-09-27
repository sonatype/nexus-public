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
package org.sonatype.nexus.security.internal;

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
 * Rapture {@link UiPluginDescriptor} for {@code nexus-coreui-plugin}.
 *
 * @since 3.0
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE - 100) // after nexus-rapture
public class SecurityUiPluginDescriptor
    implements UiPluginDescriptor
{
  private final List<String> scripts;

  private final List<String> debugScripts;

  private final List<String> styles;

  @Inject
  public SecurityUiPluginDescriptor(final ClassSpace space) {

    scripts = asList(UiUtil.getHashedFilename("nexus-security-bundle.js", space));
    debugScripts = asList(UiUtil.getHashedFilename("nexus-security-bundle.debug.js", space));
    styles = asList(UiUtil.getHashedFilename("nexus-security-bundle.css", space));
  }

  @Override
  public String getPluginId() {
    return "nexus-security-plugin";
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  @Override
  public boolean hasScript() {
    return false;
  }

  @Nullable
  @Override
  public String getNamespace() {
    return null;
  }

  @Nullable
  @Override
  public String getConfigClassName() {
    return null;
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
