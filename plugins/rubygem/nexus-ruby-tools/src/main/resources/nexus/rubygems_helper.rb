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

java_import java.io.ByteArrayInputStream

# this module just has a bunch of helper method dealing
# with reading binary data from a file or stream. marshalling
# and unmarshalling binary data, dito. rzip and runzip them.
#
# @author Christian Meier
module Nexus
  module RubygemsHelper

    # read binary data either from a file or a stream
    # @param io [IO, String] either a stream or a filename
    # @return [String] packed as character data
    def read_binary( io )
      case io
      when String
        Gem.read_binary( io )
      else
        result = []
        while ( ( b = io.read ) != -1 ) do
          result << b
        end
        result.pack 'C*'
      end
    end

    # ruby-unzip a stream or file and unmarshal it to an object.
    # @param io [IO, String] stream or filename
    # @return [Object] unmarshalled object
    def runzip( io )
      Marshal.load( Gem.inflate( read_binary( io ) ) )
    end

    # marshal a given object and turn it into a ruby-zip stream.
    # @param obj [Object] any ruby object
    # @return [IO] stream with rzipped marshalled object
    def rzip( obj )
      ByteArrayInputStream.new( Gem.deflate( Marshal.dump( obj ) ).to_java_bytes )
    end

    # unmarshal object from stream or file
    # @param io [IO, String] stream or filename
    # @return [Object] unmarshalled object
    def marshal_load( io )
      Marshal.load( read_binary( io ) )
    end

    # marshal given object and turn it to a stream
    # @param obj [Object] any ruby object
    # @return [IO] stream of the marshalled object
    def marshal_dump( obj)
      ByteArrayInputStream.new( Marshal.dump( obj ).to_java_bytes )
    end

    def load_specs( io )
      marshal_load( io )
    end

    def dump_specs( specs )
      specs.uniq!
      specs.sort!
      marshal_dump( compact_specs( specs ) )
    end

    def compact_specs( specs )
      names = {}
      versions = {}
      platforms = {}

      specs.map do |( name, version, platform )|
        names[ name ] = name unless names.include? name
        versions[ version ] = version unless versions.include? version
        platforms[ platform ] = platform unless platforms.include? platform

        [ names[ name ], versions[ version ], platforms[ platform ] ]
      end
    end

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

    def regenerate_latest( specs )
      specs.sort!
      specs.uniq!
      map = {}
      specs.each do |s|
        list = map[ s[ 0 ] ] ||= []
        list << s
      end
      result = []
      map.each do |name, list|
        list.sort!
        list.uniq!
        lastest_versions = {}
        list.each do |i|
          version = i[1]
          platform = i[2]
          lastest_versions[ platform ] = i
        end
        result += lastest_versions.collect { |k, v| v }
      end
      result
    end
  end
end
