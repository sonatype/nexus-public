// @flow

const colorValues = {
  blue: [34, 39],
  gray: [90, 39],
  green: [32, 39],
  red: [31, 39],
  yellow: [33, 39],
};

const colorFns = {};

const matchOperatorsRe = /[|\\{}()[\]^$+*?.]/g;

Object.keys(colorValues).forEach(color => {
  const colorValue = colorValues[color];
  colorFns[color] = (str: string) => {
    const open = `\u001b[${colorValue[0]}m`;
    const close = `\u001b[${colorValue[1]}m`;
    const regex = new RegExp(close.replace(matchOperatorsRe, '\\$&'), 'g');
    const regStr = `${str}`.replace(regex, open);
    return `${open}${regStr}${close}`;
  };
});

export default colorFns;
