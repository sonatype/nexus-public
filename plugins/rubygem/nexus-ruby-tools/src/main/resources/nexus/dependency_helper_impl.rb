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

java_import org.sonatype.nexus.ruby.DependencyHelper

# this class can collect dependency data (which are an array of hashes).
# the collected data can be added as either marshaled stream of dependency
# data or marshaled stream of rzipped gemspec files.
#
# the basic idea is either to merge those dependency data and then
# marshal them again. or to have an array of dependecy data and
# split or group with their gemname as criterium.
#
# @author Christian Meier
module Nexus
  class DependencyHelperImpl
    include DependencyHelper
    include RubygemsHelper

    # create an object with no dependencies added
    def initialize
      @result = []
    end

    # add dependencies
    # @param dep [IO, String] can be filename or an IO object
    def add( is )
      @result += marshal_load( is )
    end

    # add dependency from given (rzipped) gemspec file
    # @param dep [IO, String] can be filename or an IO object
    def add_gemspec( is )
      spec = runzip( is )
      @result << dependency_data( spec.name, 
                                  spec.version.to_s,
                                  spec.platform.to_s, 
                                  deps_from( spec ) )
    end

    # return the list of gemnames from the dependency added so far.
    # freezes the state, i.e. adding more dependencies or gemspecs will
    # raise errors.
    # @return [Array[String]]
    def gemnames
      map.keys
    end

    # get the marshaled array of dependencies as stream for the
    # given gemname. this method will freeze the state, i.e. adding
    # more dependencies or gemspecs will raise errors.
    # @param gemname [String]
    # @return [IO] array of dependencies as stream
    def input_stream_of( gemname )
      marshal_dump( map[ gemname ] || [] )
    end
    alias :get_input_stream_of :input_stream_of

    # get the marshaled array of all dependencies as stream with or
    # without duplicates
    # @param unique [boolean]
    # @return [IO] array of dependencies as stream
    def input_stream( unique )
      r = if unique
            @result.uniq { |n| "#{n[:name]}-#{n[:number]}-#{n[:platform]}" }
          else
            @result
          end
      marshal_dump( r )
    end
    alias :get_input_stream :input_stream

    # string representation with internal data
    # @return [String]
    def to_s
      @result.inspect
    end

    private

    def map
      @map ||= split
    end
    
    def split
      @result.freeze
      map = {}
      @result.each do |d|
        bucket = map[ d[:name] ] ||= []
        bucket << d
      end 
      map
    end

    def deps_from( spec )
      spec.runtime_dependencies.collect do |d|
        # issue https://github.com/sonatype/nexus-ruby-support/issues/25
        name = case d.name
               when Array
                 d.name.first
               else
                 d.name
               end
        [ name, d.requirement.to_s ]
      end
    end

    def dependency_data( gemname, number, platform, deps )
      { :name => gemname,
        :number => number,
        :platform => platform,
        :dependencies => deps }
    end
  end
end
