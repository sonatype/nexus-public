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

import java.util.List;

import org.sonatype.nexus.plugin.PluginIdentity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Used to look up plugins contributing proper require.js UI code.
 *
 * @since 2.5
 */
public interface UiContributor
{
  /**
   * @deprecated Plugins should create {@link PluginIdentity} components to capture GAV details.
   */
  @Deprecated
  String OSS_PLUGIN_GROUP = "org.sonatype.nexus.plugins";

  /**
   * @deprecated Plugins should create {@link PluginIdentity} components to capture GAV details.
   */
  @Deprecated
  String PRO_PLUGIN_GROUP = "com.sonatype.nexus.plugins";

  /**
   * Called on Nexus UI page load.
   *
   * @param debug true if debug code should be delivered; false otherwise.
   * @return An object containing information about what to load for the UI.
   */
  UiContribution contribute(boolean debug);

  /**
   * Contains dependencies to load and the base module to use, for example:
   *
   * dependencies: ["js/static/plugin-all.js"]
   *
   * module name: "Nexus/plugin/bootstrap"
   *
   * where plugin-all.js contains require.js module definitions (e.g. `define("Nexus/plugin/bootstrap",
   * ["Nexus/plugin/dep1"], function() {}` ).
   *
   * @see org.sonatype.nexus.plugins.ui.contribution.UiContributionBuilder
   * @since 2.4
   */
  class UiContribution
  {
    private String module;

    private List<String> dependencies;

    private boolean enabled;

    /**
     * @see org.sonatype.nexus.plugins.ui.contribution.UiContributionBuilder
     */
    public UiContribution(final String module, final List<String> dependencies, final boolean enabled) {
      this.module = checkNotNull(module);
      this.dependencies = checkNotNull(dependencies);
      this.enabled = enabled;
    }

    /**
     * The dependencies to load before the module defined by {@link #getModule()} is required.
     * May be any valid require.js dependency name (url, module name, plugin)
     *
     * @return the list of dependencies to load.
     */
    public List<String> getDependencies() {
      return dependencies;
    }

    /**
     * The module that is used to initialize the plugin.
     *
     * @return the module name.
     */
    public String getModule() {
      return module;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

}
