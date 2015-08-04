--- !ruby/object:Gem::Specification 
name: memcache-client
version: !ruby/object:Gem::Version 
  version: 1.6.3
platform: ruby
authors: 
- Eric Hodel
- Robert Cottrell
- Mike Perham
autorequire: 
bindir: bin
cert_chain: []

date: 2009-02-14 06:00:00 +00:00
default_executable: 
dependencies: 
- !ruby/object:Gem::Dependency 
  name: 
  - RubyInline
  type: :runtime
  version_requirement: 
  version_requirements: !ruby/object:Gem::Requirement 
    requirements: 
    - - ">="
      - !ruby/object:Gem::Version 
        version: "0"
    version: 
description: A Ruby library for accessing memcached.
email: mperham@gmail.com
executables: []

extensions: []

extra_rdoc_files: []

files: 
- README.rdoc
- LICENSE.txt
- History.txt
- Rakefile
- lib/continuum.rb
- lib/memcache.rb
- lib/memcache_util.rb
has_rdoc: false
homepage: http://github.com/mperham/memcache-client
licenses: 
post_install_message: 
rdoc_options: []

require_paths: 
- lib
required_ruby_version: !ruby/object:Gem::Requirement 
  requirements: 
  - - ">="
    - !ruby/object:Gem::Version 
      version: "0"
  version: 
required_rubygems_version: !ruby/object:Gem::Requirement 
  requirements: 
  - - ">="
    - !ruby/object:Gem::Version 
      version: "0"
  version: 
requirements: []

rubyforge_project: seattlerb
rubygems_version: 1.3.7
signing_key: 
specification_version: 2
summary: A Ruby library for accessing memcached.
test_files: 
- test/test_mem_cache.rb

