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

import type { PyPIResult, PyPISearchResponse, PyPIDetail, PyPISearchFilters } from './pypi.types';

/**
 * Mock PyPI package data for development and testing.
 */
export const mockPyPIResults: PyPIResult[] = [
  {
    id: 'pypi:requests',
    name: 'requests',
    displayName: 'requests',
    latestVersion: '2.31.0',
    versionsCount: 142,
    summary: 'Python HTTP for Humans.',
    author: 'Kenneth Reitz',
    license: 'Apache 2.0',
    keywords: ['http', 'client', 'urllib'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'License :: OSI Approved :: Apache Software License'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'pypi:numpy',
    name: 'numpy',
    displayName: 'numpy',
    latestVersion: '1.26.3',
    versionsCount: 256,
    summary: 'Fundamental package for array computing in Python',
    author: 'NumPy Developers',
    license: 'BSD-3-Clause',
    keywords: ['numpy', 'array', 'scientific', 'computing'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Programming Language :: Python :: 3'],
    repositoriesCount: 3,
    lastUpdated: '2024-01-15T14:22:00Z',
  },
  {
    id: 'pypi:pandas',
    name: 'pandas',
    displayName: 'pandas',
    latestVersion: '2.2.0',
    versionsCount: 189,
    summary: 'Powerful data structures for data analysis',
    author: 'The Pandas Development Team',
    license: 'BSD-3-Clause',
    keywords: ['pandas', 'data', 'analysis', 'dataframe'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Topic :: Scientific/Engineering'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-18T09:15:00Z',
  },
  {
    id: 'pypi:flask',
    name: 'Flask',
    displayName: 'Flask',
    latestVersion: '3.0.1',
    versionsCount: 78,
    summary: 'A simple framework for building complex web applications.',
    author: 'Pallets',
    license: 'BSD-3-Clause',
    keywords: ['web', 'framework', 'wsgi'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Framework :: Flask'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-10T16:45:00Z',
  },
  {
    id: 'pypi:django',
    name: 'Django',
    displayName: 'Django',
    latestVersion: '5.0.1',
    versionsCount: 234,
    summary: 'A high-level Python web framework',
    author: 'Django Software Foundation',
    license: 'BSD-3-Clause',
    keywords: ['web', 'framework', 'django'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Framework :: Django'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-08T14:00:00Z',
  },
  {
    id: 'pypi:pytest',
    name: 'pytest',
    displayName: 'pytest',
    latestVersion: '8.0.0',
    versionsCount: 156,
    summary: 'pytest: simple powerful testing with Python',
    author: 'Holger Krekel and pytest-dev team',
    license: 'MIT',
    keywords: ['test', 'testing', 'pytest'],
    classifiers: ['Development Status :: 6 - Mature', 'Topic :: Software Development :: Testing'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-19T11:00:00Z',
  },
  {
    id: 'pypi:boto3',
    name: 'boto3',
    displayName: 'boto3',
    latestVersion: '1.34.25',
    versionsCount: 567,
    summary: 'The AWS SDK for Python',
    author: 'Amazon Web Services',
    license: 'Apache 2.0',
    keywords: ['aws', 'amazon', 'cloud', 'sdk'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'License :: OSI Approved :: Apache Software License'],
    repositoriesCount: 3,
    lastUpdated: '2024-01-20T08:30:00Z',
  },
  {
    id: 'pypi:tensorflow',
    name: 'tensorflow',
    displayName: 'tensorflow',
    latestVersion: '2.15.0',
    versionsCount: 312,
    summary: 'TensorFlow is an open source machine learning framework',
    author: 'Google Inc.',
    license: 'Apache 2.0',
    keywords: ['tensorflow', 'machine learning', 'deep learning', 'ai'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Topic :: Scientific/Engineering :: Artificial Intelligence'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-05T12:00:00Z',
  },
  {
    id: 'pypi:scikit-learn',
    name: 'scikit-learn',
    displayName: 'scikit-learn',
    latestVersion: '1.4.0',
    versionsCount: 98,
    summary: 'A set of python modules for machine learning and data mining',
    author: 'scikit-learn developers',
    license: 'BSD-3-Clause',
    keywords: ['machine learning', 'sklearn', 'classification', 'regression'],
    classifiers: ['Development Status :: 5 - Production/Stable', 'Topic :: Scientific/Engineering'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T10:00:00Z',
  },
  {
    id: 'pypi:pillow',
    name: 'Pillow',
    displayName: 'Pillow',
    latestVersion: '10.2.0',
    versionsCount: 145,
    summary: 'Python Imaging Library (Fork)',
    author: 'Jeffrey A. Clark',
    license: 'MIT-CMU',
    keywords: ['pil', 'image', 'pillow', 'imaging'],
    classifiers: ['Development Status :: 6 - Mature', 'Topic :: Multimedia :: Graphics'],
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T08:00:00Z',
  },
];

/**
 * Mock PyPI detail data.
 */
export const mockPyPIDetail: PyPIDetail = {
  id: 'pypi:requests',
  name: 'requests',
  displayName: 'requests',
  summary: 'Python HTTP for Humans.',
  description: `Requests is a simple, yet elegant, HTTP library.

## Features

Requests allows you to send HTTP/1.1 requests extremely easily. There's no need to manually add query strings to your URLs, or to form-encode your PUT & POST data — but nowadays, just use the json method!

## Usage

>>> import requests
>>> r = requests.get('https://api.github.com/user', auth=('user', 'pass'))
>>> r.status_code
200
>>> r.headers['content-type']
'application/json; charset=utf8'`,
  author: 'Kenneth Reitz',
  authorEmail: 'me@kennethreitz.org',
  license: 'Apache 2.0',
  homepage: 'https://requests.readthedocs.io',
  projectUrl: 'https://github.com/psf/requests',
  keywords: ['http', 'client', 'urllib', 'requests'],
  classifiers: [
    'Development Status :: 5 - Production/Stable',
    'License :: OSI Approved :: Apache Software License',
    'Programming Language :: Python :: 3',
    'Programming Language :: Python :: 3.8',
    'Programming Language :: Python :: 3.9',
    'Programming Language :: Python :: 3.10',
    'Programming Language :: Python :: 3.11',
    'Programming Language :: Python :: 3.12',
  ],
  versions: [
    { version: '2.31.0', published: '2024-01-20T10:30:00Z', repository: 'pypi-hosted', requiresPython: '>=3.7' },
    { version: '2.30.0', published: '2023-11-15T10:00:00Z', repository: 'pypi-hosted', requiresPython: '>=3.7' },
    { version: '2.29.0', published: '2023-08-22T09:00:00Z', repository: 'pypi-hosted', requiresPython: '>=3.7' },
    { version: '2.28.2', published: '2023-04-10T08:00:00Z', repository: 'pypi-proxy', requiresPython: '>=3.7' },
    { version: '2.28.1', published: '2022-12-15T12:00:00Z', repository: 'pypi-proxy', requiresPython: '>=3.7' },
  ],
  repositories: ['pypi-hosted', 'pypi-proxy', 'pypi-group'],
  requiresPython: '>=3.7',
};

/**
 * Simulates PyPI search API call with mock data.
 */
export async function mockPyPISearchApi(filters: PyPISearchFilters): Promise<PyPISearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockPyPIResults];

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

  // Filter by summary
  if (filters.summary) {
    const s = filters.summary.toLowerCase();
    filtered = filtered.filter((r) =>
      r.summary?.toLowerCase().includes(s)
    );
  }

  // Filter by keywords
  if (filters.keywords) {
    const k = filters.keywords.toLowerCase();
    filtered = filtered.filter((r) =>
      r.keywords?.some((kw) => kw.toLowerCase().includes(k))
    );
  }

  // Filter by classifiers
  if (filters.classifiers) {
    const c = filters.classifiers.toLowerCase();
    filtered = filtered.filter((r) =>
      r.classifiers?.some((cl) => cl.toLowerCase().includes(c))
    );
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates PyPI detail API call.
 */
export async function mockPyPIDetailApi(id: string): Promise<PyPIDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockPyPIDetail;
}


