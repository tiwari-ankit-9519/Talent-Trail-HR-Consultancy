import { readFileSync, readdirSync, statSync } from "node:fs";
import { join } from "node:path";

const messages = JSON.parse(readFileSync("messages/en.json", "utf8"));

function has(key) {
  let node = messages;
  for (const part of key.split(".")) {
    if (node === null || typeof node !== "object" || !(part in node)) return false;
    node = node[part];
  }
  return typeof node === "string";
}

function walk(dir, out) {
  for (const name of readdirSync(dir)) {
    if (name === "node_modules" || name === ".next" || name.startsWith(".")) continue;
    const path = join(dir, name);
    const info = statSync(path);
    if (info.isDirectory()) walk(path, out);
    else if (/\.tsx?$/.test(name)) out.push(path);
  }
  return out;
}

const missing = new Set();
for (const file of [...walk("app", []), ...walk("components", []), ...walk("lib", [])]) {
  const source = readFileSync(file, "utf8");
  for (const match of source.matchAll(/\bt\(\s*"([\w.]+)"/g)) {
    if (!has(match[1])) missing.add(`${file}: ${match[1]}`);
  }
}

for (const line of [...missing].sort()) console.log(line);
console.log(`missing ${missing.size}`);
process.exit(missing.size === 0 ? 0 : 1);
