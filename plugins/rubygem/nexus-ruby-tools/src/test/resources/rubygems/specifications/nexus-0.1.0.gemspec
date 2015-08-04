# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "nexus"
  s.version = "0.1.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Nick Quaranto"]
  s.date = "2012-09-05"
  s.description = "Adds a command to RubyGems for uploading gems to a nexus server."
  s.email = ["nick@quaran.to"]
  s.executables = ["nbundle"]
  s.files = ["bin/nbundle"]
  s.homepage = "https://github.com/sonatype/nexus-ruby-support/tree/master/nexus-gem"
  s.post_install_message = "\n========================================================================\n\n           Thanks for installing Nexus gem! You can now run:\n\n    gem nexus          publish your gems onto Nexus server\n\n    nbundle            a bundler fork with mirror support. \n                       just add a mirror with:\n\n    bundle config mirror.http://rubygems.org http://localhost:8081/nexus/content/repositories/rubygems.org\n\n                       and use 'nbundle' instead of 'bundle'\n\n========================================================================\n\n"
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.21"
  s.summary = "Commands to interact with nexus server"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<always_verify_ssl_certificates>, ["~> 0.3.0"])
      s.add_development_dependency(%q<rake>, ["= 0.9.2.2"])
      s.add_development_dependency(%q<shoulda>, ["~> 3.1.1"])
      s.add_development_dependency(%q<webmock>, ["~> 1.8.8"])
      s.add_development_dependency(%q<rr>, [">= 0"])
    else
      s.add_dependency(%q<always_verify_ssl_certificates>, ["~> 0.3.0"])
      s.add_dependency(%q<rake>, ["= 0.9.2.2"])
      s.add_dependency(%q<shoulda>, ["~> 3.1.1"])
      s.add_dependency(%q<webmock>, ["~> 1.8.8"])
      s.add_dependency(%q<rr>, [">= 0"])
    end
  else
    s.add_dependency(%q<always_verify_ssl_certificates>, ["~> 0.3.0"])
    s.add_dependency(%q<rake>, ["= 0.9.2.2"])
    s.add_dependency(%q<shoulda>, ["~> 3.1.1"])
    s.add_dependency(%q<webmock>, ["~> 1.8.8"])
    s.add_dependency(%q<rr>, [">= 0"])
  end
end
