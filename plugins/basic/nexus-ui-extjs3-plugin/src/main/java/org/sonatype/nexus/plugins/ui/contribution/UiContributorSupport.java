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
package org.sonatype.nexus.plugins.ui.contribution;

import org.sonatype.nexus.plugin.PluginIdentity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link UiContributor} implementations.
 *
 * @since 2.7
 */
public class UiContributorSupport
    implements UiContributor
{
  private final PluginIdentity owner;

  public UiContributorSupport(final PluginIdentity owner) {
    this.owner = checkNotNull(owner);
  }

  @Override
  public UiContribution contribute(final boolean debug) {
    UiContributionBuilder builder = new UiContributionBuilder(owner);

    if (debug) {
      // include debug stylesheet if there is one for the plugin
      String css = String.format("static/css/%s.css", owner.getCoordinates().getArtifactId());
      if (owner.getClass().getClassLoader().getResource(css) != null) {
        builder.withDependency("css!" + css + builder.getCacheBuster(css));
      }
    }

    // maybe customize
    customize(builder);

    return builder.build(debug);
  }

  /**
   * Sub-class can customize the contribution (to add conditions, etc).
   */
  protected void customize(final UiContributionBuilder builder) {
    // empty
  }
}
