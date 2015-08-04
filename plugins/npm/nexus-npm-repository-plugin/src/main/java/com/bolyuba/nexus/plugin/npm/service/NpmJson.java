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

import java.util.Map;

import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for "raw" JSON wrapper (represented as map of String-Object).
 */
abstract class NpmJson
{
  private final String repositoryId;

  private final Map<String, Object> raw;

  public NpmJson(final String repositoryId, final Map<String, Object> raw) {
    this.repositoryId = checkNotNull(repositoryId);
    this.raw = Maps.newHashMap();
    setRaw(raw);
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public Map<String, Object> getRaw() {
    return raw;
  }

  public void setRaw(final Map<String, Object> raw) {
    validate(raw);
    this.raw.clear();
    this.raw.putAll(raw);
  }

  protected abstract void validate(final Map<String, Object> raw);
}
