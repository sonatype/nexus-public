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
package org.sonatype.nexus.coreui.internal.repository.conan;

import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import static org.sonatype.nexus.common.app.FeatureFlags.CONAN_V2_ENABLED_NAMED;

@Named
@Singleton
public class ConanV2StateContributor
    extends ComponentSupport
    implements StateContributor
{
  private final boolean conanV2Enabled;

  @Inject
  public ConanV2StateContributor(@Named(CONAN_V2_ENABLED_NAMED) final boolean featureFlag) {
    this.conanV2Enabled = featureFlag;
  }

  @Override
  public Map<String, Object> getState() {
    return Collections.singletonMap("conanV2Enabled", conanV2Enabled);
  }
}
