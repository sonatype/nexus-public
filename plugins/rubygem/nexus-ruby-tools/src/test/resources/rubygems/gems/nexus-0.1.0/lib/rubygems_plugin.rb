require 'rubygems/command_manager'
require 'commands/abstract_command'

require "commands/nexus"
Gem::CommandManager.instance.register_command :nexus

