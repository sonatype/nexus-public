const chalk = require("chalk");

// Only show emoji on OSx (Windows shell doesn't like them that much ¯\_(ツ)_/¯ )
function emoji(emoji) {
	if (process.stdout.isTTY && process.platform === "darwin") {
		return emoji;
	} else {
		return "";
	}
}

function print(str = "", color = "dim") {
	const terminalCols = 80;
	// eslint-disable-next-line no-control-regex
	const ansiEscapeSeq = /\u001b\[[0-9]{1,2}m/g;
	const strLength = str.replace(ansiEscapeSeq, "").length;
	const leftPaddingLength = Math.floor((terminalCols - strLength) / 2);
	const leftPadding = " ".repeat(leftPaddingLength);
	str = chalk[color](str);
	console.log(leftPadding, str);
}

function printBadge() {
	console.log("\n");
	print(`${chalk.bold("Thanks for using")} ${chalk.bold.blue("webpack!")}`);
	print(`Please consider donating to our ${chalk.bold.blue("Open Collective")}`);
	print("to help us maintain this package.");
	console.log("\n\n");
	print(
		`${emoji("👉")} ${chalk.bold.yellow(" Donate:")} ${chalk.reset.underline.yellow(
			"https://opencollective.com/webpack/donate"
		)}`
	);
	console.log("\n");
}

function isTrue(value) {
	return !!value && value !== "0" && value !== "false";
}
const envDisable = isTrue(process.env.DISABLE_OPENCOLLECTIVE) || isTrue(process.env.CI);

if (!envDisable) printBadge();
