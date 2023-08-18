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

package org.sonatype.nexus.repository.maven;

import java.util.Collection;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.utils.httpclient.UserAgentGenerator;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class MavenProxyRequestHeaderSupport
{
  private static final String ANALYTICS_CAPABILITY = "analytics-configuration";

  private final CapabilityRegistry capabilityRegistry;
  private final UserAgentGenerator userAgentGenerator;

  @Inject
  public MavenProxyRequestHeaderSupport(
      final CapabilityRegistry capabilityRegistry,
      final UserAgentGenerator userAgentGenerator)
  {
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    this.userAgentGenerator = checkNotNull(userAgentGenerator);
  }

  public String getUserAgentForAnalytics() {
    CapabilityReference capabilityReference = getCapabilityReference();
    return userAgentGenerator.buildUserAgentForAnalytics(capabilityReference);
  }

  @Nullable
  public CapabilityReference getCapabilityReference()
  {
    CapabilityType capabilityType = CapabilityType.capabilityType(ANALYTICS_CAPABILITY);
    Collection<? extends CapabilityReference> refs = capabilityRegistry.get(
        CapabilityReferenceFilterBuilder.capabilities().withType(capabilityType).includeNotExposed());
    CapabilityReference capabilityReference = null;
    if (!refs.isEmpty()) {
      capabilityReference = refs.iterator().next();
    }
    return capabilityReference;
  }

}
