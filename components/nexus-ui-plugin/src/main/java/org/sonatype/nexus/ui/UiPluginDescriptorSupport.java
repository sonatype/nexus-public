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
package org.sonatype.nexus.ui;

import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link UiPluginDescriptor} implementations.
 *
 * @since 3.20
 */
public abstract class UiPluginDescriptorSupport
    extends ComponentSupport
    implements UiPluginDescriptor
{
  private final String name;

  public UiPluginDescriptorSupport(final String name) {
    this.name = checkNotNull(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getScripts(final boolean isDebug) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getStyles() {
    return Collections.emptyList();
  }
}
