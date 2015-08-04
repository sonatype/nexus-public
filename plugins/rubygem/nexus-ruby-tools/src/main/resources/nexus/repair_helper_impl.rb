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

require 'nexus/rubygems_helper'
require 'nexus/indexer'

java_import org.sonatype.nexus.ruby.RepairHelper

# some helper methods to purge broken files or to recreate
# the complete specs index of hosted rubygems repository.
#
# @author Christian Meier
module Nexus
  class RepairHelperImpl
    include RepairHelper
    include RubygemsHelper

    # recreate the complete rubygems specs index
    # @param directory [String] the path to directory where the
    #        repository is located
    def recreate_rubygems_index( directory )
      indexer = Nexus::Indexer.new( directory )
      indexer.generate_index
      indexer.remove_tmp_dir

      # delete obsolete files
      Dir[ File.join( directory, '*' ) ].each do |f|
        if !f.match( /.*specs.#{Gem.marshal_version}.gz/ ) && !File.directory?( f )
          FileUtils.rm_f( f )
        end
      end

      # NOTE that code gave all kinds of result but the expected
      #      could be jruby related or not.
      #      just leave the permissions as they are
      #
      # fix permissions 
      # mode = 16877 # File.new( directory ).stat.mode # does not work with jruby
      # ( [ directory ] + Dir[ File.join( directory, '**', '*') ] ).each do |f|
      #   begin
      #     if File.directory? f
      #       FileUtils.chmod( mode, f )
      #     end
      #   rescue
      #     # well - let it as it is
      #   end
      # end
      nil
    end

    # purge all dependency files which can not be loaded
    # @param directory [String] the path to directory where the
    #        repository is located
    def purge_broken_depencency_files( directory )
      Dir[ File.join( directory, 
                      'api', 'v1', 'dependencies', 
                      '*' ) ].each do |file|
        begin
          if File.file?( file ) and file =~ /.ruby$/
            marshal_load( file )
          else
            FileUtils.rm_rf( file )
          end
        rescue
          # just in case the file is directory delete it as well
          FileUtils.rm_rf( file )
        end
      end
      nil
    end

    # purge all gemspec files which can not be loaded
    # @param directory [String] the path to directory where the
    #        repository is located
    def purge_broken_gemspec_files( directory )
      d1 = Dir[ File.join( directory, 
                           'quick', "Marshal.#{Gem.marshal_version}",
                           '*', '*' ) ]
      # the new location under api as well
      d2 = Dir[ File.join( directory, 'api',
                           'quick', "Marshal.#{Gem.marshal_version}",
                           '*', '*' ) ]

      ( d1 + d2 ).each do |file|
        begin
          runzip( file )
        rescue
          # just in case the file is directory delete it as well
          FileUtils.rm_rf( file )
        end
      end
      nil
    end
  end
end
