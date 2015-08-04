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

require 'nexus/merge_specs_helper_impl'
require 'minitest/spec'
require 'minitest/autorun'
require 'stringio'

describe Nexus::MergeSpecsHelperImpl do

  # just create it the same as the java app would do it
  subject { Nexus::MergeSpecsHelperImpl.new }

  let( :a1java ) { [ 'a', '1', 'java' ] }
  let( :a2java ) { [ 'a', '2', 'java' ] }
  let( :a1 ) { ['a', '1', 'ruby' ] }
  let( :a2 ) { ['a', '2', 'ruby' ] }
  let( :b4 ) { ['b', '4', 'ruby' ] }

  let( :nothing ) do
    tmp = File.join( 'target', 'merge_nothing' )
    File.open( tmp, 'w' ){ |f| f.print Marshal.dump( [ a1java, a2, b4 ] ) }
    tmp
  end

  let( :something ) do
    tmp = File.join( 'target', 'merge_something' )
    File.open( tmp, 'w' ){ |f| f.print Marshal.dump( [ a2java, a2, a1 ] ) }
    tmp
  end

  it 'should merge nothing' do
    subject.add( nothing )
    subject.marshal_load( subject.input_stream( false ) ).must_equal [ a1java, a2, b4 ]
  end

  it 'should merge something' do
    subject.add( nothing )
    subject.add( something )
    subject.marshal_load( subject.input_stream( false ) ).must_equal [ a1java, a1, a2java, a2, b4 ]
  end

  it 'should merge something latest' do
    subject.add( nothing )
    subject.add( something )
    subject.marshal_load( subject.input_stream( true ) ).must_equal [ a2java, a2, b4 ]
  end
end
