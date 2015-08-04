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

java_import org.sonatype.nexus.ruby.DependencyData

# parsed dependency data and restructure to retrieve the platform for
# given version or give the list of all version.
#
# @author Christian Meier
module Nexus
  class DependencyDataImpl
    include DependencyData
    include RubygemsHelper
    
    attr_reader :name, :modified

    # contructor parsing dependency data
    # @param data [Array, IO, String] either the unmarshalled array or 
    #        the stream or filename to the marshalled data
    def initialize( data, name, modified )
      data = marshal_load( data ) unless data.is_a?( Array )
      @name = name
      @modified = modified
      @versions = {}
      data.sort do |m,n|
        Gem::Version.new(m[:number]) <=> Gem::Version.new(n[:number])
      end.select do |d|
        is_ruby = d[:platform].downcase =~ /ruby/ 
        is_java = d[:platform].downcase =~ /(java|jruby)/ 
        if is_ruby
          @versions[ d[:number] ] ||= d[:platform]
        end
        if is_java
          # java overwrites since it has higher prio
          @versions[ d[:number] ] = d[:platform]
        end
      end
    end
    
    # returns all the version
    # @param prereleases [boolean] whether or not to include prereleased
    #         versions
    # @return [Array] an array of versions
    def versions( prereleases )
      if prereleases
        @versions.keys.select { |v| v =~ /[a-zA-Z]/ }
      else
        @versions.keys.select { |v| ! ( v =~ /[a-zA-Z]/ ) }
      end
    end

    # retrieve the platform for a given version
    # @param version [String] the version to query
    # @return [String] the platform
    def platform( version )
      @versions[ version ]
    end
  end
end
