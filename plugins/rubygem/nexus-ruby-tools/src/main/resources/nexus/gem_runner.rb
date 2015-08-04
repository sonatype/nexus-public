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

require 'rubygems/gem_runner'
require 'rubygems/exceptions'
require 'rubygems/spec_fetcher'
require 'stringio'

module Nexus
  class GemRunner < Gem::GemRunner

    def load_plugins
      Gem.load_plugins
    end

    def exec( *args )

      out = StringIO.new
      err = StringIO.new
      Gem::DefaultUserInteraction.ui = Gem::StreamUI.new( STDIN, out, err )

      run args

      out.string

    rescue Gem::SystemExitException => e
      begin
        raise err.string if e.exit_code != 0
      rescue RuntimeError
        # happens when reaching user input
      end

      out.string

    ensure
      Gem::SpecFetcher.fetcher = nil
    end

  end
end
# this makes it easy for a scripting container to 
# create an instance of this class
Nexus::GemRunner
