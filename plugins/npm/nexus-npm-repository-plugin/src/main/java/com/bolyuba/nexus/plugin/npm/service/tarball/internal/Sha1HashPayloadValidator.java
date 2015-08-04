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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;

/**
 * Validator that based on expected and calculated (during transport) SHA1 hashes decides is content corrupted or not
 * (invalid or valid).
 */
@Singleton
@Named
public class Sha1HashPayloadValidator
    extends ComponentSupport
    implements TarballValidator
{
  @Override
  public Result validate(final TarballRequest request, final NpmBlob tarball) {
    // checksum validation: if present in metadata (usually is) as repo itself has no policy settings
    final String expectedShasum = request.getPackageVersion().getDistShasum();
    if (expectedShasum != null && !expectedShasum.equals(tarball.getSha1sum())) {
      return Result.INVALID;
    }
    else if (expectedShasum == null) {
      return Result.NEUTRAL;
    }
    else {
      return Result.VALID;
    }
  }
}
