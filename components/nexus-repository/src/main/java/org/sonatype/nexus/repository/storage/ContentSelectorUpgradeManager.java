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
package org.sonatype.nexus.repository.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.CselValidator;
import org.sonatype.nexus.selector.SelectorManager;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

/**
 * @since 3.6
 */
@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class ContentSelectorUpgradeManager
    extends StateGuardLifecycleSupport
{
  private final CselValidator cselValidator;

  private final SelectorManager selectorManager;

  @Inject
  public ContentSelectorUpgradeManager(final CselValidator cselValidator, final SelectorManager selectorManager) {
    this.cselValidator = cselValidator;
    this.selectorManager = selectorManager;
  }

  @Override
  protected void doStart() throws Exception {
    selectorManager.browseJexl().forEach(config -> {
      String expression = (String) config.getAttributes().get("expression");
      String name = config.getName();

      log.debug("Attempting to upgrade jexl content selector to csel, expression={}", expression);

      try {
        if (cselValidator.validate(expression)) {
          config.setType(CselSelector.TYPE);
          selectorManager.update(config);
        }
        else {
          log.warn(
              "Could not convert deprecated jexl content selector into csel content selector with name={}, expression={}",
              name, expression);
        }
      }
      catch (Exception e) {
        log.warn(
            "Failed to parse jexl content selector for conversion to csel content selector with name={}, expression={}",
            name, expression, log.isDebugEnabled() ? e : null);
      }
    });
  }
}
