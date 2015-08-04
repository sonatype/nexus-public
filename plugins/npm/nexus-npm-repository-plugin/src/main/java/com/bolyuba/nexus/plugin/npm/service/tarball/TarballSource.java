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
package com.bolyuba.nexus.plugin.npm.service.tarball;

import java.io.IOException;

import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;

/**
 * Transport for getting NPM tarballs, that might be anywhere (the URL pointed by metadata should be used).
 */
public interface TarballSource
{
  /**
   * Unconditionally fetches the tarball for given package version. This call does not perform any conditional
   * checking of remote, as NPM stores the checksum in the metadata (package version), hence, if locally exists
   * the given file, and checksum matches, no need to check on remote for "newer version". On the other hand, this
   * method will make it's best to ensure that returned tarball is correct content (content validation and transport
   * consistency is checked).
   */
  NpmBlob get(NpmProxyRepository npmProxyRepository, TarballRequest tarballRequest) throws IOException;
}
