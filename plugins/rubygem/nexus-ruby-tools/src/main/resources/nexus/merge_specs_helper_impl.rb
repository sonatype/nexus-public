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

java_import org.sonatype.nexus.ruby.MergeSpecsHelper

# this class can collect specs-index data (which are an array of arrays).
# there are no duplicates in the merged data and the output can be
# set to have only latest version of each 'gem' inside the array.
#
# @author Christian Meier
module Nexus
  class MergeSpecsHelperImpl
    include MergeSpecsHelper
    include RubygemsHelper

    # contructor to setup internal empty array
    def initialize
      @result = []
    end

    # add a specs index to object
    # @para source [IO, String] stream or filename of specs
    def add( source )
      @result += load_specs( source )
    end

    # returns a marshaled stream on the data either for all gems or 
    # for the latest gems
    # @param latest [boolean] whether only the data of the lastest or all
    # @return [IO] marshaled merge specs data as stream
    def input_stream( latest )
      if latest
        dump_specs( regenerate_latest( @result ) )
      else
        dump_specs( @result )
      end
    ensure
      @result.freeze      
    end
    alias :get_input_stream :input_stream

    # string representation with internal data
    # @return [String]
    def to_s
      @result.inspect
    end
  end
end
