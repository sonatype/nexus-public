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
package com.bolyuba.nexus.plugin.npm.service;

import java.io.File;

import org.sonatype.nexus.proxy.item.FileContentLocator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NPM related binary object, usually tarball.
 */
public class NpmBlob
    extends FileContentLocator
{
  private final String name;

  private final String sha1sum;

  public NpmBlob(final File file, final String contentType, final String name, final String sha1sum) {
    super(file, contentType);
    this.name = checkNotNull(name);
    this.sha1sum = checkNotNull(sha1sum);
  }

  public String getName() {
    return name;
  }

  public String getSha1sum() {
    return sha1sum;
  }
}
