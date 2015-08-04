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

import java.io.IOException;

import org.sonatype.nexus.proxy.item.ContentLocator;

/**
 * Metadata consumer that consumes "raw", probably streamed input of a package root.
 */
public interface Consumer
{
  /**
   * Parses the package request and it's belonging content into {@link PackageRoot} instance.
   */
  PackageRoot parsePackageRoot(PackageRequest request, ContentLocator contentLocator) throws IOException;

  /**
   * Consumes the package root into underlying store and returns the consumed package
   * root (merged if applicable, in case update happened).
   */
  PackageRoot consumePackageRoot(PackageRoot packageRoot) throws IOException;
}

