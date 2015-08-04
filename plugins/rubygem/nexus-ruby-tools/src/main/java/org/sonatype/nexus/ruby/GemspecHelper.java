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

import org.jruby.runtime.builtin.IRubyObject;

/**
 * wrapper around a Gem::Specification ruby object with some extra
 * helper methods and only delegate a few needed methods to the underlying object.
 * 
 * @author christian
 *
 */
public interface GemspecHelper
{

  /**
   * the filename of associated gem
   * @return filename
   */
  String filename();
  
  /**
   * get name of gem
   * @return name of the gem
   */
  String name();
  
  /**
   * get gemspec object
   * @return the wrapped Gem::Specification object
   */
  IRubyObject gemspec();
  
  /**
   * create pom XML out of the gemspec used for gem-artifacts.
   * gem versions with alphabets are "prereleased" versions and
   * the closet to them are snapshot versions in maven. but some
   * released gems do have depdendencies to prereleased version, so
   * both SNAPSHOT and non-SNAPSHOT version of prereleased gem-artifacts
   * need to be provided. 
   * 
   * @param snapshot whether to use snapshot version
   * @return pom XML
   */
  String pom(boolean snapshot);
  
  /**
   * marshals the gemspec and deflate it and turns it into a ByteArrayInputStream.
   * that is the format XYZ.gemspec.rz of a rubygems repo.
   * @return the stream to binray data
   */
  ByteArrayInputStream getRzInputStream();
}
