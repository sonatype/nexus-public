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

java_import org.sonatype.nexus.ruby.SpecsHelper

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
  class SpecsHelperImpl
    include SpecsHelper
    include RubygemsHelper

    # dump an empty string
    # @return [IO] stream to data
    def create_empty_specs
      dump_specs( [] )
    end
    
    # collect all version/platform of given specs index
    # for the given name. the string elements are {version}-{platform}
    # @param gemname [String]
    # @param source [IO, String] filename or an IO object of the specs index
    # @return [Array[String]] the version-platform data
    def list_all_versions( name, source )
      specs = load_specs( source )
      specs.select do |s| 
        s[0].to_s == name
      end.collect do |s|
        "#{s[1]}-#{s[2]}"
      end
    end

    # add the given spec to specs index. action depends on
    # given type to match the release and prerelease version
    # with specs index type. it returns nil of nothing changed
    # on the given index.
    # @param spec [Gem::Specification] to be added
    # @param source [IO, String] filename or an IO object of the specs index
    # @param type [String, Symbol] the type of specs index :release, :prerelease, :latest
    # @return [IO, NilClass] eiher the stream to changed specs index or nil
    def add_spec( spec, source, type )
      case type.to_s.downcase.to_sym
      when :latest
        do_add_spec( spec, source, true ) unless spec.version.prerelease?
      when :release
        do_add_spec( spec, source ) unless spec.version.prerelease?
      when :prerelease
        do_add_spec( spec, source ) if spec.version.prerelease?
      end
    end

    # delete the given spec from the specs index. action depends on
    # given type to match the release and prerelease version
    # with specs index type. it returns nil of nothing changed
    # on the given index.
    #
    # it assumes that the method is called with all three specs index
    # types and the last call has to be with "latest" type. 
    # the method will keep some state which is needed for the call with
    # "latest".
    #
    # @param spec [Gem::Specification] to be deleted
    # @param source [IO, String] filename or an IO object of the specs index
    # @param type [String, Symbol] the type of specs index :release, :prerelease, :latest
    # @return [IO, NilClass] eiher the stream to changed specs index or nil
    def delete_spec( spec, source, type )
      if @releases == false
        raise 'already run method with type == :latest once'
      end
      specs = load_specs( source )
      old_entry = [ spec.name, spec.version, spec.platform.to_s ]
      if specs.member? old_entry
        specs.delete old_entry
        case type.to_s.downcase.to_sym
        when :latest
          if @releases
            # assume @releases is up to date
            specs = regenerate_latest( @releases )
            @releases = false
          else
            raise 'did not run method with type == :release'
          end
        when :release
          @releases = specs
        end
        dump_specs( specs )
      end
    end

    # string representation with internal data
    # @return [String]
    def to_s
      self.inspect
    end

    private

    def do_add_spec( spec, source, latest = false )
      specs = load_specs( source )
      new_entry = [ spec.name, spec.version, spec.platform.to_s ]
      unless specs.member?( new_entry )
        if latest
          new_specs = regenerate_latest( specs + [ new_entry ] )
          dump_specs( new_specs ) if new_specs != specs
        else
          specs << new_entry
          dump_specs( specs )
        end
      end
    end
  end
end
