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
import { Permissions } from '@sonatype/nexus-ui-plugin';
import { UIView } from '@uirouter/react';
import { ROUTE_NAMES } from '../routeNames/routeNames';
import { lazyLoad } from './lazyLoad';
import FeatureFlags from '../../constants/FeatureFlags';

// Lazy load all route components for better code splitting
const Welcome = lazyLoad(() => import('../../components/pages/user/Welcome/Welcome'));
const BrowseReactExt = lazyLoad(() => import('../../components/pages/browse/Browse/BrowseExt'));
const MalwareRemediation = lazyLoad(() => import('../../components/pages/maliciousrisk/MalwareRemediation'));
const SearchGenericExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchGenericExt'));
const SearchAlpineExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchAlpineExt'));
const SearchAnsiblegalaxyExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchAnsiblegalaxyExt'));
const SearchAptExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchAptExt'));
const SearchCargoExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchCargoExt'));
const SearchCocoapodsExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchCocoapodsExt'));
const SearchComposerExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchComposerExt'));
const SearchConanExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchConanExt'));
const SearchCondaExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchCondaExt'));
const SearchCustomExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchCustomExt'));
const SearchDockerExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchDockerExt'));
const SearchGitLfsExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchGitLfsExt'));
const SearchGolangExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchGolangExt'));
const SearchHelmExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchHelmExt'));
const SearchHuggingFaceExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchHuggingFaceExt'));
const SearchMavenExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchMavenExt'));
const SearchNpmExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchNpmExt'));
const SearchNugetExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchNugetExt'));
const SearchP2Ext = lazyLoad(() => import('../../components/pages/browse/Search/SearchP2Ext'));
const SearchPubExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchPubExt'));
const SearchPypiExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchPypiExt'));
const SearchRExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchRExt'));
const SearchRawExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchRawExt'));
const SearchRubygemsExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchRubygemsExt'));
const SearchTerraformExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchTerraformExt'));
const SearchSwiftExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchSwiftExt'));
const SearchYumExt = lazyLoad(() => import('../../components/pages/browse/Search/SearchYumExt'));
const Tags = lazyLoad(() => import('../../components/pages/browse/Tags/Tags'));
const UploadList = lazyLoad(() => import('../../components/pages/browse/Upload/UploadList'));
const UploadDetails = lazyLoad(() => import('../../components/pages/browse/Upload/UploadDetails'));
const TestForm = lazyLoad(() => import('../../components/pages/TestForm'));

const { MALWARE_RISK_ENABLED } = FeatureFlags;

const BROWSE = ROUTE_NAMES.BROWSE;

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const browseRoutes = [
  {
    name: BROWSE.DIRECTORY,
    url: 'browse',
    component: UIView,
    data: {
      visibilityRequirements: {
        ignoreForMenuVisibilityCheck: true,
      },
      title: BROWSE.BROWSE.TITLE,
    },
  },
  {
    name: BROWSE.WELCOME.ROOT,
    url: '/welcome',
    component: Welcome,
    data: {
      // make sure we don't inherit from BROWSE
      visibilityRequirements: {
        anonymousAccessOrHasUser: true,
      },
      title: BROWSE.WELCOME.TITLE,
    },
  },
  {
    name: BROWSE.SEARCH.ROOT,
    url: '/search',
    component: UIView,
    abstract: true,
    data: {
      // make sure we don't inherit from BROWSE
      visibilityRequirements: {},
      title: BROWSE.SEARCH.TITLE,
    },
  },
  {
    name: BROWSE.SEARCH.GENERIC,
    url: '/generic:keyword',
    component: SearchGenericExt,
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
      },
      title: BROWSE.SEARCH.TITLE,
    },
  },
  {
    name: BROWSE.SEARCH.CUSTOM,
    url: '/custom/:keyword',
    component: SearchCustomExt,
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
      },
      title: BROWSE.SEARCH.TITLE,
    },
  },
  {
    name: BROWSE.SEARCH.ALPINE,
    url: '/alpine/:keyword',
    component: SearchAlpineExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'alpine',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.APT,
    url: '/apt/:keyword',
    component: SearchAptExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'apt',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.ANSIBLEGALAXY,
    url: '/ansiblegalaxy/:keyword',
    component: SearchAnsiblegalaxyExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'ansiblegalaxy',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CARGO,
    url: '/cargo/:keyword',
    component: SearchCargoExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'cargo',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.COCOAPODS,
    url: '/cocoapods/:keyword',
    component: SearchCocoapodsExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'cocoapods',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.COMPOSER,
    url: '/composer/:keyword',
    component: SearchComposerExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'composer',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CONAN,
    url: '/conan/:keyword',
    component: SearchConanExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'conan',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.CONDA,
    url: '/conda/:keyword',
    component: SearchCondaExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'conda',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.DOCKER,
    url: '/docker/:keyword',
    component: SearchDockerExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'docker',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.GITLFS,
    url: '/gitlfs/:keyword',
    component: SearchGitLfsExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'gitlfs',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.GOLANG,
    url: '/golang/:keyword',
    component: SearchGolangExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'go',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.HELM,
    url: '/helm/:keyword',
    component: SearchHelmExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'helm',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.HUGGING_FACE,
    url: '/hugging_face/:keyword',
    component: SearchHuggingFaceExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'huggingface',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.MAVEN,
    url: '/maven/:keyword',
    component: SearchMavenExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'maven2',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.NPM,
    url: '/npm/:keyword',
    component: SearchNpmExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'npm',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.NUGET,
    url: '/nuget/:keyword',
    component: SearchNugetExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'nuget',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.P2,
    url: '/p2/:keyword',
    component: SearchP2Ext,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'p2',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.PYPI,
    url: '/pypi/:keyword',
    component: SearchPypiExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'pypi',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.PUB,
    url: '/pub/:keyword',
    component: SearchPubExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'pub',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.R,
    url: '/r/:keyword',
    component: SearchRExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'r',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.RAW,
    url: '/raw/:keyword',
    component: SearchRawExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'raw',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.RUBYGEMS,
    url: '/rubygems/:keyword',
    component: SearchRubygemsExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'rubygems',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.TERRAFORM,
    url: '/terraform/:keyword',
    component: SearchTerraformExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'terraform',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.YUM,
    url: '/yum/:keyword',
    component: SearchYumExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'yum',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.BROWSE.ROOT,
    url: '/browse:repo',
    component: BrowseReactExt,
    data: {
      visibilityRequirements: {
        anonymousAccessOrHasUser: true,
        // Allow Browse visibility for users with either repository-view OR repository-content-selector permissions
        // This ensures Content Selectors make Browse visible in UI (fixes NEXUS-50552)
        permissionPrefixes: ['nexus:repository-view', 'nexus:repository-content-selector'],
        statesEnabled: [
          {
            key: 'browseableformats',
            defaultValue: [],
          },
        ],
      },
      title: BROWSE.BROWSE.TITLE,
    },
    params: {
      repo: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.UPLOAD.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.COMPONENT.CREATE],
      },
      title: BROWSE.UPLOAD.TITLE,
    },
  },
  {
    name: BROWSE.UPLOAD.LIST,
    url: '/upload',
    component: UploadList,
  },
  {
    name: BROWSE.UPLOAD.EDIT,
    url: '/upload/:itemId',
    component: UploadDetails,
  },
  {
    name: BROWSE.TAGS.ROOT,
    url: '/tags:itemId',
    component: Tags,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.TAGS.READ],
        editions: ['PRO'],
      },
      title: BROWSE.TAGS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.SEARCH.SWIFT,
    url: '/swift/:keyword',
    component: SearchSwiftExt,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:search:read'],
        browseableFormat: 'swift',
      },
      title: BROWSE.SEARCH.TITLE,
    },
    params: {
      keyword: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },
  {
    name: BROWSE.MALWARERISK.ROOT,
    url: '/malwarerisk',
    component: MalwareRemediation,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
        // RHC available to ALL editions (CE, Pro, Enterprise, Nexus One)
        // Protect is marketing/education module - always visible to admins
      },
      title: BROWSE.MALWARERISK.TITLE,
    },
  },

  // Test route for DynamicFormField
  {
    name: 'browse.testform',
    url: '/testform',
    component: TestForm,
    data: {
      visibilityRequirements: {},
      title: 'Dynamic Form Field Test',
    },
  },
];
