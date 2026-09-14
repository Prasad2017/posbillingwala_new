const fs = require("fs");
const path = require("path");

const flutterRoot = path.resolve(__dirname, "..");
const repoRoot = path.resolve(flutterRoot, "..");
const out = path.join(flutterRoot, "lib", "l10n", "ui_catalog.dart");

const xml = {
  en: path.join(repoRoot, "WithTable/app/src/main/res/values/strings_ui.xml"),
  hi: path.join(repoRoot, "WithTable/app/src/main/res/values-hi/strings_ui.xml"),
  mr: path.join(repoRoot, "WithTable/app/src/main/res/values-mr/strings_ui.xml"),
};
const extra = {
  en: path.join(repoRoot, "WithTable/app/src/main/res/values/strings.xml"),
  hi: path.join(repoRoot, "WithTable/app/src/main/res/values-hi/strings.xml"),
  mr: path.join(repoRoot, "WithTable/app/src/main/res/values-mr/strings.xml"),
};
const extraKeys = new Set([
  "discount_wise_report",
  "refund_wise_report",
  "cancel",
  "retry",
]);

const stringRe = /<string\s+name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g;

function unescapeXml(raw) {
  return raw
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\\'/g, "'")
    .replace(/\\"/g, '"')
    .replace(/\\n/g, "\n")
    .replace(/\\t/g, "\t")
    .trim();
}

function parse(file, extraOnly) {
  const text = fs.readFileSync(file, "utf8");
  const outMap = {};
  stringRe.lastIndex = 0;
  let m;
  while ((m = stringRe.exec(text))) {
    const name = m[1];
    if (extraOnly && !extraKeys.has(name)) continue;
    const value = unescapeXml(m[2]);
    if (!value || value.startsWith("@")) continue;
    outMap[name] = value;
  }
  return outMap;
}

function dartStr(s) {
  return `"${s
    .replace(/\\/g, "\\\\")
    .replace(/\$/g, "\\$")
    .replace(/"/g, '\\"')
    .replace(/\r/g, "")
    .replace(/\n/g, "\\n")}"`;
}

function emitMap(name, data) {
  const keys = Object.keys(data).sort();
  const lines = [`const ${name} = <String, String>{`];
  for (const key of keys) {
    lines.push(`  ${dartStr(key)}: ${dartStr(data[key])},`);
  }
  lines.push("};");
  return lines.join("\n");
}

const maps = {};
for (const [lang, file] of Object.entries(xml)) {
  maps[lang] = parse(file, false);
  Object.assign(maps[lang], parse(extra[lang], true));
}

const content = `// GENERATED from WithTable strings_ui.xml — do not edit by hand.
// node tool/gen_ui_catalog.js
class UiCatalog {
  UiCatalog._();

  static String get(String lang, String key) {
    final map = switch (lang) {
      'hi' => _hi,
      'mr' => _mr,
      _ => _en,
    };
    return map[key] ?? _en[key] ?? key;
  }
}

${emitMap("_en", maps.en)}

${emitMap("_hi", maps.hi)}

${emitMap("_mr", maps.mr)}
`;

fs.writeFileSync(out, content, "utf8");
console.log(
  `Wrote ${out} en=${Object.keys(maps.en).length} hi=${Object.keys(maps.hi).length} mr=${Object.keys(maps.mr).length}`,
);
