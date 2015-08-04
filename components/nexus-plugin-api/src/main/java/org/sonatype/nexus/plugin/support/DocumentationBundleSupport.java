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
package org.sonatype.nexus.plugin.support;

import org.sonatype.nexus.plugin.PluginIdentity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link DocumentationBundle} implementations.
 *
 * @since 2.7
 */
public abstract class DocumentationBundleSupport
    extends AbstractDocumentationResourceBundle
{
  private final PluginIdentity owner;

  protected DocumentationBundleSupport(final PluginIdentity plugin) {
    this.owner = checkNotNull(plugin);
  }

  @Override
  public String getPluginId() {
    return owner.getId();
  }

  @Override
  public String getDescription() {
    return String.format("%s API", owner.getId());
  }
}
