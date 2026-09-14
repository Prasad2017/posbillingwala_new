from pathlib import Path
import re

ROOT = Path(r"d:\Webus Labs\posbillingwala-flutter\lib\features")
IMPORT_BP = "import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';"
IMPORT_RL = "import 'package:pos_billingwala_v2/core/widgets/responsive_layout.dart';"

targets = [
    "expense/presentation/add_expense_page.dart",
    "settings/presentation/about_page.dart",
    "settings/presentation/change_pin_page.dart",
    "settings/presentation/business_hours_page.dart",
    "settings/presentation/share_app_page.dart",
    "settings/presentation/settings_page.dart",
    "settings/presentation/company_settings_page.dart",
    "support/presentation/create_support_ticket_page.dart",
    "support/presentation/support_tickets_page.dart",
    "support/presentation/support_ticket_detail_page.dart",
    "masters/presentation/product_form_page.dart",
    "masters/presentation/product_portions_page.dart",
    "masters/presentation/products_page.dart",
    "masters/presentation/portion_masters_page.dart",
    "masters/presentation/subcategories_page.dart",
    "masters/presentation/categories_page.dart",
    "masters/presentation/table_master_page.dart",
    "masters/presentation/masters_page.dart",
    "reports/presentation/expense_report_page.dart",
    "reports/presentation/invoice_detail_page.dart",
    "reports/presentation/product_wise_report_page.dart",
    "reports/presentation/mess_member_report_page.dart",
    "reports/presentation/table_list_report_page.dart",
    "reports/presentation/operational_report_page.dart",
    "print/presentation/test_invoice_preview_page.dart",
    "mess/presentation/mess_token_qr_page.dart",
    "mess/presentation/mess_coupon_page.dart",
    "mess/presentation/mess_payments_page.dart",
    "mess/presentation/mess_meal_sessions_page.dart",
    "mess/presentation/mess_page.dart",
    "pos/presentation/kot_preview_page.dart",
    "tables/presentation/split_bill_page.dart",
]


def find_matching_paren(s: str, open_idx: int) -> int:
    depth = 0
    i = open_idx
    in_str = None
    while i < len(s):
        ch = s[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == in_str:
                in_str = None
            i += 1
            continue
        if ch in ("'", '"'):
            in_str = ch
            i += 1
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def ensure_imports(text: str) -> str:
    if IMPORT_RL in text and IMPORT_BP in text:
        return text
    lines = text.splitlines(True)
    last_import = -1
    for i, line in enumerate(lines):
        if line.startswith("import "):
            last_import = i
    inserts = []
    if IMPORT_BP not in text:
        inserts.append(IMPORT_BP + "\n")
    if IMPORT_RL not in text:
        inserts.append(IMPORT_RL + "\n")
    if last_import >= 0 and inserts:
        lines[last_import + 1 : last_import + 1] = inserts
        return "".join(lines)
    return text


def update_padding(inner: str) -> str:
    new_inner = re.sub(
        r"padding:\s*const\s+EdgeInsets\.fromLTRB\(\s*16\s*,\s*([^,]+)\s*,\s*16\s*,\s*([^)]+)\)",
        r"padding: EdgeInsets.fromLTRB(\n"
        r"            AppBreakpoints.pagePaddingFor(context.widthClass),\n"
        r"            \1,\n"
        r"            AppBreakpoints.pagePaddingFor(context.widthClass),\n"
        r"            \2)",
        inner,
        count=1,
    )
    new_inner = re.sub(
        r"padding:\s*const\s+EdgeInsets\.all\(\s*16\s*\)",
        "padding: EdgeInsets.all(\n"
        "            AppBreakpoints.pagePaddingFor(context.widthClass),\n"
        "          )",
        new_inner,
        count=1,
    )
    return new_inner


def find_listview_open(text: str):
    """Return (wrap_kind, listview_token_start, open_paren_idx) or None."""
    m = re.search(r"body:\s*ListView\s*\(", text)
    if m:
        open_paren = text.find("(", m.end() - 1)
        lv_start = text.rfind("ListView", 0, open_paren)
        return "body_list", lv_start, open_paren

    m = re.search(r"body:\s*Form\(", text)
    if m:
        lm = re.search(r"child:\s*ListView\s*\(", text[m.start() : m.start() + 800])
        if lm:
            abs_start = m.start() + lm.start()
            open_paren = text.find("(", abs_start + lm.group(0).rfind("("))
            lv_start = text.rfind("ListView", 0, open_paren)
            return "form_list", lv_start, open_paren

    m = re.search(r"Expanded\(\s*\n\s*child:\s*ListView\s*\(", text)
    if m:
        open_paren = text.find("(", m.end() - 1)
        lv_start = text.rfind("ListView", 0, open_paren)
        return "expanded_list", lv_start, open_paren

    m = re.search(r"RefreshIndicator\(", text)
    if m:
        window = text[m.start() : m.start() + 500]
        lm = re.search(r"child:\s*ListView\s*\(", window)
        if lm:
            abs_start = m.start() + lm.start()
            open_paren = text.find("(", abs_start + lm.group(0).rfind("("))
            lv_start = text.rfind("ListView", 0, open_paren)
            return "refresh_list", lv_start, open_paren

    # Prefer Scaffold body return ListView inside when()/builder — first return ListView
    # but only if not already inside ResponsiveScrollShell later
    for m in re.finditer(r"return ListView\s*\(", text):
        open_paren = text.find("(", m.end() - 1)
        lv_start = text.rfind("ListView", 0, open_paren)
        return "return_list", lv_start, open_paren

    return None


changed = []
skipped = []
errors = []

for rel in targets:
    path = ROOT / rel
    if not path.exists():
        skipped.append(f"missing:{rel}")
        continue
    text = path.read_text(encoding="utf-8")
    if "ResponsiveScrollShell" in text:
        skipped.append(f"already:{rel}")
        continue

    found = find_listview_open(text)
    if not found:
        skipped.append(f"nopattern:{rel}")
        continue

    wrap_kind, lv_start, open_paren = found
    close_paren = find_matching_paren(text, open_paren)
    if close_paren < 0:
        errors.append(f"paren:{rel}")
        continue

    inner = text[lv_start : close_paren + 1]
    new_inner = update_padding(inner)
    wrapped = (
        "ResponsiveScrollShell(\n"
        "        dashboard: true,\n"
        f"        child: {new_inner},\n"
        "      )"
    )
    text = text[:lv_start] + wrapped + text[close_paren + 1 :]
    text = ensure_imports(text)
    path.write_text(text, encoding="utf-8")
    changed.append(f"{rel}:{wrap_kind}")

print("CHANGED", len(changed))
for c in changed:
    print(" ", c)
print("SKIPPED", len(skipped))
for s in skipped:
    print(" ", s)
print("ERRORS", len(errors))
for e in errors:
    print(" ", e)
