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

require 'nexus/repair_helper_impl'
require 'minitest/spec'
require 'minitest/autorun'
require 'stringio'

describe Nexus::RepairHelperImpl do

  subject { Nexus::RepairHelperImpl.new }

  let( :broken_from ) do
    File.join( 'src', 'test', 'resources', 'broken' )
  end

  let( :broken_to ) do
    File.join( 'target', 'broken' )
  end

  before do
    FileUtils.rm_rf( broken_to )
    FileUtils.cp_r( broken_from, broken_to )
  end

  it 'purge api files' do
    subject.purge_broken_depencency_files( broken_to )
    dirs = Dir[ File.join( broken_to, 'api', '**', '*' ) ]
    dirs.each do |f|
      if File.file?( f )
        f.must_match /.ruby$/
      else
        File.directory?( f ).must_equal true
      end
    end
    dirs.size.must_equal 3
  end

  it 'purge gemspec files' do
    subject.purge_broken_gemspec_files( broken_to )
    dirs = Dir[ File.join( broken_to, 'quick', '**', '*' ) ]
    dirs.each do |f|
      File.directory?( f ).must_equal true
    end
    dirs.size.must_equal 2
  end

  it 'rebuild rubygems metadata' do
    subject.recreate_rubygems_index( broken_to )
    Dir[ File.join( broken_to, '*specs.4.8.gz' ) ].size.must_equal 3
    Dir[ File.join( broken_to, '*specs.4.8' ) ].size.must_equal 0
    Dir[ File.join( broken_to, '*' ) ].size.must_equal 6
    # this includes all the defaultgems from jruby
    # also includes all the gems coming from maven-tools dependency !!
    # i.e. a new jruby.version can change that number !!
    # puts Dir[ File.join( broken_to, 'quick', '**', '*' ) ].join("\n")
    Dir[ File.join( broken_to, 'quick', '**', '*' ) ].size.must_equal 32
  end

end
