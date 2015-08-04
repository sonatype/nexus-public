/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.tarball.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballSource;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.TarballValidator.Result;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link TarballSource} implementation.
 */
@Singleton
@Named
public class TarballSourceImpl
    extends ComponentSupport
    implements TarballSource
{
  private final ApplicationDirectories applicationDirectories;

  private final HttpTarballTransport tarballTransport;

  private final Map<String, TarballValidator> validators;

  @Inject
  public TarballSourceImpl(final ApplicationDirectories applicationDirectories,
                           final HttpTarballTransport tarballTransport,
                           final Map<String, TarballValidator> validators)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.tarballTransport = checkNotNull(tarballTransport);
    this.validators = checkNotNull(validators);
  }

  @Override
  public NpmBlob get(final NpmProxyRepository npmProxyRepository, final TarballRequest tarballRequest)
      throws IOException
  {
    int fetchRetries = 1;
    if (npmProxyRepository.getRemoteStorageContext() != null) {
      RemoteConnectionSettings settings = npmProxyRepository.getRemoteStorageContext().getRemoteConnectionSettings();
      if (settings != null) {
        fetchRetries = settings.getRetrievalRetryCount();
      }
    }
    final boolean debug = log.isDebugEnabled();
    final File tempFile = File
        .createTempFile(npmProxyRepository.getId() + "-tarball", "tgz",
            applicationDirectories.getTemporaryDirectory());
    for (int i = 0; i < fetchRetries; i++) {
      if (debug) {
        log.debug("Retry {}/{} for {}@{} tarball...", i, fetchRetries, tarballRequest.getPackageVersion().getName(),
            tarballRequest.getPackageVersion().getVersion());
      }
      try {
        NpmBlob tarball = tarballTransport
            .getTarballForVersion(npmProxyRepository, tempFile, tarballRequest.getPackageVersion());
        if (tarball == null) {
          if (debug) {
            log.debug("Tarball for {}@{} not found on {}",
                tarballRequest.getPackageVersion().getName(),
                tarballRequest.getPackageVersion().getVersion(),
                tarballRequest.getPackageVersion().getDistTarball());
          }
          return null;
        }
        for (TarballValidator validator : validators.values()) {
          final Result result = validator.validate(tarballRequest, tarball);
          if (debug) {
            log.debug("Validated tarball {}@{} :: {} found '{}' by validator {}",
                tarballRequest.getPackageVersion().getName(),
                tarballRequest.getPackageVersion().getVersion(),
                tarballRequest.getPackageVersion().getDistTarball(),
                result.name(),
                validator.getClass().getSimpleName());
          }
          if (result == Result.INVALID) {
            tarball.delete();
            throw new IOException("Invalid content detected: " + validator.getClass().getSimpleName());
          }
        }
        return tarball;
      }
      catch (IOException e) {
        // note and retry
        log.warn("Fetch {}/{} failed for {}@{} tarball: {}",
            i, fetchRetries,
            tarballRequest.getPackageVersion().getName(),
            tarballRequest.getPackageVersion().getVersion(),
            e.toString());
      }
    }
    return null;
  }
}
