import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const directory = path.dirname(fileURLToPath(import.meta.url));
const prototypePath = path.join(directory, "workbench-v1.0.html");
const html = fs.readFileSync(prototypePath, "utf8");

const scriptMatches = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
if (scriptMatches.length !== 1) {
  throw new Error(`Expected one inline script, found ${scriptMatches.length}`);
}
new Function(scriptMatches[0][1]);

const ids = [...html.matchAll(/\sid="([^"]+)"/g)].map((match) => match[1]);
const duplicateIds = ids.filter((id, index) => ids.indexOf(id) !== index);
if (duplicateIds.length > 0) {
  throw new Error(`Duplicate element IDs: ${[...new Set(duplicateIds)].join(", ")}`);
}

const requiredIds = [
  "login-view",
  "my-applications",
  "other-applications",
  "collaboration-applications",
  "submission-logs",
  "password-modal",
  "create-modal"
];
for (const id of requiredIds) {
  if (!ids.includes(id)) {
    throw new Error(`Missing required prototype region: ${id}`);
  }
}

const requiredText = ["chyong", "admin123", "暂无地址，请补填", "创建合作应用"];
for (const text of requiredText) {
  if (!html.includes(text)) {
    throw new Error(`Missing required prototype text: ${text}`);
  }
}

console.log(`Prototype validation passed: ${Buffer.byteLength(html)} bytes, ${ids.length} unique IDs`);
