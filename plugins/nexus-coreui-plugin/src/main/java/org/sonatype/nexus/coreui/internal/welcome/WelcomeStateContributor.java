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
package org.sonatype.nexus.coreui.internal.welcome;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.collect.ImmutableMap;
import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class WelcomeStateContributor
    extends ComponentSupport
    implements StateContributor
{
  public static final String NODE_ID = "nexus.node.id";

  private final NodeAccess nodeAccess;

  private final Boolean featureFlag;

  @Inject
  public WelcomeStateContributor(
      @Named("${nexus.react.welcome:-true}") final Boolean featureFlag,
      final NodeAccess nodeAccess)
  {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.featureFlag = featureFlag;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        "nexus.react.welcome", featureFlag,
        NODE_ID, nodeAccess.getId());
  }
}
