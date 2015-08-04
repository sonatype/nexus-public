# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "always_verify_ssl_certificates"
  s.version = "0.3.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["James Golick"]
  s.date = "2011-03-17"
  s.description = "Ruby\342\200\231s net/http is setup to never verify SSL certificates by default. Most ruby libraries do the same. That means that you\342\200\231re not verifying the identity of the server you\342\200\231re communicating with and are therefore exposed to man in the middle attacks. This gem monkey-patches net/http to force certificate verification and make turning it off impossible."
  s.email = "jamesgolick@gmail.com"
  s.extra_rdoc_files = ["LICENSE", "README.rdoc"]
  s.files = ["LICENSE", "README.rdoc"]
  s.homepage = "http://github.com/jamesgolick/always_verify_ssl_certificates"
  s.rdoc_options = ["--charset=UTF-8"]
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.21"
  s.summary = "Force net/http to always verify SSL certificates."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
