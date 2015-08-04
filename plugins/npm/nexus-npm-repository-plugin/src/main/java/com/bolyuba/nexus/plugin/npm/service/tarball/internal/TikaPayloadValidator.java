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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;
import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tika based validator, uses MIME magic bytes of the content itself to figure out what content is.
 */
@Singleton
@Named
public class TikaPayloadValidator
    extends ComponentSupport
    implements TarballValidator
{
  /**
   * This is MIME type we expect Tika to detect. This is NOT the MIME type we advertise downstream, which
   * is {@link NpmRepository#TARBALL_MIME_TYPE}.
   */
  private static final String[] EXPECTED_MIME_TYPES = {"application/x-gzip", "application/gzip"};

  private final MimeSupport mimeSupport;

  @Inject
  public TikaPayloadValidator(final MimeSupport mimeSupport) {
    this.mimeSupport = checkNotNull(mimeSupport);
  }

  @Override
  public Result validate(final TarballRequest request, final NpmBlob tarball) {
    try {
      final List<String> detectedMimeTypes = mimeSupport.detectMimeTypesListFromContent(tarball);
      log.trace("Tika detected content of {} as '{}'", tarball.getName(), detectedMimeTypes);
      for (String expectedMimeType : EXPECTED_MIME_TYPES) {
        if (detectedMimeTypes.contains(expectedMimeType)) {
          return Result.VALID;
        }
      }
      return Result.INVALID;
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
