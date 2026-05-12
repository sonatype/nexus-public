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

import type { HelmResult, HelmSearchResponse, HelmDetail, HelmSearchFilters } from './helm.types';

/**
 * Mock Helm chart data for development.
 */
export const mockHelmResults: HelmResult[] = [
  {
    id: 'helm:nginx-ingress',
    name: 'nginx-ingress',
    displayName: 'nginx-ingress',
    latestVersion: '4.9.0',
    appVersion: '3.4.0',
    versionsCount: 87,
    description: 'NGINX Ingress Controller for Kubernetes',
    icon: 'https://upload.wikimedia.org/wikipedia/commons/c/c5/Nginx_logo.svg',
    home: 'https://github.com/kubernetes/ingress-nginx',
    keywords: ['nginx', 'ingress', 'controller', 'kubernetes'],
    maintainers: [{ name: 'Kubernetes', email: 'kubernetes@nginx.com' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-20T10:30:00Z',
  },
  {
    id: 'helm:prometheus',
    name: 'prometheus',
    displayName: 'prometheus',
    latestVersion: '25.8.2',
    appVersion: '2.48.1',
    versionsCount: 156,
    description: 'Prometheus is a monitoring system and time series database',
    icon: 'https://raw.githubusercontent.com/prometheus/prometheus/main/documentation/images/prometheus-logo.svg',
    home: 'https://prometheus.io/',
    keywords: ['prometheus', 'monitoring', 'alerting', 'metrics'],
    maintainers: [{ name: 'Prometheus Community' }],
    repositoriesCount: 3,
    lastUpdated: '2024-01-18T14:22:00Z',
  },
  {
    id: 'helm:grafana',
    name: 'grafana',
    displayName: 'grafana',
    latestVersion: '7.0.19',
    appVersion: '10.2.3',
    versionsCount: 203,
    description: 'The leading tool for querying and visualizing time series and metrics',
    icon: 'https://raw.githubusercontent.com/grafana/grafana/main/public/img/grafana_icon.svg',
    home: 'https://grafana.com',
    keywords: ['grafana', 'dashboard', 'visualization', 'metrics'],
    maintainers: [{ name: 'Grafana Labs' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-19T09:15:00Z',
  },
  {
    id: 'helm:redis',
    name: 'redis',
    displayName: 'redis',
    latestVersion: '18.6.1',
    appVersion: '7.2.3',
    versionsCount: 178,
    description: 'Redis is an open source, advanced key-value cache and store',
    icon: 'https://bitnami.com/assets/stacks/redis/img/redis-stack-220x234.png',
    home: 'https://redis.io/',
    keywords: ['redis', 'cache', 'database', 'keyvalue'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-17T11:00:00Z',
  },
  {
    id: 'helm:postgresql',
    name: 'postgresql',
    displayName: 'postgresql',
    latestVersion: '14.0.1',
    appVersion: '16.1.0',
    versionsCount: 234,
    description: 'PostgreSQL is an open source object-relational database',
    icon: 'https://bitnami.com/assets/stacks/postgresql/img/postgresql-stack-220x234.png',
    home: 'https://www.postgresql.org/',
    keywords: ['postgresql', 'database', 'sql', 'replication'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 3,
    lastUpdated: '2024-01-16T08:30:00Z',
  },
  {
    id: 'helm:mongodb',
    name: 'mongodb',
    displayName: 'mongodb',
    latestVersion: '14.4.10',
    appVersion: '7.0.4',
    versionsCount: 189,
    description: 'MongoDB is a cross-platform document-oriented NoSQL database',
    icon: 'https://bitnami.com/assets/stacks/mongodb/img/mongodb-stack-220x234.png',
    home: 'https://www.mongodb.com/',
    keywords: ['mongodb', 'database', 'nosql', 'document'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-15T16:45:00Z',
  },
  {
    id: 'helm:elasticsearch',
    name: 'elasticsearch',
    displayName: 'elasticsearch',
    latestVersion: '19.17.3',
    appVersion: '8.11.3',
    versionsCount: 145,
    description: 'Elasticsearch is a distributed search and analytics engine',
    icon: 'https://bitnami.com/assets/stacks/elasticsearch/img/elasticsearch-stack-220x234.png',
    home: 'https://www.elastic.co/elasticsearch/',
    keywords: ['elasticsearch', 'search', 'analytics', 'logging'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-14T12:00:00Z',
  },
  {
    id: 'helm:kafka',
    name: 'kafka',
    displayName: 'kafka',
    latestVersion: '26.6.2',
    appVersion: '3.6.1',
    versionsCount: 167,
    description: 'Apache Kafka is a distributed streaming platform',
    icon: 'https://bitnami.com/assets/stacks/kafka/img/kafka-stack-220x234.png',
    home: 'https://kafka.apache.org/',
    keywords: ['kafka', 'streaming', 'messaging', 'queue'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-13T14:00:00Z',
  },
  {
    id: 'helm:rabbitmq',
    name: 'rabbitmq',
    displayName: 'rabbitmq',
    latestVersion: '12.8.0',
    appVersion: '3.12.10',
    versionsCount: 198,
    description: 'RabbitMQ is an open source message broker software',
    icon: 'https://bitnami.com/assets/stacks/rabbitmq/img/rabbitmq-stack-220x234.png',
    home: 'https://www.rabbitmq.com/',
    keywords: ['rabbitmq', 'messaging', 'queue', 'amqp'],
    maintainers: [{ name: 'Bitnami' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-12T10:00:00Z',
  },
  {
    id: 'helm:cert-manager',
    name: 'cert-manager',
    displayName: 'cert-manager',
    latestVersion: '1.13.3',
    appVersion: '1.13.3',
    versionsCount: 98,
    description: 'A Kubernetes add-on to automate the management of TLS certificates',
    icon: 'https://cert-manager.io/images/cert-manager-logo-icon.svg',
    home: 'https://cert-manager.io/',
    keywords: ['cert-manager', 'tls', 'certificates', 'letsencrypt'],
    maintainers: [{ name: 'cert-manager maintainers' }],
    repositoriesCount: 2,
    lastUpdated: '2024-01-11T09:00:00Z',
  },
];

/**
 * Mock Helm detail data.
 */
export const mockHelmDetail: HelmDetail = {
  id: 'helm:nginx-ingress',
  name: 'nginx-ingress',
  displayName: 'nginx-ingress',
  description: 'NGINX Ingress Controller for Kubernetes. This chart bootstraps an NGINX Ingress Controller deployment on a Kubernetes cluster using the Helm package manager.',
  icon: 'https://upload.wikimedia.org/wikipedia/commons/c/c5/Nginx_logo.svg',
  home: 'https://github.com/kubernetes/ingress-nginx',
  sources: ['https://github.com/kubernetes/ingress-nginx'],
  maintainers: [
    { name: 'Kubernetes', email: 'kubernetes@nginx.com', url: 'https://kubernetes.io' },
  ],
  keywords: ['nginx', 'ingress', 'controller', 'kubernetes', 'networking'],
  versions: [
    { version: '4.9.0', appVersion: '3.4.0', created: '2024-01-20T10:30:00Z', repository: 'helm-hosted' },
    { version: '4.8.3', appVersion: '3.3.0', created: '2024-01-10T14:00:00Z', repository: 'helm-hosted' },
    { version: '4.8.2', appVersion: '3.3.0', created: '2023-12-15T09:00:00Z', repository: 'helm-hosted' },
    { version: '4.7.1', appVersion: '3.2.0', created: '2023-11-20T08:00:00Z', repository: 'helm-proxy' },
    { version: '4.6.0', appVersion: '3.1.0', created: '2023-10-05T12:00:00Z', repository: 'helm-proxy' },
  ],
  repositories: ['helm-hosted', 'helm-proxy', 'helm-group'],
};

/**
 * Simulates Helm search API call with mock data.
 */
export async function mockHelmSearchApi(filters: HelmSearchFilters): Promise<HelmSearchResponse> {
  await new Promise((resolve) => setTimeout(resolve, 300));

  let filtered = [...mockHelmResults];

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

  // Filter by appVersion (exact match)
  if (filters.appVersion) {
    filtered = filtered.filter((r) => r.appVersion === filters.appVersion);
  }

  // Filter by description
  if (filters.description) {
    const d = filters.description.toLowerCase();
    filtered = filtered.filter((r) =>
      r.description?.toLowerCase().includes(d)
    );
  }

  return {
    items: filtered,
    totalCount: filtered.length,
    continuationToken: undefined,
  };
}

/**
 * Simulates Helm detail API call.
 */
export async function mockHelmDetailApi(id: string): Promise<HelmDetail> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // Return mock detail (in real impl, would look up by id)
  return mockHelmDetail;
}


