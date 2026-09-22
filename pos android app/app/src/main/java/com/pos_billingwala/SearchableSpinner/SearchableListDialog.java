package com.pos_billingwala.SearchableSpinner;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.R;

import java.io.Serializable;
import java.util.List;

public class SearchableListDialog extends BottomSheetDialogFragment implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static final String ITEMS = "items";

    private ArrayAdapter listAdapter;

    private ListView _listViewItems;

    private SearchableItem _searchableItem;

    private OnSearchTextChanged _onSearchTextChanged;

    private SearchView _searchView;

    private String _strTitle;

    private String _strPositiveButtonText;

    public SearchableListDialog() {

    }

    public static SearchableListDialog newInstance(List items) {
        SearchableListDialog multiSelectExpandableFragment = new SearchableListDialog();

        Bundle args = new Bundle();
        args.putSerializable(ITEMS, (Serializable) items);

        multiSelectExpandableFragment.setArguments(args);

        return multiSelectExpandableFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (null != savedInstanceState) {
            _searchableItem = (SearchableItem) savedInstanceState.getSerializable("item");
        }

        View rootView = inflater.inflate(R.layout.searchable_list_dialog, container, false);
        setData(rootView);

        String strTitle = _strTitle == null ? "Select Item" : _strTitle;
        TextView titleView = rootView.findViewById(R.id.sheetTitle);
        if (titleView != null) {
            titleView.setText(strTitle);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            BottomSheetUi.applyFullWidth((com.google.android.material.bottomsheet.BottomSheetDialog) getDialog());
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable("item", _searchableItem);
        super.onSaveInstanceState(outState);
    }

    public void setTitle(String strTitle) {
        _strTitle = strTitle;
    }

    public void setPositiveButton(String strPositiveButtonText) {
        _strPositiveButtonText = strPositiveButtonText;
    }

    public void setPositiveButton(String strPositiveButtonText, android.content.DialogInterface.OnClickListener onClickListener) {
        _strPositiveButtonText = strPositiveButtonText;
    }

    public void setOnSearchableItemClickListener(SearchableItem searchableItem) {
        this._searchableItem = searchableItem;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChanged onSearchTextChanged) {
        this._onSearchTextChanged = onSearchTextChanged;
    }

    private void setData(View rootView) {
        SearchManager searchManager = (SearchManager) requireActivity().getSystemService(Context.SEARCH_SERVICE);

        _searchView = rootView.findViewById(R.id.search);
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().getComponentName()));
        _searchView.setIconifiedByDefault(false);
        _searchView.setOnQueryTextListener(this);
        _searchView.setOnCloseListener(this);
        _searchView.clearFocus();
        InputMethodManager mgr = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(_searchView.getWindowToken(), 0);

        List items = (List) requireArguments().getSerializable(ITEMS);

        _listViewItems = rootView.findViewById(R.id.listItems);

        listAdapter = new ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, items);
        _listViewItems.setAdapter(listAdapter);
        _listViewItems.setTextFilterEnabled(true);

        _listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                _searchableItem.onSearchableItemClicked(listAdapter.getItem(position), position);
                dismiss();
            }
        });
    }

    @Override
    public boolean onClose() {
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        _searchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        if (TextUtils.isEmpty(s)) {
            ((ArrayAdapter) _listViewItems.getAdapter()).getFilter().filter(null);
        } else {
            ((ArrayAdapter) _listViewItems.getAdapter()).getFilter().filter(s);
        }
        if (null != _onSearchTextChanged) {
            _onSearchTextChanged.onSearchTextChanged(s);
        }
        return true;
    }

    public interface SearchableItem<T> extends Serializable {
        void onSearchableItemClicked(T item, int position);
    }

    public interface OnSearchTextChanged {
        void onSearchTextChanged(String strText);
    }
}
