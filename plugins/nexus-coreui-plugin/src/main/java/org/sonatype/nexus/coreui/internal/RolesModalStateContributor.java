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
package org.sonatype.nexus.coreui.internal;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.StateContributor;

import com.google.common.collect.ImmutableMap;

import static org.sonatype.nexus.common.app.FeatureFlags.REACT_ROLES_MODAL_ENABLED;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_ROLES_MODAL_NAMED;

@Singleton
@Named
public class RolesModalStateContributor
    implements StateContributor
{
  private final boolean isRolesModalEnabled;

  @Inject
  public RolesModalStateContributor(
  @Named(REACT_ROLES_MODAL_NAMED) boolean isRolesModalEnabled)
  {
    this.isRolesModalEnabled = isRolesModalEnabled;
  }

  @Override
  public Map<String, Object> getState() {
    return ImmutableMap.of(REACT_ROLES_MODAL_ENABLED, isRolesModalEnabled);
  }
}
