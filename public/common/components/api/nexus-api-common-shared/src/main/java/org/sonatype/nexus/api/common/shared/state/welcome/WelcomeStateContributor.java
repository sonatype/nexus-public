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
package org.sonatype.nexus.api.common.shared.state.welcome;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Value;

import static java.util.Objects.requireNonNull;
import org.springframework.stereotype.Component;

@Component
public class WelcomeStateContributor
    implements StateContributor
{

  public static final String NODE_ID = "nexus.node.id";

  private final NodeAccess nodeAccess;

  private final Boolean featureFlag;

  @Autowired
  public WelcomeStateContributor(
      @Value("${nexus.react.welcome:true}") final Boolean featureFlag,
      final NodeAccess nodeAccess)
  {
    this.nodeAccess = requireNonNull(nodeAccess);
    this.featureFlag = featureFlag;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(
        "nexus.react.welcome", featureFlag,
        NODE_ID, nodeAccess.getId());
  }
}
