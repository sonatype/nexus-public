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

require 'bundler'
require 'bundler/cli'
require 'stringio'

module Nexus

  class BundleRunner

    def _out
      @out ||= StringIO.new
    end
    def _err
      @err ||= StringIO.new
    end

    def exec( *args )
      $stdout = _out
      $stderr = _err

      ENV['PATH'] ||= '' # just make sure bundler has a PATH variable

      Bundler::CLI.start( args )

      _out.string

    rescue SystemExit => e
      raise err.string if e.exit_code != 0

      out.string

    rescue Exception => e
      STDOUT.puts out.string
      STDERR.puts err.string
      trace = e.backtrace.join("\n\t")
      raise "#{e.message}\n\t#{trace}"

    ensure
      $stdout = STDOUT
      $stderr = STDERR
    end

  end
end
# this makes it easy for a scripting container to
# create an instance of this class
Nexus::BundleRunner
