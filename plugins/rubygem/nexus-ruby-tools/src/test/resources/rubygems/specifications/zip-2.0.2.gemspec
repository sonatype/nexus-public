# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "zip"
  s.version = "2.0.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Postmodern", "Thomas Sondergaard", "Sam Lown"]
  s.date = "2010-05-26"
  s.description = "zip is a Ruby library for reading and writing Zip files. Unlike the official rubyzip, zip is compatible with Ruby 1.9.1."
  s.email = ["me@samlown.com", "postmodern.mod3@gmail.com"]
  s.extra_rdoc_files = ["ChangeLog.txt", "README"]
  s.files = ["ChangeLog.txt", "README"]
  s.homepage = "http://github.com/postmodern/rubyzip2"
  s.rdoc_options = ["--charset=UTF-8"]
  s.require_paths = ["lib"]
  s.rubygems_version = "1.8.21"
  s.summary = "zip is a Ruby library for reading and writing Zip files"

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
