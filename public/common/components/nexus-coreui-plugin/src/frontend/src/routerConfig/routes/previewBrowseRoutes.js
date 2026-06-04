/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Preview Browse Routes - SUPER UX Migration
 * 
 * Implemented formats show their Radix-based search pages.
 * Unimplemented formats show the ComingSoonPage placeholder.
 */

import {UIView} from '@uirouter/react';
import {
  Permissions,
  // Preview UI page components (pre-lazied in nexus-ui-plugin)
  HealthReportPage,
  FirewallReportPage,
  WelcomeSuper,
  ProtectHub,
  MalwareRemediationWizard,
  MaliciousPackagesPage,
  MalwareRiskPageSuper,
  BrowsePage,
  RepositoryProfilePage,
  TagsPage,
  TagDetailPage,
  UploadPage,
  UploadFormPage,
  ApiPage,
  RemediatePage,
  GASearchPage,
  GADetailPage,
  GenericSearchPage,
  CustomSearchPage,
  NpmSearchPage,
  NpmDetailPage,
  NuGetSearchPage,
  NuGetDetailPage,
  DockerSearchPage,
  DockerDetailPage,
  PyPISearchPage,
  PyPIDetailPage,
  HelmSearchPage,
  HelmDetailPage,
  GolangSearchPage,
  GolangDetailPage,
  RawSearchPage,
  RawDetailPage,
  YumSearchPage,
  YumDetailPage,
  AptSearchPage,
  AptDetailPage,
  RubyGemsSearchPage,
  RubyGemsDetailPage,
  UnifiedSearchPage,
  AssetDetailPage,
  ComingSoonPage,
} from '@sonatype/nexus-ui-plugin';
import {lazyLoad} from './lazyLoad';
import FeatureFlags from '../../constants/FeatureFlags';

// Audit Log Page lives in coreui (not relocated to preview subtree).
const AuditLogPage = lazyLoad(() => import('../../components/pages/admin/audit/AuditLogPage'));

export const previewBrowseRoutes = [
  // =============================================================================
  // ROOT ROUTES
  // =============================================================================
  {
    name: 'preview',
    url: 'preview',
    abstract: true,
    component: UIView,
  },
  {
    name: 'preview.browse',
    url: '/browse', // Nested under preview: #/preview/browse
    abstract: true,
    component: UIView,
    data: {title: 'Browse'},
  },

  // =============================================================================
  // DASHBOARD
  // =============================================================================
  {
    name: 'preview.browse.welcome',
    url: '/welcome?tab', // Nested: #/preview/browse/welcome
    component: WelcomeSuper,
    params: {
      tab: { type: 'string', dynamic: true, value: 'overview' },
    },
    data: {
      title: 'Welcome',
      visibilityRequirements: {
        anonymousAccessOrHasUser: true,
      },
    },
  },

  // =============================================================================
  // PROTECT — Malware Risk Hub (Overview, Detect, Remediate, Harden)
  // Wizard: sibling route so /protect?tab can render without blocking /protect/wizard/...
  // =============================================================================
  {
    name: 'preview.browse.protect',
    url: '/protect?tab',
    component: MaliciousPackagesPage,
    params: {
      tab: {value: 'overview', type: 'string', dynamic: true},
    },
    data: {
      title: 'Protect',
      visibilityRequirements: {
        requiresUser: true,
      },
    },
  },
  {
    name: 'preview.browse.protectWizard',
    url: '/protect/wizard/:repositoryName?step',
    component: MalwareRemediationWizard,
    params: {
      repositoryName: {type: 'string', dynamic: true},
      step: {type: 'string', value: '1', dynamic: true},
    },
    data: {
      title: 'Malicious Packages Wizard',
      visibilityRequirements: {
        requiresUser: true,
      },
    },
  },

  // =============================================================================
  // API DOCUMENTATION - Swagger UI (promoted from Settings, no sidebar)
  // Requires authentication to view
  // =============================================================================
  {
    name: 'preview.browse.api',
    url: '/api',
    component: ApiPage,
    data: {
      title: 'API',
      visibilityRequirements: {
        requiresUser: true,
        permissions: ['nexus:settings:read'],
      },
    },
  },

  // =============================================================================
  // AUDIT LOG - IMPLEMENTED ✅
  // Requires authentication to view
  // =============================================================================
  {
    name: 'preview.browse.audit',
    url: '/audit?repositoryName',
    component: AuditLogPage,
    params: {
      repositoryName: { type: 'string', dynamic: true, value: null },
    },
    data: {
      title: 'Audit',
      visibilityRequirements: {
        requiresUser: true,
        permissions: ['nexus:audit:read'],
        statesEnabled: [
          {
            key: 'previewAuditEnabled',
            defaultValue: false,
          },
        ],
      },
    },
  },

  // =============================================================================
  // MALICIOUS PACKAGES - Backward-compat redirect to Malware Risk
  // =============================================================================
  {
    name: 'preview.browse.malicious-packages',
    url: '/malicious-packages?tab',
    redirectTo: 'preview.browse.malwarerisk',
    params: {
      tab: {value: 'overview', type: 'string', dynamic: true},
    },
    data: {
      title: 'Malware Risk',
      visibilityRequirements: {
        requiresUser: true,
      },
    },
  },

  // =============================================================================
  // MALWARE REMEDIATION (Legacy) - IMPLEMENTED ✅
  // Requires authentication to view
  // =============================================================================
  {
    name: 'preview.browse.remediate',
    url: '/remediate',
    component: RemediatePage,
    data: {
      title: 'Malicious Packages',
      visibilityRequirements: {
        requiresUser: true,
      },
    },
  },

  // =============================================================================
  // BROWSE - IMPLEMENTED ✅
  // =============================================================================
  {
    name: 'preview.browse.browse',
    url: '?format',
    component: BrowsePage,
    params: {
      format: { type: 'string', value: null, dynamic: true, inherit: true },
    },
    data: {title: 'Browse'},
  },
  {
    name: 'preview.browse.browse.repo',
    url: '/{repoName:[^/]+}/',
    component: BrowsePage,
    params: {
      repoName: { value: null, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
    },
    data: {title: 'Browse Repository'},
  },
  {
    name: 'preview.browse.browse.repo.path',
    url: '*path?tab',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { value: null, raw: true, dynamic: true },
      tab: { type: 'string', value: 'summary', dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'summary', dynamic: true }, // Default tab
    },
    data: {title: 'Browse'},
  },
  // Tab-based routing for asset detail views
  {
    name: 'preview.browse.browse.repo.path.summary',
    url: '/summary',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { type: 'string', value: null, raw: true, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'summary', dynamic: true },
    },
    data: {title: 'Summary'},
  },
  {
    name: 'preview.browse.browse.repo.path.usage',
    url: '/usage',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { type: 'string', value: null, raw: true, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'usage', dynamic: true },
    },
    data: {title: 'Usage'},
  },
  {
    name: 'preview.browse.browse.repo.path.attributes',
    url: '/attributes',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { type: 'string', value: null, raw: true, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'attributes', dynamic: true },
    },
    data: {title: 'Attributes'},
  },
  {
    name: 'preview.browse.browse.repo.path.tags',
    url: '/tags',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { type: 'string', value: null, raw: true, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'tags', dynamic: true },
    },
    data: {title: 'Component Tags'},
  },
  {
    name: 'preview.browse.browse.repo.path.lifecycle',
    url: '/lifecycle',
    component: BrowsePage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
      path: { type: 'string', value: null, raw: true, dynamic: true },
      format: { type: 'string', value: null, dynamic: true, inherit: true },
      tab: { type: 'string', value: 'lifecycle', dynamic: true },
    },
    data: {title: 'Sonatype Lifecycle'},
  },

  // =============================================================================
  // REPOSITORY PROFILE (Browse Context) - IMPLEMENTED ✅
  // Uses SAME component as Settings profile, but rendered without Settings sidebar
  // This is a sibling route so it replaces BrowsePage entirely
  // =============================================================================
  {
    name: 'preview.browse.repository-profile',
    url: '/repository-profile/:repositoryName?tab',
    component: RepositoryProfilePage,
    params: {
      repositoryName: { type: 'string', dynamic: true },
      tab: { value: null, type: 'string', dynamic: true },
    },
    resolve: [
      {
        token: 'repositoryName',
        deps: ['$stateParams'],
        resolveFn: ($stateParams) => $stateParams.repositoryName,
      },
      {
        token: 'context',
        resolveFn: () => 'browse', // Tells component to show "Back to Browse"
      },
    ],
    data: { title: 'Repository Profile' },
  },

  // =============================================================================
  // UPLOAD - IMPLEMENTED ✅
  // =============================================================================
  // Base upload route - abstract container
  {
    name: 'preview.browse.upload',
    url: '/upload',
    abstract: true,
    component: UIView,
    data: {title: 'Upload'},
  },
  // Upload list (repository selection)
  {
    name: 'preview.browse.upload.list',
    url: '',
    component: UploadPage,
    data: {title: 'Upload'},
  },
  // Upload form for a specific repository
  {
    name: 'preview.browse.upload.form',
    url: '/:repoName',
    component: UploadFormPage,
    params: {
      repoName: { type: 'string', value: null, dynamic: true },
    },
    data: {title: 'Upload to Repository'},
  },
  // Alias for compatibility with left nav which uses browse.upload.edit -> preview.browse.upload.edit
  {
    name: 'preview.browse.upload.edit',
    redirectTo: 'preview.browse.upload.form',
  },

  // =============================================================================
  // TAGS - IMPLEMENTED ✅
  // =============================================================================
  {
    name: 'preview.browse.tags',
    url: '/tags',
    component: TagsPage,
    data: {title: 'Tags'},
  },
  {
    name: 'preview.browse.tagdetail',
    url: '/tags/:tagName',
    component: TagDetailPage,
    params: {
      tagName: { type: 'string', value: null, dynamic: true },
    },
    data: {title: 'Tag Details'},
  },

  // =============================================================================
  // HEALTH CHECK + FIREWALL REPORT PAGES - Dedicated full-page reports
  // Replaces cramped SecurityReportModal; iframe fills viewport
  // =============================================================================
  {
    name: 'preview.browse.health-report',
    url: '/health-report/:repositoryName',
    component: HealthReportPage,
    params: {
      repositoryName: { type: 'string', dynamic: true },
    },
    data: { title: 'Health Check Report' },
  },
  {
    name: 'preview.browse.firewall-report',
    url: '/firewall-report/:repositoryName',
    component: FirewallReportPage,
    params: {
      repositoryName: { type: 'string', dynamic: true },
    },
    data: { title: 'Firewall Report' },
  },

  // =============================================================================
  // MALWARE RISK — Primary Nexus One UI page (1:1 with Default UI Malware Risk)
  // =============================================================================
  {
    name: 'preview.browse.malwarerisk',
    url: '/malwarerisk',
    component: MalwareRiskPageSuper,
    data: {
      title: 'Malware Risk',
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
    },
  },
  // Backward compat: old URL redirects to Malware Risk
  {
    name: 'preview.browse.malware-defense',
    url: '/malware-defense',
    redirectTo: 'preview.browse.malwarerisk',
    data: { title: 'Malware Risk' },
  },

  // =============================================================================
  // SEARCH PARENT
  // =============================================================================
  {
    name: 'preview.browse.search',
    url: '/search',
    abstract: true,
    component: UIView,
    data: {
      title: 'Search',
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
      },
    },
  },
  
  // =============================================================================
  // UNIFIED SEARCH PAGE - Main entry point for ALL formats
  // =============================================================================
  {
    name: 'preview.browse.search.unified',
    url: '?q',
    component: UnifiedSearchPage,
    params: {
      q: { value: null, dynamic: true },
    },
    data: {title: 'Search Components'},
  },

  // =============================================================================
  // IMPLEMENTED SEARCH FORMATS (Legacy - kept for backward compatibility)
  // =============================================================================

  // Generic Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.generic',
    url: '/generic:keyword',
    component: GenericSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Generic Search'},
  },

  // Asset Detail - Unified asset detail with Component Tags ✅
  {
    name: 'preview.browse.search.asset',
    url: '/asset/:repositoryName/:assetId',
    component: AssetDetailPage,
    params: {
      repositoryName: {type: 'string', raw: true},
      assetId: {type: 'string', raw: true},
      componentId: {type: 'string', value: null, raw: true, dynamic: true},
    },
    data: {title: 'Asset Details'},
  },

  // Custom Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.custom',
    url: '/custom',
    component: CustomSearchPage,
    data: {title: 'Custom Search'},
  },
  
  // Maven Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.maven',
    url: '/maven/:keyword',
    component: GASearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Maven Search'},
  },
  // Component Detail - child of preview.browse.search (NOT maven)
  // This makes it a SIBLING of maven/npm/etc search pages,
  // so it replaces the search view entirely instead of nesting inside GASearchPage
  {
    name: 'preview.browse.search.component',
    url: '/component/:gaId?version',
    component: GADetailPage,
    params: {
      gaId: { type: 'string', raw: true },
      version: { type: 'string', value: null, squash: true },
    },
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Component Details'},
  },
  {
    name: 'preview.browse.search.component.overview',
    url: '/overview',
    component: GADetailPage,
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Overview'},
  },
  {
    name: 'preview.browse.search.component.versions',
    url: '/versions',
    component: GADetailPage,
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Versions'},
  },
  {
    name: 'preview.browse.search.component.repos',
    url: '/repos',
    component: GADetailPage,
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Repositories'},
  },
  {
    name: 'preview.browse.search.component.files',
    url: '/files',
    component: GADetailPage,
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Files'},
  },
  {
    name: 'preview.browse.search.component.security',
    url: '/security',
    component: GADetailPage,
    resolve: [
      { token: 'gaId', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.gaId },
      { token: 'version', deps: ['$stateParams'], resolveFn: ($stateParams) => $stateParams.version },
    ],
    data: {title: 'Security'},
  },
  
  // npm Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.npm',
    url: '/npm/:keyword',
    component: NpmSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'npm Search'},
  },
  {
    name: 'preview.browse.search.npm.detail',
    url: '/:packageId',
    component: NpmDetailPage,
    params: {packageId: {type: 'string', raw: true}},
    data: {title: 'npm Package Details'},
  },

  // NuGet Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.nuget',
    url: '/nuget/:keyword',
    component: NuGetSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'NuGet Search'},
  },
  {
    name: 'preview.browse.search.nuget.detail',
    url: '/:packageId',
    component: NuGetDetailPage,
    params: {packageId: {type: 'string', raw: true}},
    data: {title: 'NuGet Package Details'},
  },

  // Docker Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.docker',
    url: '/docker/:keyword',
    component: DockerSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Docker Search'},
  },
  {
    name: 'preview.browse.search.docker.detail',
    url: '/:imageId',
    component: DockerDetailPage,
    params: {imageId: {type: 'string', raw: true}},
    data: {title: 'Docker Image Details'},
  },

  // PyPI Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.pypi',
    url: '/pypi/:keyword',
    component: PyPISearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'PyPI Search'},
  },
  {
    name: 'preview.browse.search.pypi.detail',
    url: '/:packageId',
    component: PyPIDetailPage,
    params: {packageId: {type: 'string', raw: true}},
    data: {title: 'PyPI Package Details'},
  },

  // Helm Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.helm',
    url: '/helm/:keyword',
    component: HelmSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Helm Search'},
  },
  {
    name: 'preview.browse.search.helm.detail',
    url: '/:chartId',
    component: HelmDetailPage,
    params: {chartId: {type: 'string', raw: true}},
    data: {title: 'Helm Chart Details'},
  },

  // Golang Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.golang',
    url: '/go/:keyword',
    component: GolangSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Go Search'},
  },
  {
    name: 'preview.browse.search.golang.detail',
    url: '/:moduleId',
    component: GolangDetailPage,
    params: {moduleId: {type: 'string', raw: true}},
    data: {title: 'Go Module Details'},
  },

  // Raw Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.raw',
    url: '/raw/:keyword',
    component: RawSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Raw Search'},
  },
  {
    name: 'preview.browse.search.raw.detail',
    url: '/:assetId',
    component: RawDetailPage,
    params: {assetId: {type: 'string', raw: true}},
    data: {title: 'Raw Asset Details'},
  },

  // Yum Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.yum',
    url: '/yum/:keyword',
    component: YumSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Yum Search'},
  },
  {
    name: 'preview.browse.search.yum.detail',
    url: '/:packageId',
    component: YumDetailPage,
    params: {packageId: {type: 'string', raw: true}},
    data: {title: 'Yum Package Details'},
  },

  // Apt Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.apt',
    url: '/apt/:keyword',
    component: AptSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Apt Search'},
  },
  {
    name: 'preview.browse.search.apt.detail',
    url: '/:packageId',
    component: AptDetailPage,
    params: {packageId: {type: 'string', raw: true}},
    data: {title: 'Apt Package Details'},
  },

  // RubyGems Search - IMPLEMENTED ✅
  {
    name: 'preview.browse.search.rubygems',
    url: '/rubygems/:keyword',
    component: RubyGemsSearchPage,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'RubyGems Search'},
  },
  {
    name: 'preview.browse.search.rubygems.detail',
    url: '/:gemId',
    component: RubyGemsDetailPage,
    params: {gemId: {type: 'string', raw: true}},
    data: {title: 'RubyGems Details'},
  },

  // =============================================================================
  // COMING SOON SEARCH FORMATS
  // =============================================================================

  // Alpine - COMING SOON
  {
    name: 'preview.browse.search.alpine',
    url: '/alpine/:keyword',
    component: () => <ComingSoonPage featureName="Alpine Search" description="Alpine APK package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Alpine Search'},
  },

  // Cargo - COMING SOON
  {
    name: 'preview.browse.search.cargo',
    url: '/cargo/:keyword',
    component: () => <ComingSoonPage featureName="Cargo Search" description="Cargo/Rust package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Cargo Search'},
  },

  // Cocoapods - COMING SOON
  {
    name: 'preview.browse.search.cocoapods',
    url: '/cocoapods/:keyword',
    component: () => <ComingSoonPage featureName="Cocoapods Search" description="Cocoapods package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Cocoapods Search'},
  },

  // Composer - COMING SOON
  {
    name: 'preview.browse.search.composer',
    url: '/composer/:keyword',
    component: () => <ComingSoonPage featureName="Composer Search" description="PHP Composer package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Composer Search'},
  },

  // Conan - COMING SOON
  {
    name: 'preview.browse.search.conan',
    url: '/conan/:keyword',
    component: () => <ComingSoonPage featureName="Conan Search" description="Conan C/C++ package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Conan Search'},
  },

  // Conda - COMING SOON
  {
    name: 'preview.browse.search.conda',
    url: '/conda/:keyword',
    component: () => <ComingSoonPage featureName="Conda Search" description="Conda package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Conda Search'},
  },

  // Git LFS - COMING SOON
  {
    name: 'preview.browse.search.gitlfs',
    url: '/gitlfs/:keyword',
    component: () => <ComingSoonPage featureName="Git LFS Search" description="Git LFS search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Git LFS Search'},
  },

  // HuggingFace - COMING SOON
  {
    name: 'preview.browse.search.hugging_face',
    url: '/huggingface/:keyword',
    component: () => <ComingSoonPage featureName="HuggingFace Search" description="HuggingFace model search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'HuggingFace Search'},
  },

  // P2 - COMING SOON
  {
    name: 'preview.browse.search.p2',
    url: '/p2/:keyword',
    component: () => <ComingSoonPage featureName="P2 Search" description="Eclipse P2 search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'P2 Search'},
  },

  // R - COMING SOON
  {
    name: 'preview.browse.search.r',
    url: '/r/:keyword',
    component: () => <ComingSoonPage featureName="R Search" description="R package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'R Search'},
  },

  // Terraform - COMING SOON
  {
    name: 'preview.browse.search.terraform',
    url: '/terraform/:keyword',
    component: () => <ComingSoonPage featureName="Terraform Search" description="Terraform module search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Terraform Search'},
  },

  // Swift - COMING SOON
  {
    name: 'preview.browse.search.swift',
    url: '/swift/:keyword',
    component: () => <ComingSoonPage featureName="Swift Search" description="Swift package search is being migrated to the new Radix UI design." />,
    params: {keyword: {value: null, raw: true, dynamic: true}},
    data: {title: 'Swift Search'},
  },
];
