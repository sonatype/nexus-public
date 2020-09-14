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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.IOException;
import java.util.Map;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

/**
 * npm hosted facet.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface OrientNpmHostedFacet
    extends Facet, NpmHostedFacet
{
  /**
   * Add the package using the package.json and <code>TempBlob</code>.
   *
   * @since 3.7
   */
  Asset putPackage(Map<String, Object> packageJson, TempBlob tempBlob) throws IOException;
}
