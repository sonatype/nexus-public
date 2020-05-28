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
package org.sonatype.nexus.coreui.internal.node;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.rapture.StateContributor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Contributes freeze state to the UI.
 *
 * @since 3.24
 */
@Named
@Singleton
public class FreezeStateContributor
    implements StateContributor
{
  private final FreezeService freezeService;

  @Inject
  public FreezeStateContributor(final FreezeService freezeService) {
    this.freezeService = checkNotNull(freezeService);
  }

  @Override
  public Map<String, Object> getState() {
    Map<String, Object> state = new HashMap<>();
    state.put("frozen", freezeService.isFrozen());
    state.put("frozenManually", freezeService.isFrozenByUser());
    return state;
  }
}
