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
import org.sonatype.nexus.util.Condition;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder for contributions used to activate plugin UI.
 *
 * @see org.sonatype.nexus.plugins.ui.contribution.UiContributor
 * @since 2.5
 */
public class UiContributionBuilder
    extends AbstractUiContributionBuilder<UiContributor.UiContribution>
{

  private String module;

  private List<String> dependencies = Lists.newLinkedList();

  private Condition condition = new Condition()
  {
    @Override
    public boolean isSatisfied() {
      return true;
    }
  };

  public UiContributionBuilder(final Object owner, final String groupId, final String artifactId) {
    super(owner, groupId, artifactId);
  }

  /**
   * @since 2.7
   */
  public UiContributionBuilder(final PluginIdentity owner) {
    this(owner, owner.getCoordinates().getGroupId(), owner.getCoordinates().getArtifactId());
  }

  /**
   * Sets the entry point to use. If no module is set, the builder will use "artifactId-boot" as a default
   * module name.
   */
  public UiContributionBuilder boot(final String module) {
    this.module = module;
    return this;
  }

  /**
   * Adds a dependency.
   */
  public UiContributionBuilder withDependency(final String dependency) {
    checkNotNull(dependency);
    dependencies.add(dependency);
    return this;
  }

  /**
   * Adds the default location for a compressed plugin js file: {@code static/js/${artifactId}-all.js}.
   */
  public UiContributionBuilder withDefaultAggregateDependency() {
    final String js = getDefaultPath("js", true);
    return withDependency(js);
  }

  private void maybeAddDefaultCssDependency() {
    final String path = getDefaultPath("css", false);
    if (owner.getClass().getClassLoader().getResource(path) != null) {
      withDependency("css!" + path + getCacheBuster(path));
    }
  }

  /**
   * Adds the default css dependency if it is available: {@code static/js/${artifactId}-all.css}.
   */
  public UiContributionBuilder withDefaultCssDependency() {
    maybeAddDefaultCssDependency();
    return this;
  }

  public UiContributionBuilder setDefaultModule() {
    return boot(artifactId + "-boot");
  }

  @Override
  public UiContributor.UiContribution build() {
    return build(false);
  }

  /**
   * If no module is set, the builder will use "artifactId-boot" as a default
   * module name.
   * <p/>
   * If no dependencies are set, the builder will add the default CSS dependency,
   * if it is available by Classloader resource lookup.
   * <p/>
   * If no dependencies are set, the builder will add the default JS dependency,
   * unless the debug parameter is set to true.
   */
  public UiContributor.UiContribution build(final boolean debug) {
    if (module == null) {
      setDefaultModule();
    }

    if (dependencies.isEmpty()) {
      // always add css dependency, also needed when debug is requested
      maybeAddDefaultCssDependency();
      if (!debug) {
        withDefaultAggregateDependency();
      }
    }

    return new UiContributor.UiContribution(module, dependencies, condition.isSatisfied());
  }

  /**
   * Add a condition to the builder that is evaluated on {@link #build} to determine if the contribution is enabled or
   * not.
   *
   * @param condition The condition which must be satisfied to enable the contribution.
   */
  public UiContributionBuilder withCondition(final Condition condition) {
    this.condition = checkNotNull(condition);
    return this;
  }

}
