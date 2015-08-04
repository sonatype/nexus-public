class Gem::Commands::NexusCommand < Gem::AbstractCommand

  def description
    'Upload a gem up to Nexus server'
  end

  def arguments
    "GEM       built gem to upload"
  end

  def usage
    "#{program_name} GEM"
  end

  def initialize
    super 'nexus', description
    add_proxy_option
  end

  def execute
    setup
    send_gem
  end

  def send_gem
    say "Uploading gem to Nexus..."

    path = get_one_gem_name

    response = make_request(:post, "gems/#{File.basename(path)}") do |request|
      request.body = Gem.read_binary(path)
      request.add_field("Content-Length", request.body.size)
      request.add_field("Content-Type", "application/octet-stream")
      request.add_field("Authorization", authorization.strip)
    end

    case response.code
    when "401"
      say "Unauthorized"
    when "400"
      say "something went wrong - maybe (re)deployment is not allowed"
    when "500"
      say "something went wrong"
    else
      say response.message
    end
  end
end
