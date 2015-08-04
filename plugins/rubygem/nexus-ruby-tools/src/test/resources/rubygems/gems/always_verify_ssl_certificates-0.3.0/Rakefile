require 'rubygems'
require 'rake'

begin
  require 'jeweler'
  Jeweler::Tasks.new do |gem|
    gem.name = "always_verify_ssl_certificates"
    gem.summary = %Q{Force net/http to always verify SSL certificates.}
    gem.description = %Q{Ruby’s net/http is setup to never verify SSL certificates by default. Most ruby libraries do the same. That means that you’re not verifying the identity of the server you’re communicating with and are therefore exposed to man in the middle attacks. This gem monkey-patches net/http to force certificate verification and make turning it off impossible.}
    gem.email = "jamesgolick@gmail.com"
    gem.homepage = "http://github.com/jamesgolick/always_verify_ssl_certificates"
    gem.authors = ["James Golick"]
  end
  Jeweler::GemcutterTasks.new
rescue LoadError
  puts "Jeweler (or a dependency) not available. Install it with: gem install jeweler"
end

require 'rake/testtask'
Rake::TestTask.new(:test) do |test|
  test.libs << 'lib' << 'test'
  test.pattern = 'test/**/test_*.rb'
  test.verbose = true
end

begin
  require 'rcov/rcovtask'
  Rcov::RcovTask.new do |test|
    test.libs << 'test'
    test.pattern = 'test/**/test_*.rb'
    test.verbose = true
  end
rescue LoadError
  task :rcov do
    abort "RCov is not available. In order to run rcov, you must: sudo gem install spicycode-rcov"
  end
end

task :test => :check_dependencies

task :default => :test

require 'rake/rdoctask'
Rake::RDocTask.new do |rdoc|
  version = File.exist?('VERSION') ? File.read('VERSION') : ""

  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = "always_verify_ssl_certificates #{version}"
  rdoc.rdoc_files.include('README*')
  rdoc.rdoc_files.include('lib/**/*.rb')
end
