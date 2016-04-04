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
package org.sonatype.nexus.rapture;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link UiPluginDescriptor} implementations.
 *
 * @since 3.0
 */
public class UiPluginDescriptorSupport
  extends ComponentSupport
  implements UiPluginDescriptor
{
  /**
   * Artifact ID of UI-contributing plugin.  Used to resolve the location of its contributed web-resources.
   */
  private final String pluginId;

  private boolean hasStyle = true;

  private boolean hasScript = true;

  private String namespace;

  private String configClassName;

  public UiPluginDescriptorSupport(final String artifactId) {
    this.pluginId = checkNotNull(artifactId);
  }

  @Override
  public String getPluginId() {
    return pluginId;
  }

  @Override
  public boolean hasStyle() {
    return hasStyle;
  }

  public void setHasStyle(final boolean hasStyle) {
    this.hasStyle = hasStyle;
  }

  @Override
  public boolean hasScript() {
    return hasScript;
  }

  public void setHasScript(final boolean hasScript) {
    this.hasScript = hasScript;
  }

  @Override
  @Nullable
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(@Nullable final String namespace) {
    this.namespace = namespace;
  }

  @Override
  @Nullable
  public String getConfigClassName() {
    return configClassName;
  }

  public void setConfigClassName(final String configClassName) {
    this.configClassName = configClassName;
  }
}
