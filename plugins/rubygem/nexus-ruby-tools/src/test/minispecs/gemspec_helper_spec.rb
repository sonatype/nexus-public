#
# Sonatype Nexus (TM) Open Source Version
# Copyright (c) 2008-present Sonatype, Inc.
# All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
#
# This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
# which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
#
# Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
# of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
# Eclipse Foundation. All other trademarks are the property of their respective owners.
#

require 'nexus/gemspec_helper_impl'
require 'minitest/spec'
require 'minitest/autorun'

describe Nexus::GemspecHelperImpl do

  let( :dir ) { File.dirname( __FILE__ ) }
  describe :gem do
    subject do
      file = File.join( dir, '../repo/gems/h/hufflepuf-0.1.0-universal-java-1.5.gem' )
      Nexus::GemspecHelperImpl.from_gem( file )
    end

    it 'has the correct metadata' do
      subject.name.must_equal 'hufflepuf'
      subject.filename.must_equal 'hufflepuf-0.1.0-universal-java-1.5.gem'
      subject.pom( false ).must_equal File.read( File.join( dir, 'hufflepuf.pom' ) ).gsub( /^.*<project>/m, '<project>' )
      subject.pom( true ).must_equal File.read( File.join( dir, 'hufflepuf.pom' ) ).gsub( /^.*<project>/m, '<project>' )
      subject.gemspec.class.must_equal Gem::Specification
      subject.gemspec.must_equal subject.runzip( subject.rz_input_stream )
    end
  end

  describe :gemspec do
    subject do
      file = File.join( dir, '../repo/quick/Marshal.4.8/p/pre-0.1.0.beta.gemspec.rz' )
      Nexus::GemspecHelperImpl.from_gemspec_rz( file )
    end

    it 'has the correct metadata' do
      subject.name.must_equal 'pre'
      subject.filename.must_equal 'pre-0.1.0.beta.gem'
      subject.gemspec.class.must_equal Gem::Specification
      subject.gemspec.must_equal subject.runzip( subject.rz_input_stream )      
    end

    it 'creates snapshot poms and non-snaphost poms' do
      subject.pom( false ).must_equal File.read( File.join( dir, 'pre.pom' ) ).gsub( /^.*<project>/m, '<project>' )
      subject.pom( true ).must_equal File.read( File.join( dir, 'pre-SNAPSHOT.pom' ) ).gsub( /^.*<project>/m, '<project>' )
    end
  end
end
