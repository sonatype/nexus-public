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

module Nexus
  class Check

    def check_gemspec_rz(gemfile, gemspec)
      spec_from_gem = Gem::Package.new( gemfile ).spec
      spec_from_gemspec = Marshal.load( Gem.inflate( Gem.read_binary( gemspec) ) )
      spec_from_gem == spec_from_gemspec
    end

    def check_spec_name( gemfile )
      Gem::Format.from_file_by_path( gemfile ).spec.name
    end

    def specs_size( specsfile )
      specs = Marshal.load( Gem.read_binary( specsfile ) )
      specs.size
    end

  end
end
Nexus::Check.new
