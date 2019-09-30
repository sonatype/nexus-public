'use strict';

const path = require('path');

module.exports = {
  ENVIRONMENT_CONFIG_MAP: {
    JEST_SUITE_NAME: 'suiteName',
    JEST_JUNIT_OUTPUT: 'output',
    JEST_JUNIT_OUTPUT_DIR: 'outputDirectory',
    JEST_JUNIT_OUTPUT_NAME: 'outputName',
    JEST_JUNIT_CLASSNAME: 'classNameTemplate',
    JEST_JUNIT_SUITE_NAME: 'suiteNameTemplate',
    JEST_JUNIT_TITLE: 'titleTemplate',
    JEST_JUNIT_ANCESTOR_SEPARATOR: 'ancestorSeparator',
    JEST_JUNIT_ADD_FILE_ATTRIBUTE: 'addFileAttribute',
    JEST_JUNIT_INCLUDE_CONSOLE_OUTPUT: 'includeConsoleOutput',
    JEST_USE_PATH_FOR_SUITE_NAME: 'usePathForSuiteName',
  },
  DEFAULT_OPTIONS: {
    suiteName: 'jest tests',
    output: path.join(process.cwd(), './junit.xml'),
    outputDirectory: null,
    outputName: 'junit.xml',
    classNameTemplate: '{classname} {title}',
    suiteNameTemplate: '{title}',
    titleTemplate: '{classname} {title}',
    ancestorSeparator: ' ',
    usePathForSuiteName: 'false',
    addFileAttribute: 'false',
    includeConsoleOutput: 'false',
  },
  CLASSNAME_VAR: 'classname',
  FILENAME_VAR: 'filename',
  FILEPATH_VAR: 'filepath',
  TITLE_VAR: 'title',
  DISPLAY_NAME_VAR: 'displayName',
};
