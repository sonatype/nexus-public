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
import { faSearch } from '@fortawesome/free-solid-svg-icons';

export default {
  SEARCH: {
    KEYWORD: {
      MENU: {
        text: 'Search',
        description: 'Search for components by attributes',
        icon: faSearch
      }
    },
    CUSTOM: {
      MENU: {
        text: 'Custom',
        description: 'Search for components by custom attributes',
        icon: faSearch
      }
    },
    APT: {
      MENU: {
        text: 'Apt',
        description: 'Search for components in Apt repositories',
        icon: faSearch
      }
    },
    CARGO: {
      MENU: {
        text: 'Cargo',
        description: 'Search for components by Cargo attributes',
        icon: faSearch
      }
    },
    COCOAPODS: {
      MENU: {
        text: 'Cocoapods',
        description: 'Search for components in Cocoapods repositories',
        icon: faSearch
      }
    },
    COMPOSER: {
      MENU: {
        text: 'Composer',
        description: 'Search for components in Composer repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Composer Repositories',
        FIELD_LABEL: {
          VERSION: 'Version',
          PACKAGE: 'Package',
          VENDOR: 'Vendor',
        }
      }
    },
    CONAN: {
      MENU: {
        text: 'Conan',
        description: 'Search for components by Conan attributes',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Conan Repositories',
        FIELD_LABEL: {
          BASE_VERSION: 'Base Version',
          CHANNEL: 'Channel',
          RECIPE_REVISION: 'Recipe Revision',
          PACKAGE_ID: 'Package Id',
          PACKAGE_REVISION: 'Package Revision',
          BASE_VERSION_STRICT: 'Base Version Strict',
          RECIPE_REVISION_LATEST: 'Latest revision',
          ARCH: 'Arch',
          OS: 'Os',
          COMPILER: 'Compiler',
          COMPILER_VERSION: 'Compiler Version',
          COMPILER_RUNTIME: 'Compiler Runtime',
        }
      }
    },
    CONDA: {
      MENU: {
        text: 'Conda',
        description: 'Search for components in Conda repositories',
        icon: faSearch
      }
    },
    DOCKER: {
      MENU: {
        text: 'Docker',
        description: 'Search for components in Docker repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Docker Repositories',
        FIELD_LABEL: {
          IMAGE_NAME: 'Image Name',
          IMAGE_TAG: 'Image Tag',
          LAYER_ID: 'Layer Id',
          CONTENT_DIGEST: 'Content Digest',
        }
      }
    },
    GITLFS: {
      MENU: {
        text: 'Git LFS',
        description: 'Search for components in Git LFS repositories',
        icon: faSearch
      }
    },
    GOLANG: {
      MENU: {
        text: 'Go',
        description: 'Search for components in Go repositories',
        icon: faSearch
      }
    },
    HELM: {
      MENU: {
        text: 'Helm',
        description: 'Search for components in Helm repositories',
        icon: faSearch
      }
    },
    HUGGING_FACE: {
      MENU: {
        text: 'HuggingFace',
        description: 'Search for components in HuggingFace repositories',
        icon: faSearch
      }
    },
    MAVEN: {
      MENU: {
        text: 'Maven',
        description: 'Search for components in Conan repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Maven Repositories',
        FIELD_LABEL: {
          GROUP_ID: 'Group Id',
          ARTIFACT_ID: 'Artifact Id',
          BASE_VERSION: 'Base Version',
          CLASSIFIER: 'Classifier',
          EXTENSION: 'Extension',
        }
      }
    },
    NPM: {
      MENU: {
        text: 'npm',
        description: 'Search for components in npm repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Npm Repositories',
        FIELD_LABEL: {
          SCOPE: 'Scope',
          AUTHOR: 'Author',
          DESCRIPTION: 'Description',
          KEYWORDS: 'Keywords',
          LICENSE: 'License',
        }
      }
    },
    NUGET: {
      MENU: {
        text: 'NuGet',
        description: 'Search for components in NuGet repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'NuGet Repositories',
        FIELD_LABEL: {
          ID: 'ID',
          TAGS: 'Tags',
        }
      }
    },
    P2: {
      MENU: {
        text: 'P2',
        description: 'Search for components in P2 repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'P2 Repositories',
        FIELD_LABEL: {
          PLUGIN_NAME: 'Plugin name',
        }
      }
    },
    PYPI: {
      MENU: {
        text: 'PyPI',
        description: 'Search for components in PyPI repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'PyPI Repositories',
        FIELD_LABEL: {
          CLASSIFIERS: 'Classifiers',
          DESCRIPTION: 'Description',
          KEYWORDS: 'PyPI Keywords',
          SUMMARY: 'Summary',
        }
      }
    },
    PUB: {
      MENU: {
        text: 'Pub',
        description: 'Search for components in Pub repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Pub Repositories',
        FIELD_LABEL: {
          NAME: 'Name',
          VERSION: 'Version',
        }
      }
    },
    R: {
      MENU: {
        text: 'R',
        description: 'Search for components in R repositories',
        icon: faSearch
      }
    },
    RAW: {
      MENU: {
        text: 'Raw',
        description: 'Search for components in Raw repositories',
        icon: faSearch
      }
    },
    RUBYGEMS: {
      MENU: {
        text: 'RubyGems',
        description: 'Search for components in RubyGems repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'RubyGems Repositories',
        FIELD_LABEL: {
          PLATFORM: 'Platform',
          SUMMARY: 'Summary',
          DESCRIPTION: 'Description',
        }
      }
    },
    TERRAFORM: {
      MENU: {
        text: 'Terraform',
        description: 'Search for components by Terraform attributes',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Terraform Repositories',
        FIELD_LABEL: {
          PROVIDER: 'Provider/Type',
          NAMESPACE: 'Namespace',
          NAME: 'Name',
        }
      }
    },
    YUM: {
      MENU: {
        text: 'Yum',
        description: 'Search for components in Yum repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Yum Repositories',
        FIELD_LABEL: {
          ARCHITECTURE: 'Architecture',
          NAME: 'Name',
        }
      }
    },
    SWIFT: {
      MENU: {
        text: 'Swift',
        description: 'Search for components in Swift repositories',
        icon: faSearch
      },
      CRITERIA: {
        GROUP: 'Swift Repositories',
        FIELD_LABEL: {
          NAME: 'Name',
          VERSION: 'Version',
          SCOPE: 'Scope',
        }
      }
    },
  }
};
