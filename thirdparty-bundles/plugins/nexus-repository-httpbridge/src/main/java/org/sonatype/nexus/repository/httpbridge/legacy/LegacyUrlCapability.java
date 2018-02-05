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
package org.sonatype.nexus.repository.httpbridge.legacy;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.capability.CapabilitySupport;

/**
 * Legacy URL capability.
 *
 * @since 3.7
 */
@Named(LegacyUrlCapabilityDescriptor.TYPE_ID)
public class LegacyUrlCapability
    extends CapabilitySupport<LegacyUrlCapabilityConfiguration>
{
  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("NXRM2 style urls enabled")
    String description();
  }

  private static final Messages messages = I18N.create(Messages.class);

  @Override
  protected LegacyUrlCapabilityConfiguration createConfig(final Map<String, String> properties) {
    return new LegacyUrlCapabilityConfiguration();
  }

  @Override
  protected String renderDescription() throws Exception {
    if (context().isActive()) {
      return messages.description();
    }
    return null;
  }
}
