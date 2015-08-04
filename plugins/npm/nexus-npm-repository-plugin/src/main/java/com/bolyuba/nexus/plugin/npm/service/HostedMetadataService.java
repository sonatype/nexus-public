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

/**
 * Metadata service for hosted repositories, it serves up what has been consumed by it (deployed to it).
 */
public interface HostedMetadataService
    extends Generator, Consumer
{
  /**
   * Deletes all metadata hosted by this service from the underlying store.
   *
   * @since 2.11.3
   */
  boolean deleteAllMetadata();

  /**
   * Deletes package metadata hosted by this service from underlying store, returns {@code true} if package existed.
   *
   * @since 2.11.3
   */
  boolean deletePackage(String packageName);
}
