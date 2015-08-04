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
package org.sonatype.nexus.configuration.application.events;

import org.sonatype.nexus.configuration.application.GlobalRemoteProxySettings;
import org.sonatype.nexus.events.AbstractEvent;

/**
 * Event fired when global remote Proxy settings are changed (within configuration change). The settings carried in
 * this event will reflect the NEW values, but if you have the {@link org.sonatype.nexus.configuration.application.GlobalRemoteProxySettings}
 * component, you can
 * query it too <em>after</em> you received this event .
 *
 * @since 2.6
 */
public class GlobalRemoteProxySettingsChangedEvent
    extends AbstractEvent<GlobalRemoteProxySettings>
{

  public GlobalRemoteProxySettingsChangedEvent(
      final GlobalRemoteProxySettings remoteProxySettingsConfiguration)
  {
    super(remoteProxySettingsConfiguration);
  }

}
