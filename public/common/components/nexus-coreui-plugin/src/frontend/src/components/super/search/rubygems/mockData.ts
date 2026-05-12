/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import type { RubyGemsResult, RubyGemsSearchResponse, RubyGemsDetail, RubyGemsSearchFilters } from './rubygems.types';

/**
 * Mock RubyGems data for development and testing.
 */
export const mockRubyGemsResults: RubyGemsResult[] = [
  {
    id: 'rubygems:rails',
    name: 'rails',
    displayName: 'rails',
    latestVersion: '7.1.3',
    versionsCount: 523,
    platform: 'ruby',
    summary: 'Full-stack web application framework.',
    description: 'Ruby on Rails is a full-stack web framework optimized for programmer happiness.',
    authors: 'David Heinemeier Hansson',
    licenses: ['MIT'],
    homepage: 'https://rubyonrails.org',
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'rubygems:bundler',
    name: 'bundler',
    displayName: 'bundler',
    latestVersion: '2.5.4',
    versionsCount: 245,
    platform: 'ruby',
    summary: 'The best way to manage your Ruby application dependencies.',
    authors: 'Bundler Contributors',
    licenses: ['MIT'],
    homepage: 'https://bundler.io',
    repositoriesCount: 3,
    lastUpdated: '2024-01-15T14:22:00Z',
  },
  {
    id: 'rubygems:rake',
    name: 'rake',
    displayName: 'rake',
    latestVersion: '13.1.0',
    versionsCount: 178,
    platform: 'ruby',
    summary: 'Rake is a Make-like program implemented in Ruby.',
    authors: 'Hiroshi SHIBATA, Eric Hodel, Jim Weirich',
    licenses: ['MIT'],
    homepage: 'https://ruby.github.io/rake',
    repositoriesCount: 2,
    lastUpdated: '2024-01-18T09:15:00Z',
  },
  {
    id: 'rubygems:rspec',
    name: 'rspec',
    displayName: 'rspec',
    latestVersion: '3.13.0',
    versionsCount: 156,
    platform: 'ruby',
    summary: 'BDD for Ruby',
    description: 'BDD for Ruby. RSpec is a testing tool for Ruby, created for behavior-driven development (BDD).',
    authors: 'Steven Baker, David Chelimsky, Myron Marston',
    licenses: ['MIT'],
    homepage: 'https://rspec.info',
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T16:45:00Z',
  },
  {
    id: 'rubygems:puma',
    name: 'puma',
    displayName: 'puma',
    latestVersion: '6.4.2',
    versionsCount: 134,
    platform: 'ruby',
    summary: 'A Ruby/Rack web server built for parallelism',
    authors: 'Evan Phoenix',
    licenses: ['BSD-3-Clause'],
    homepage: 'https://puma.io',
    repositoriesCount: 2,
    lastUpdated: '2024-01-08T14:00:00Z',
  },
  {
    id: 'rubygems:nokogiri',
    name: 'nokogiri',
    displayName: 'nokogiri',
    latestVersion: '1.16.0',
    versionsCount: 198,
    platform: 'ruby',
    summary: 'Nokogiri is an HTML, XML, SAX, and Reader parser.',
    description: 'Nokogiri parses and searches XML/HTML using native libraries.',
    authors: 'Mike Dalessio, Aaron Patterson',
    licenses: ['MIT'],
    homepage: 'https://nokogiri.org',
    repositoriesCount: 3,
    lastUpdated: '2024-01-19T11:00:00Z',
  },
  {
    id: 'rubygems:devise',
    name: 'devise',
    displayName: 'devise',
    latestVersion: '4.9.3',
    versionsCount: 145,
    platform: 'ruby',
    summary: 'Flexible authentication solution for Rails with Warden.',
    authors: 'Jose Valim, Carlos Souza',
    licenses: ['MIT'],
    homepage: 'https://github.com/heartcombo/devise',
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T08:30:00Z',
  },
  {
    id: 'rubygems:sidekiq',
    name: 'sidekiq',
    displayName: 'sidekiq',
    latestVersion: '7.2.1',
    versionsCount: 267,
    platform: 'ruby',
    summary: 'Simple, efficient background processing for Ruby.',
    authors: 'Mike Perham',
    licenses: ['LGPL-3.0'],
    homepage: 'https://sidekiq.org',
    repositoriesCount: 2,
    lastUpdated: '2024-01-05T12:00:00Z',
  },
  {
    id: 'rubygems:activerecord',
    name: 'activerecord',
    displayName: 'activerecord',
    latestVersion: '7.1.3',
    versionsCount: 489,
    platform: 'ruby',
    summary: 'Object-relational mapping layer for Rails.',
    authors: 'David Heinemeier Hansson',
    licenses: ['MIT'],
    homepage: 'https://rubyonrails.org',
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'rubygems:jruby-openssl',
    name: 'jruby-openssl',
    displayName: 'jruby-openssl',
    latestVersion: '0.14.3',
    versionsCount: 45,
    platform: 'java',
    summary: 'JRuby OpenSSL library',
    authors: 'Ola Bini, Karol Bucek',
    licenses: ['MIT', 'GPL-2.0'],
    homepage: 'https://github.com/jruby/jruby-openssl',
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T08:00:00Z',
  },
];

/**
 * Mock RubyGems detail data.
 */
export const mockRubyGemsDetail: RubyGemsDetail = {
  id: 'rubygems:rails',
  name: 'rails',
  displayName: 'rails',
  summary: 'Full-stack web application framework.',
  description: `Ruby on Rails is a full-stack web framework optimized for programmer happiness and sustainable productivity. It encourages beautiful code by favoring convention over configuration.

Rails includes:
- Action Pack for controller and view handling
- Active Record for ORM database access
- Action Mailer for email handling
- Active Job for background processing
- Action Cable for WebSocket support`,
  authors: 'David Heinemeier Hansson',
  licenses: ['MIT'],
  homepage: 'https://rubyonrails.org',
  sourceCodeUri: 'https://github.com/rails/rails',
  documentationUri: 'https://api.rubyonrails.org',
  versions: [
    { version: '7.1.3', platform: 'ruby', published: '2024-01-20T10:30:00Z', repository: 'rubygems-hosted', rubyVersion: '>= 2.7.0' },
    { version: '7.1.2', platform: 'ruby', published: '2023-11-10T10:00:00Z', repository: 'rubygems-hosted', rubyVersion: '>= 2.7.0' },
    { version: '7.1.1', platform: 'ruby', published: '2023-10-15T09:00:00Z', repository: 'rubygems-hosted', rubyVersion: '>= 2.7.0' },
    { version: '7.0.8', platform: 'ruby', published: '2023-09-01T08:00:00Z', repository: 'rubygems-proxy', rubyVersion: '>= 2.7.0' },
    { version: '6.1.7.6', platform: 'ruby', published: '2023-06-29T12:00:00Z', repository: 'rubygems-proxy', rubyVersion: '>= 2.5.0' },
  ],
  repositories: ['rubygems-hosted', 'rubygems-proxy', 'rubygems-group'],
  rubyVersion: '>= 2.7.0',
};

/**
 * Simulates RubyGems search API call with mock data.
 */
export async function mockRubyGemsSearchApi(filters: RubyGemsSearchFilters): Promise<RubyGemsSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockRubyGemsResults];

  // Filter by name
  if (filters.name) {
    const n = filters.name.toLowerCase();
    filtered = filtered.filter((r) =>
      r.name.toLowerCase().includes(n) || r.displayName.toLowerCase().includes(n)
    );
  }

  // Filter by version (exact match)
  if (filters.version) {
    filtered = filtered.filter((r) => r.latestVersion === filters.version);
  }

  // Filter by platform
  if (filters.platform) {
    const p = filters.platform.toLowerCase();
    filtered = filtered.filter((r) => r.platform.toLowerCase() === p);
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates RubyGems detail API call.
 */
export async function mockRubyGemsDetailApi(id: string): Promise<RubyGemsDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockRubyGemsDetail;
}


