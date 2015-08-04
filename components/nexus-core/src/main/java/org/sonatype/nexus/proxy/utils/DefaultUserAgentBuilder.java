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
package org.sonatype.nexus.proxy.utils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.plexus.util.StringUtils;

@Named
@Singleton
public class DefaultUserAgentBuilder
    implements UserAgentBuilder
{
  private final ApplicationStatusSource applicationStatusSource;

  private final List<UserAgentContributor> contributors;

  /**
   * The edition, that will tell us is there some change happened with installation.
   */
  private String platformEditionShort;

  /**
   * The lazily calculated invariant part of the UserAgentString.
   */
  private String userAgentPlatformInfo;

  @Inject
  public DefaultUserAgentBuilder(final ApplicationStatusSource applicationStatusSource,
                                 final List<UserAgentContributor> contributors)
  {
    this.applicationStatusSource = applicationStatusSource;
    this.contributors = contributors;
  }

  @Override
  public String formatGenericUserAgentString() {
    return getUserAgentPlatformInfo();
  }

  @Override
  public String formatRemoteRepositoryStorageUserAgentString(final ProxyRepository repository,
                                                             final RemoteStorageContext ctx)
  {

    return ua(ctx, repository).toString();
  }

  @Override
  public String formatUserAgentString(final RemoteStorageContext ctx) {
    return ua(ctx).toString();
  }

  // ==

  private StringBuilder ua(final RemoteStorageContext ctx) {
    return ua(ctx, null);
  }

  @VisibleForTesting
  StringBuilder ua(final RemoteStorageContext ctx, final ProxyRepository repository) {
    final StringBuilder buf = new StringBuilder(getUserAgentPlatformInfo());

    if (repository != null) {
      final RemoteRepositoryStorage rrs = repository.getRemoteStorage();
      buf.append(" ").append(rrs.getProviderId()).append("/").append(rrs.getVersion());
    }

    // user customization
    RemoteConnectionSettings remoteConnectionSettings = ctx.getRemoteConnectionSettings();

    if (!StringUtils.isEmpty(remoteConnectionSettings.getUserAgentCustomizationString())) {
      buf.append(" ").append(remoteConnectionSettings.getUserAgentCustomizationString());
    }

    // plugin customization
    for (UserAgentContributor contributor : contributors) {
      final String contribution = contributor.getUserAgent(ctx, repository);
      if (!StringUtils.isEmpty(contribution)) {
        buf.append(" ").append(contribution);
      }
    }

    return buf;
  }

  protected synchronized String getUserAgentPlatformInfo() {
    // TODO: this is a workaround, see NXCM-363
    SystemStatus status = applicationStatusSource.getSystemStatus();

    if (platformEditionShort == null || !platformEditionShort.equals(status.getEditionShort())
        || userAgentPlatformInfo == null) {
      platformEditionShort = status.getEditionShort();

      userAgentPlatformInfo =
          new StringBuilder("Nexus/").append(status.getVersion()).append(" (").append(
              status.getEditionShort()).append("; ").append(System.getProperty("os.name")).append("; ").append(
              System.getProperty("os.version")).append("; ").append(System.getProperty("os.arch")).append(
              "; ").append(System.getProperty("java.version")).append(")").toString();
    }

    return userAgentPlatformInfo;
  }

}
