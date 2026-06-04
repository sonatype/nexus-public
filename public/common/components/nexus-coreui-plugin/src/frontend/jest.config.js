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
// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  // Use 50% of available CPUs for test parallelism.
  // Previously hard-coded to 2 for CircleCI memory constraints, which no longer applies.
  maxWorkers: '50%',

  // All imported modules in your tests should be mocked automatically
  // automock: false,

  // Stop running tests after `n` failures
  // bail: 0,

  // Respect "browser" field in package.json when resolving modules
  // browser: false,

  // The directory where Jest should store its cached dependency information
  cacheDirectory: '<rootDir>/../../target/.jest-cache',

  // Automatically clear mock calls and instances between every test
  clearMocks: true,

  // Indicates whether the coverage information should be collected while executing the test
  // collectCoverage: false,

  // An array of glob patterns indicating a set of files for which coverage information should be collected
  collectCoverageFrom: [
    'src/**/*.{js,jsx,ts,tsx}',
    '!src/**/*.test.{js,jsx,ts,tsx}',
    '!src/**/*.spec.{js,jsx,ts,tsx}',
    '!src/**/__tests__/**',
    '!src/__jest__/**',
    '!src/**/__mocks__/**',
    '!src/interface/ExtJS.js'
  ],

  // The directory where Jest should output its coverage files
  coverageDirectory: '../../target/js-coverage',

  // An array of regexp pattern strings used to skip coverage collection
  // ExtJS files are excluded - they will be deleted during migration
  coveragePathIgnorePatterns: [
    '/node_modules/',
    'interface/ExtJS.js',
    '/ExtJS/',
    '\\.extjs\\.',
    'extjsUtils'
  ],

  // A list of reporter names that Jest uses when writing coverage reports
  coverageReporters: [
    "json",
    "text",
    "lcov",
    "clover"
  ],

  // Coverage is collected and reported but not enforced via thresholds.
  // Arbitrary percentage gates caused more build churn than they prevented
  // regressions; coverage reports remain available for review.

  // A path to a custom dependency extractor
  // dependencyExtractor: null,

  // Make calling deprecated APIs throw helpful error messages
  // errorOnDeprecated: false,

  // Force coverage collection from ignored files using an array of glob patterns
  // forceCoverageMatch: [],

  // A path to a module which exports an async function that is triggered once before all test suites
  // globalSetup: null,

  // A path to a module which exports an async function that is triggered once after all test suites
  // globalTeardown: null,

  // A set of global variables that need to be available in all test environments
  globals: {
    // Build-time define: always false in tests (Sonatype internal test pages excluded)
    __SONATYPE_INTERNAL__: false,
  },

  // An array of directory names to be searched recursively up from the requiring module's location
  // moduleDirectories: [
  //   "node_modules"
  // ],

  // An array of file extensions your modules use
  // TypeScript enabled for new modules (search, preview UI)
  moduleFileExtensions: [
    'ts',
    'tsx',
    'js',
    'jsx',
    'json'
  ],

  // A map from regular expressions to module names that allow to stub out resources with a single module
  moduleNameMapper: {
    '^@sonatype/nexus-ui-plugin$': '<rootDir>/../../../nexus-ui-plugin/src/frontend/src/index.js',
    // Redirect nexus-ui-plugin's broken Tasks exports (files missing, live in nexus-coreui-plugin) - see TEST-BASELINE.md
    '^(\\./)?components/admin/Tasks/TasksList$': '<rootDir>/src/components/pages/admin/Tasks/TasksList.jsx',
    '^(\\./)?components/admin/Tasks/TasksListMachine$': '<rootDir>/src/components/pages/admin/Tasks/TasksListMachine.js',
    '^(\\./)?components/admin/Tasks/TasksHelper$': '<rootDir>/src/components/pages/admin/Tasks/TasksHelper.js',
    '^@nosc$': '<rootDir>/src/nosc/index.ts',
    '^@nosc/(.*)$': '<rootDir>/src/nosc/$1',
    '^@/utils/api$': '<rootDir>/__jest__/mocks/utilsApiMock.js',
    '^@/(.*)$': '<rootDir>/src/$1',
    '\\.scss$': '<rootDir>/__jest__/styleMock.js',
    '\\.css$': '<rootDir>/__jest__/styleMock.js',
    '\\.(png|svg)$': '<rootDir>/__jest__/imgMock.js'
  },

  // An array of regexp pattern strings, matched against all module paths before considered 'visible' to the module loader
  // modulePathIgnorePatterns: [],

  // Activates notifications for test results
  // notify: false,

  // An enum that specifies notification mode. Requires { notify: true }
  // notifyMode: "failure-change",

  // A preset that is used as a base for Jest's configuration
  // preset: null,

  // Run tests from one or more projects
  // projects: null,

  // Use this configuration option to add custom reporters to Jest
  reporters: ['default', 'jest-junit'],

  // Automatically reset mock state between every test
  // resetMocks: false,

  // Reset the module registry before running each individual test
  // resetModules: false,

  // A path to a custom resolver
  // resolver: null,

  // Automatically restore mock state between every test
  // restoreMocks: false,

  // The root directory that Jest should scan for tests and modules within
  // rootDir: null,

  // A list of paths to directories that Jest should use to search for files in
  // roots: ["<rootDir>"],

  // Allows you to use a custom runner instead of Jest's default test runner
  // runner: "jest-runner",

  // The paths to modules that run some code to configure or set up the testing environment before each test
  // setupFiles: [],

  // A list of paths to modules that run some code to configure or set up the testing framework before each test
  // consoleSetup.js MUST run first to filter console output before other imports
  setupFilesAfterEnv: ['<rootDir>/__jest__/consoleSetup.js', '<rootDir>/__jest__/setup.js'],

  // A list of paths to snapshot serializer modules Jest should use for snapshot testing
  snapshotSerializers: [],

  // The test environment that will be used for testing
  testEnvironment: 'jsdom',

  // Options that will be passed to the testEnvironment
  // testEnvironmentOptions: {},

  // Adds a location field to test results
  // testLocationInResults: false,

  // The glob patterns Jest uses to detect test files
  testMatch: ['**/?(*.)+(spec|test).[tj]s?(x)'],

  // An array of regexp pattern strings that are matched against all test paths, matched tests are skipped
  // testPathIgnorePatterns: [
  //   "/node_modules/"
  // ],

  // The regexp pattern or array of patterns that Jest uses to detect test files
  // testRegex: [],

  // This option allows the use of a custom results processor
  // testResultsProcessor: null,

  // This option allows use of a custom test runner
  // testRunner: "jasmine2",

  // This option sets the URL for the jsdom environment. It is reflected in properties such as location.href
  // testURL: "http://localhost",

  // Setting this value to "fake" allows the use of fake timers for functions such as "setTimeout"
  // timers: "real",

  // A map from regular expressions to paths to transformers
  // SWC handles both TypeScript and JavaScript files
  // swcrc: false is required to ignore .swcrc (which has env.targets for rspack builds)
  // and avoid "env and jsc.target cannot be used together" error
  transform: {
    '\\.tsx?$': ['@swc/jest', {
      swcrc: false,
      jsc: {
        parser: {
          syntax: 'typescript',
          tsx: true,
          decorators: false,
          dynamicImport: true
        },
        transform: {
          react: {
            runtime: 'automatic'
          }
        }
      }
    }],
    '\\.jsx?$': ['@swc/jest', {
      swcrc: false,
      jsc: {
        parser: {
          syntax: 'ecmascript',
          jsx: true,
          decorators: false,
          dynamicImport: true
        },
        transform: {
          react: {
            runtime: 'automatic'
          }
        }
      }
    }]
  },

  // An array of regexp pattern strings that are matched against all source file paths, matched files will skip transformation
  transformIgnorePatterns: [
    '/node_modules/(?!@sonatype/react-shared-components|pretty-bytes|@react-hook|d3-.*|internmap|swagger-ui-react|@radix-ui)'
  ],

  // An array of regexp pattern strings that are matched against all modules before the module loader will automatically return a mock for them
  // unmockedModulePathPatterns: undefined,

  // Indicates whether each individual test should be reported during the run
  // verbose: null,

  // An array of regexp patterns that are matched against all source file paths before re-running tests in watch mode
  // watchPathIgnorePatterns: [],

  // Whether to use watchman for file crawling
  // watchman: true,

  // Timeout for all tests to help CI pass
  testTimeout: 10000
};
