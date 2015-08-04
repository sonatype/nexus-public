/*
 * Copyright (c) 2007-2014 Sonatype, Inc. and Georgy Bolyuba. All rights reserved.
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
package com.bolyuba.nexus.plugin.npm;

import org.sonatype.nexus.proxy.repository.Repository;

import com.bolyuba.nexus.plugin.npm.internal.NpmMimeRulesSource;
import com.bolyuba.nexus.plugin.npm.service.Generator;

/**
 * @author Georgy Bolyuba (georgy@bolyuba.com)
 */
public interface NpmRepository
    extends Repository
{
  /**
   * Mime type used for npm metadata downstream. See {@link NpmMimeRulesSource}.
   */
  String JSON_MIME_TYPE = "application/json";

  /**
   * Mime type used for npm tarballs downstream. See {@link NpmMimeRulesSource}.
   */
  String TARBALL_MIME_TYPE = "application/x-gzip";

  /**
   * Registry "escape" character, that is invalid package name or version.
   */
  String NPM_REGISTRY_SPECIAL = "-";

  /**
   * Key for flag used to mark a store request "already serviced" by NPM metadata service.
   */
  String NPM_METADATA_SERVICED = "NpmMetadataServiced";

  /**
   * Key for flag used to disable NPM metadata service in repository.
   */
  String NPM_METADATA_NO_SERVICE = "NpmMetadataNoService";

  /**
   * Returns the npm repository's metadata service. At this level, it's is basically a {@link Generator}, but see
   * specialized interfaces for NPM hosted, proxy and group repositories.
   */
  Generator getMetadataService();
}
