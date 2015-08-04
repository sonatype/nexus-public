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
package org.sonatype.nexus.plexusplugin.rest.ui;

import org.sonatype.nexus.plugins.ui.contribution.UiContributionBuilder;
import org.sonatype.nexus.plugins.ui.contribution.UiContributor;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = UiContributor.class, hint = "PlexusPluginUiContributor")
public class PlexusPluginUiContributor
    implements UiContributor
{

  public static final String ARTIFACT_ID = "nexus-plexusplugin-plugin";

  public static final String GROUP_ID = "org.sonatype.nexus.plugins";

  @Override
  public UiContribution contribute(final boolean debug) {
    return new UiContributionBuilder(this, GROUP_ID, ARTIFACT_ID).build(debug);
  }
}
