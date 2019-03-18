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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.IOException;
import java.util.Map;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;

/**
 * Persistence for PyPI hosted.
 *
 * @since 3.1
 */
@Facet.Exposed
public interface PyPiHostedFacet
    extends Facet
{
  /**
   * Retrieve root index.
   *
   * @return simple root HTML
   */
  Content getRootIndex();

  /**
   * Retrieve index.
   *
   * @param name package name
   * @return simple package HTML
   */
  Content getIndex(String name) throws IOException;

  /**
   * Retrieve package.
   *
   * @param packagePath the full package path
   * @return the package content
   */
  Content getPackage(String packagePath);

  /**
   * Perform package upload.
   *
   * @param filename the name of the file
   * @param attributes the package attributes
   * @param payload uploaded file content
   * @return the created/updated asset
   */
  Asset upload(String filename, Map<String, String> attributes, TempBlobPartPayload payload) throws IOException;

  /**
   * Extract metadata from a package
   *
   * @param tempBlob the temporary blob
   * @return the created/updated asset
   */
  Map<String, String> extractMetadata(TempBlob tempBlob) throws IOException;

  /**
   * Create path to the package indicated by the name, version and filename.
   *
   * @param name the package name
   * @param version the package version
   * @param filename the filename
   * @return the path
   */
  String createPackagePath(String name, String version, String filename);
}
