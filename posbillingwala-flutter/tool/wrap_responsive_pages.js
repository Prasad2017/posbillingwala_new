const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..', 'lib', 'features');
const IMPORT_BP =
  "import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';";
const IMPORT_RL =
  "import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';";

const targets = [
  'expense/presentation/add_expense_page.dart',
  'settings/presentation/about_page.dart',
  'settings/presentation/change_pin_page.dart',
  'settings/presentation/business_hours_page.dart',
  'settings/presentation/share_app_page.dart',
  'settings/presentation/settings_page.dart',
  'settings/presentation/company_settings_page.dart',
  'support/presentation/create_support_ticket_page.dart',
  'support/presentation/support_tickets_page.dart',
  'support/presentation/support_ticket_detail_page.dart',
  'masters/presentation/product_form_page.dart',
  'masters/presentation/product_portions_page.dart',
  'masters/presentation/products_page.dart',
  'masters/presentation/portion_masters_page.dart',
  'masters/presentation/subcategories_page.dart',
  'masters/presentation/categories_page.dart',
  'masters/presentation/table_master_page.dart',
  'masters/presentation/masters_page.dart',
  'reports/presentation/expense_report_page.dart',
  'reports/presentation/invoice_detail_page.dart',
  'reports/presentation/product_wise_report_page.dart',
  'reports/presentation/mess_member_report_page.dart',
  'reports/presentation/table_list_report_page.dart',
  'reports/presentation/operational_report_page.dart',
  'print/presentation/test_invoice_preview_page.dart',
  'mess/presentation/mess_token_qr_page.dart',
  'mess/presentation/mess_coupon_page.dart',
  'mess/presentation/mess_payments_page.dart',
  'mess/presentation/mess_meal_sessions_page.dart',
  'mess/presentation/mess_page.dart',
  'pos/presentation/kot_preview_page.dart',
  'tables/presentation/split_bill_page.dart',
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
  let newInner = inner.replace(
    /padding:\s*const\s+EdgeInsets\.fromLTRB\(\s*16\s*,\s*([^,]+)\s*,\s*16\s*,\s*([^)]+)\)/,
    'padding: EdgeInsets.fromLTRB(\n' +
      '            AppBreakpoints.pagePaddingFor(context.widthClass),\n' +
      '            $1,\n' +
      '            AppBreakpoints.pagePaddingFor(context.widthClass),\n' +
      '            $2)',
  );
  newInner = newInner.replace(
    /padding:\s*const\s+EdgeInsets\.all\(\s*16\s*\)/,
    'padding: EdgeInsets.all(\n' +
      '            AppBreakpoints.pagePaddingFor(context.widthClass),\n' +
      '          )',
  );
  return newInner;
}

function search(re, text) {
  const m = re.exec(text);
  return m;
}

function findListViewOpen(text) {
  let m = search(/body:\s*ListView\s*\(/, text);
  if (m) {
    const openParen = text.indexOf('(', m.index + m[0].length - 1);
    const lvStart = text.lastIndexOf('ListView', openParen);
    return ['body_list', lvStart, openParen];
  }

  m = search(/body:\s*Form\(/, text);
  if (m) {
    const window = text.slice(m.index, m.index + 800);
    const lm = /child:\s*ListView\s*\(/.exec(window);
    if (lm) {
      const absStart = m.index + lm.index;
      const openParen = text.indexOf('(', absStart + lm[0].lastIndexOf('('));
      const lvStart = text.lastIndexOf('ListView', openParen);
      return ['form_list', lvStart, openParen];
    }
  }

  m = search(/Expanded\(\s*\n\s*child:\s*ListView\s*\(/, text);
  if (m) {
    const openParen = text.indexOf('(', m.index + m[0].length - 1);
    const lvStart = text.lastIndexOf('ListView', openParen);
    return ['expanded_list', lvStart, openParen];
  }

  m = search(/RefreshIndicator\(/, text);
  if (m) {
    const window = text.slice(m.index, m.index + 500);
    const lm = /child:\s*ListView\s*\(/.exec(window);
    if (lm) {
      const absStart = m.index + lm.index;
      const openParen = text.indexOf('(', absStart + lm[0].lastIndexOf('('));
      const lvStart = text.lastIndexOf('ListView', openParen);
      return ['refresh_list', lvStart, openParen];
    }
  }

  m = search(/return ListView\s*\(/, text);
  if (m) {
    const openParen = text.indexOf('(', m.index + m[0].length - 1);
    const lvStart = text.lastIndexOf('ListView', openParen);
    return ['return_list', lvStart, openParen];
  }

  return null;
}

const changed = [];
const skipped = [];
const errors = [];

for (const rel of targets) {
  const filePath = path.join(ROOT, rel);
  if (!fs.existsSync(filePath)) {
    skipped.push(`missing:${rel}`);
    continue;
  }
  let text = fs.readFileSync(filePath, 'utf8');
  if (text.includes('ResponsiveScrollShell')) {
    skipped.push(`already:${rel}`);
    continue;
  }

  const found = findListViewOpen(text);
  if (!found) {
    skipped.push(`nopattern:${rel}`);
    continue;
  }

  const [wrapKind, lvStart, openParen] = found;
  const closeParen = findMatchingParen(text, openParen);
  if (closeParen < 0) {
    errors.push(`paren:${rel}`);
    continue;
  }

  const inner = text.slice(lvStart, closeParen + 1);
  const newInner = updatePadding(inner);
  const wrapped =
    'ResponsiveScrollShell(\n' +
    '        dashboard: true,\n' +
    `        child: ${newInner},\n` +
    '      )';

  text = text.slice(0, lvStart) + wrapped + text.slice(closeParen + 1);
  text = ensureImports(text);
  fs.writeFileSync(filePath, text, 'utf8');
  changed.push(`${rel}:${wrapKind}`);
}

console.log('CHANGED', changed.length);
for (const c of changed) console.log(' ', c);
console.log('SKIPPED', skipped.length);
for (const s of skipped) console.log(' ', s);
console.log('ERRORS', errors.length);
for (const e of errors) console.log(' ', e);
