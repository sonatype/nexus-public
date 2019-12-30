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
package org.sonatype.nexus.upgrade.internal.orient

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport
import org.sonatype.nexus.supportzip.SupportBundle
import org.sonatype.nexus.supportzip.SupportBundleCustomizer

import static com.google.common.base.Preconditions.checkNotNull
import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.REQUIRED
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.SYSINFO

/**
 * Includes details of the present Database schema versions in the support bundle.
 * @since 3.6
 */
@Named
@Singleton
class DatabaseModelVersionsCustomizer
    implements SupportBundleCustomizer
{
  final UpgradeManager upgradeManager

  @Inject
  DatabaseModelVersionsCustomizer(final UpgradeManager upgradeManager) {
    this.upgradeManager = checkNotNull(upgradeManager)
  }

  @Override
  void customize(final SupportBundle supportBundle) {
    supportBundle << new GeneratedContentSourceSupport(SYSINFO, 'info/dbModelVersions.json', REQUIRED) {
      @Override
      protected void generate(final File file) {
        file.text = prettyPrint(toJson(upgradeManager.latestKnownModelVersions()))
      }
    }
  }
}
