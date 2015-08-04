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

import java.io.InputStream;

/**
 * factory for all the ruby classes. all those ruby classes come with java interface
 * so they can be used easily from java.
 * 
 * @author christian
 *
 */
public interface RubygemsGateway
{
  /**
   * Cleans up resources used by gateway and terminates it along with the underlying scripting container.
   */
  void terminate();

  /**
   * create a new instance of <code>GemspecHelper</code>
   * @param gemspec the stream to the rzipped marshalled Gem::Specification ruby-object
   * @return an empty GemspecHelper
   */
  GemspecHelper newGemspecHelper(InputStream gemspec);

  /**
   * create a new instance of <code>GemspecHelper</code>
   * @param gem the stream to the from which the gemspec gets extracted
   * @return an empty GemspecHelper
   */
  GemspecHelper newGemspecHelperFromGem(InputStream gem);

  /**
   * create a new instance of <code>DependencyHelper</code>
   * @return an empty DependencyHelper
   */
  DependencyHelper newDependencyHelper();

  /**
   * create a new instance of <code>SpecsHelper</code>
   * @return an empty SpecsHelper
   */
  SpecsHelper newSpecsHelper();

  /**
   * create a new instance of <code>MergeSpecsHelper</code>
   * @return an empty MergeSpecsHelper
   */
  MergeSpecsHelper newMergeSpecsHelper();

  /**
   * create a new instance of <code>RepairHelper</code>
   * @return an empty DependencyHelper
   */
  RepairHelper newRepairHelper();

  /**
   * create a new instance of <code>DependencyData</code> and parse
   * the given dependency data
   * @param dependency the input-stream with the dependency data
   * @param name of gem of the dependency data
   * @param modified when the dependency data were last modified
   * @return dependency data
   */
  DependencyData newDependencyData(InputStream dependency, String name, long modified);
}
