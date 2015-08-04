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
 * helper to collect or merge dependency data from <code>DependencyFile</code>s
 * or extract the dependency data from a given <code>GemspecFile</code>.
 * the remote data from <code>BundlerApiFile</code> is collection of the same
 * dependency data format which can added as well.
 * 
 * after adding all the data, you can retrieve the list of gemnames for which
 * there are dependency data and retrieve them as marshalled stream (same format as
 * <code>DependencyFile</code> or <code>BundlerApiFile</code>).
 * 
 * @author christian
 *
 */
public interface DependencyHelper {
  
  /**
   * add dependency data to instance
   * @param marshalledDependencyData stream of the marshalled "ruby" data
   */
  void add(InputStream marshalledDependencyData);
  
  /**
   * add dependency data to instance from a rzipped gemspec object.
   * @param gemspec rzipped stream of the marshalled gemspec object
   */
  void addGemspec(InputStream gemspec);

  /**
   * freezes the instance - no more added of data is allowed - and returns
   * the list of gemnames for which dependency data was added.
   * @return String[] of gemnames
   */
  String[] getGemnames();
  
  /**
   * marshal ruby object with dependency data for the given gemname.
   * @param gemname
   * @return ByteArrayInputStream of binary data
   */
  ByteArrayInputStream getInputStreamOf(String gemname);
  
  /**
   * marshal ruby object with dependency data for all the dependency data,
   * either with or without duplicates.
   * @return ByteArrayInputStream of binary data
   */
  ByteArrayInputStream getInputStream(boolean unique);
}