const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', 'lib', 'features');
const IMPORT_BP =
  "import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';";
const IMPORT_RL =
  "import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';";

const targets = [
  'masters/presentation/table_master_page.dart',
  'mess/presentation/mess_payments_page.dart',
  'mess/presentation/mess_meal_sessions_page.dart',
];

function findMatchingParen(s, openIdx) {
  let depth = 0;
  let i = openIdx;
  let inStr = null;
  while (i < s.length) {
    const ch = s[i];
    if (inStr) {
      if (ch === '\\') {
        i += 2;
        continue;
      }
      if (ch === inStr) inStr = null;
      i += 1;
      continue;
    }
    if (ch === "'" || ch === '"') {
      inStr = ch;
      i += 1;
      continue;
    }
    if (ch === '(') depth += 1;
    else if (ch === ')') {
      depth -= 1;
      if (depth === 0) return i;
    }
    i += 1;
  }
  return -1;
}

function ensureImports(text) {
  if (text.includes(IMPORT_RL) && text.includes(IMPORT_BP)) return text;
  const lines = text.split(/(?<=\n)/);
  let lastImport = -1;
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith('import ')) lastImport = i;
  }
  const inserts = [];
  if (!text.includes(IMPORT_BP)) inserts.push(IMPORT_BP + '\n');
  if (!text.includes(IMPORT_RL)) inserts.push(IMPORT_RL + '\n');
  if (lastImport >= 0 && inserts.length) {
    lines.splice(lastImport + 1, 0, ...inserts);
    return lines.join('');
  }
  return text;
}

function updatePadding(inner) {
  return inner.replace(
    /padding:\s*const\s+EdgeInsets\.fromLTRB\(\s*16\s*,\s*([^,]+)\s*,\s*16\s*,\s*([^)]+)\)/,
    'padding: EdgeInsets.fromLTRB(\n' +
      '            AppBreakpoints.pagePaddingFor(context.widthClass),\n' +
      '            $1,\n' +
      '            AppBreakpoints.pagePaddingFor(context.widthClass),\n' +
      '            $2)',
  );
}

function wrapAllSeparated(text) {
  let result = text;
  let guard = 0;
  while (guard++ < 20) {
    const re = /ListView\.separated\s*\(/g;
    let m;
    let found = null;
    while ((m = re.exec(result))) {
      // skip if already wrapped recently
      const before = result.slice(Math.max(0, m.index - 80), m.index);
      if (before.includes('ResponsiveScrollShell')) continue;
      found = m;
      break;
    }
    if (!found) break;
    const openParen = result.indexOf('(', found.index + found[0].length - 1);
    const closeParen = findMatchingParen(result, openParen);
    if (closeParen < 0) break;
    const lvStart = found.index;
    const inner = updatePadding(result.slice(lvStart, closeParen + 1));
    const wrapped =
      'ResponsiveScrollShell(\n' +
      '        dashboard: true,\n' +
      `        child: ${inner},\n` +
      '      )';
    result = result.slice(0, lvStart) + wrapped + result.slice(closeParen + 1);
  }
  return result;
}

for (const rel of targets) {
  const filePath = path.join(ROOT, rel);
  let text = fs.readFileSync(filePath, 'utf8');
  if (text.includes('ResponsiveScrollShell')) {
    console.log('already', rel);
    continue;
  }
  const next = wrapAllSeparated(text);
  if (next === text) {
    console.log('nopattern', rel);
    continue;
  }
  fs.writeFileSync(filePath, ensureImports(next), 'utf8');
  console.log('changed', rel);
}
