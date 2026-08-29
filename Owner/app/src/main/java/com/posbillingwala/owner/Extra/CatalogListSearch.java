package com.posbillingwala.owner.Extra;

import android.text.TextUtils;
import android.view.View;

import java.util.Locale;

public final class CatalogListSearch {

    private CatalogListSearch() {
    }

    public static boolean matches(String query, String... fields) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        StringBuilder haystack = new StringBuilder();
        if (fields != null) {
            for (String field : fields) {
                if (!TextUtils.isEmpty(field)) {
                    haystack.append(field);
                }
            }
        }
        return haystack.toString().toLowerCase(Locale.getDefault()).contains(normalizedQuery);
    }

    public static String currentQuery(android.widget.EditText searchInput) {
        if (searchInput == null || searchInput.getText() == null) {
            return "";
        }
        return searchInput.getText().toString();
    }

    public static void showFilterEmpty(View listView, View noResultsView, boolean hasMatches) {
        if (listView != null) {
            listView.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
        }
        if (noResultsView != null) {
            noResultsView.setVisibility(hasMatches ? View.GONE : View.VISIBLE);
        }
    }
}
