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
package org.sonatype.nexus.proxy.events;

import org.sonatype.nexus.events.AbstractEvent;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;

/**
 * The target registry events superclass.
 *
 * @author velo
 */
public abstract class TargetRegistryEvent
    extends AbstractEvent<TargetRegistry>
{

  private final Target target;

  public Target getTarget() {
    return target;
  }

  public TargetRegistryEvent(final TargetRegistry targetRegistry, final Target target) {
    super(targetRegistry);
    this.target = target;
  }

  public TargetRegistry getTargetRegistry() {
    return getEventSender();
  }

}
