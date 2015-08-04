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
import java.util.List;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * helper around specs index like adding a spec to the index or deleting a spec from the index.
 * since spec index is marshal ruby object it offers a method to get the data for an empty spec index.
 * when to create a <code>DependencyFile</code> you need to interate over all versions of a given gem.
 * 
 * @author christian
 */
public interface SpecsHelper {

  /**
   * create an emptry spec index, i.e. create marshaled ruby object for an empty spec index
   * @return the stream to data
   */
  ByteArrayInputStream createEmptySpecs();
  
  /**
   * adds the given spec to the spec index. the action depends on the <code>SpecsIndexTyep</code>:
   * <li>release: only adds the spec if it belongs to released gem</li>
   * <li>prerelease: only adds the spec if it belongs to prereleased gem</li>
   * <li>latest: only adds the spec if it belongs to released gem and make sure we have only one version per gemname</li>
   * it only returns the new specs index if it changes, i.e. if the given spec already exists in the specs index
   * then a <code>null</code> gets returned.
   * 
   * @param spec a Gem::Specification ruby object
   * @param specsIndex the <code>InputStream</code> to the spec index
   * @param type whether it is release, prerelease or latest
   * @return the next spec index as <code>ByteArrayInputStream</code> if there was a change or <code>null</code>
   * if the spec index remained the same
   */
  ByteArrayInputStream addSpec(IRubyObject spec, InputStream specsIndex, SpecsIndexType type);
  
  /**
   * it deletes the given spec from the spec index. if spec does not exist  
   * <li>release: only adds the spec if it belongs to released gem</li>
   * <li>prerelease: only adds the spec if it belongs to prereleased gem</li>
   * <li>latest: only adds the spec if it belongs to released gem and make sure we have only one version per gemname</li>
   * 
   * it only returns the new specs index if it changes, i.e. if the given spec did not exsists in the specs index
   * then a <code>null</code> gets returned.
   * 
   * this method keeps a state and is meant to be called with all three specs-index types and with 'latest' type
   * at the end. in can happen that the latest spec index needs to reconstructed from release specs index
   * for this the release spec index remains as state of the class until the call with latest specs index is done.    
   * 
   * @param spec a Gem::Specification ruby object
   * @param specsIndex the <code>InputStream</code> to the spec index
   * @param type whether it is release, prerelease or latest
   * @return the next spec index as <code>ByteArrayInputStream</code>
   */
  ByteArrayInputStream deleteSpec(IRubyObject spec, InputStream specsIndex, SpecsIndexType type);

  /**
   * collect all versions from the given specs index for given gemname.
   * @param gemname for which the version list shall be retrieved
   * @param specsIndex <code>InputStream</code> to specs index
   * @return
   */
  List<String> listAllVersions(String gemname, InputStream specsIndex);

}