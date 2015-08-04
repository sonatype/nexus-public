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
package org.sonatype.nexus.ruby;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * helper to merge several specs-index files into one
 * 
 * @author christian
 *
 */
public interface MergeSpecsHelper {
  
  /**
   * add the specs data to the MergeSpecsHelper object
   * @param specsData as <code>InputStream</code>
   */
  void add(InputStream specsData);

  /**
   * marshal ruby object with 
   * @param latest whether of not
   * @return ByteArrayInputStream of binary data
   */
  ByteArrayInputStream getInputStream(boolean latest);

}