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
package org.sonatype.nexus.testsuite.testsupport.apt;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystem;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class AptTestHelper
{
  public static final String CONTENT_TYPE = "application/x-debian-package";

  public static final String CATEGORY = StringUtils.EMPTY;

  public static final String DEB = "nano_2.2.6-1_amd64.deb";

  public static final String DEB_SZT =
      "linux-image-unsigned-5.14.1-051401-generic_5.14.1-051401.202109030936_amd64.deb";

  public static final String DEB_ARCH = "amd64";

  public static final String DEB_NAME = "nano";

  public static final String DEB_VERSION = "2.2.6-1";

  public static final String DEB_PATH = "pool/n/nano/" + DEB;

  public static final String DEB_V2_5 = "nano_2.5.3-2_amd64.deb";

  public static final String DEB_V2_5_PATH = "pool/n/nano/" + DEB_V2_5;

  public static final String DEB_V2_5_VERSION = "2.5.3-2";

  public static final String CPU_LIMIT_DEB = "cpulimit_2.5-1_amd64.deb";

  public static final String CPU_LIMIT_NAME = "cpulimit";

  public static final String DISTRIBUTION = "bionic";

  public static final String GPG_KEY_NAME = "gpgKey";

  public static final String GPG_PUBLIC_KEY_NAME = "gpgPublicKey";

  public static final String METEDATA_PATH = "dists/bionic/";

  public static final String METADATA_INRELEASE = "InRelease";

  public static final String METADATA_INRELEASE_PATH = METEDATA_PATH + METADATA_INRELEASE;

  public static final String METADATA_RELEASE = "Release";

  public static final String METADATA_RELEASE_PATH = METEDATA_PATH + METADATA_RELEASE;

  public static final String METADATA_RELEASE_GPG = "Release.gpg";

  public static final String METADATA_RELEASE_GPG_PATH = METEDATA_PATH + METADATA_RELEASE_GPG;

  public static final String PACKAGES_PATH = METEDATA_PATH + "main/binary-amd64/Packages";

  public static final String PACKAGES_BZ2_PATH = METEDATA_PATH + "main/binary-amd64/Packages.bz2";

  public static final String PACKAGES_GZ_PATH = METEDATA_PATH + "main/binary-amd64/Packages.gz";

  @Inject
  private NexusTestSystem nexus;

  public AptClient client(final Repository repository) {
    return new AptClient(
        nexus.rest().client("admin", "admin123"),
        nexus.rest().clientContext(),
        nexus.rest().resolveNexusPath("/repository/" + repository.getName() + '/')
    );
  }
}
