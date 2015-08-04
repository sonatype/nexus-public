require 'bundler/fetcher'
require 'bundler/rubygems_integration'
require 'bundler/rubygems_mirror'
module Bundler
  class RubygemsIntegration
    def download_gem(spec, uri, path)
      uri = RubygemsMirror.to_uri(uri)
      Gem::RemoteFetcher.fetcher.download(spec, uri, path)
    end
  end
end
module Bundler
  # Handles all the fetching with the rubygems server
  class Fetcher
    def initialize(remote_uri)
      @remote_uri = RubygemsMirror.to_uri(remote_uri)
      @has_api    = true # will be set to false if the rubygems index is ever fetched
      @@connection ||= Net::HTTP::Persistent.new nil, :ENV
    end
  end
end
