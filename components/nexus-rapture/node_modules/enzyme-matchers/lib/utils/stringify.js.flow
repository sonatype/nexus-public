// @flow

import CircularJSON from 'circular-json-es6';
import colors from './colors';

function stringifySingle(key: string, value: any): Array<string> {
  let stringifyingValue = value;
  let skipCircularCheck = false;
  if (Array.isArray(value)) {
    const values = value.map(v => stringifySingle('', v)[1]);

    // Skip circular check because we have already safely dealt with it above
    skipCircularCheck = true;

    let joined = values.join(' ');
    let initialBracket = colors.gray('[');
    const endingBracket = colors.gray(']');

    if (joined.length > 20) {
      const pad = '\n  ';
      joined = `${values.join(pad)}\n`;
      initialBracket += pad;
    }

    stringifyingValue = colors.gray(
      `${initialBracket}${joined}${endingBracket}`
    );
  } else if (value === null) {
    stringifyingValue = colors.gray(value);
  } else if (typeof value === 'object') {
    stringifyingValue = colors.gray(CircularJSON.stringify(value));
  } else if (typeof value === 'string') {
    stringifyingValue = colors.gray(`"${value}"`);
  } else if (typeof value === 'number') {
    stringifyingValue = colors.blue(value);
  } else if (value) {
    stringifyingValue = colors.green(value);
  } else if (!value) {
    stringifyingValue = colors.red(value);
  }

  try {
    // circular if you cant stringify
    if (!skipCircularCheck) {
      JSON.stringify({ [key]: value });
    }

    return [key, stringifyingValue];
  } catch (e) {
    return [key, colors.gray('[Circular]')];
  }
}

function color([key, value]): string {
  return `${colors.yellow(key)}${colors.gray(':')} ${colors.yellow(value)}`;
}

export default function stringify(object: Object): string {
  const keys = Object.keys(object);

  return keys.map(key => color(stringifySingle(key, object[key]))).join('\n');
}
