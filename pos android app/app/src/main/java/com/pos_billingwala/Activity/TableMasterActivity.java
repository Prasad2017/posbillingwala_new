package com.pos_billingwala.Activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.pos_billingwala.Database.POSBillingWalaDatabase;
import com.pos_billingwala.Extra.BottomSheetUi;
import com.pos_billingwala.Extra.EmptyListUi;
import com.pos_billingwala.Extra.SearchableDropdownView;
import com.pos_billingwala.Model.DiningAreaResponse;
import com.pos_billingwala.Model.PosTableResponse;
import com.pos_billingwala.Model.TableTypeResponse;
import com.pos_billingwala.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage dine-in areas, seating types, and tables with capacity.
 */
public class TableMasterActivity extends BaseActivity {

    private static final int TAB_TABLES = 0;
    private static final int TAB_TYPES = 1;
    private static final int TAB_AREAS = 2;

    private POSBillingWalaDatabase db;
    private RecyclerView recyclerView;
    private TextView tabTables, tabTypes, tabAreas, sectionHint, btnAdd;
    private final MasterAdapter adapter = new MasterAdapter();
    private int currentTab = TAB_AREAS;

    private final List<PosTableResponse> tables = new ArrayList<>();
    private final List<TableTypeResponse> types = new ArrayList<>();
    private final List<DiningAreaResponse> areas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_master);
        db = new POSBillingWalaDatabase(this);
        db.ensureDineInMastersSeeded();

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        tabTables = findViewById(R.id.tabTables);
        tabTypes = findViewById(R.id.tabTypes);
        tabAreas = findViewById(R.id.tabAreas);
        sectionHint = findViewById(R.id.sectionHint);
        btnAdd = findViewById(R.id.btnAdd);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tabTables.setOnClickListener(v -> selectTab(TAB_TABLES));
        tabTypes.setOnClickListener(v -> selectTab(TAB_TYPES));
        tabAreas.setOnClickListener(v -> selectTab(TAB_AREAS));
        btnAdd.setOnClickListener(v -> onAddClicked());

        selectTab(TAB_AREAS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void selectTab(int tab) {
        currentTab = tab;
        styleTab(tabTables, tab == TAB_TABLES);
        styleTab(tabTypes, tab == TAB_TYPES);
        styleTab(tabAreas, tab == TAB_AREAS);
        if (tab == TAB_TABLES) {
            updateTablesAddButton();
        } else if (tab == TAB_TYPES) {
            sectionHint.setText("Create seating types like 2 Seater / 4 Seater");
            btnAdd.setText("+ Add Type");
            btnAdd.setEnabled(true);
            btnAdd.setAlpha(1f);
        } else {
            sectionHint.setText("Create areas like Hall, AC, Non-AC, Garden");
            btnAdd.setText("+ Add Area");
            btnAdd.setEnabled(true);
            btnAdd.setAlpha(1f);
        }
        reload();
    }

    private void updateTablesAddButton() {
        int configured = db.getConfiguredTableCount();
        int current = db.countActivePosTables();
        if (configured <= 0) {
            sectionHint.setText("Set No of Table in Shop Details first");
            btnAdd.setText("+ Add Table");
            btnAdd.setEnabled(true);
            btnAdd.setAlpha(1f);
            return;
        }
        sectionHint.setText("Using Shop Details table count: " + current + " of " + configured);
        btnAdd.setText("+ Add Table (" + current + "/" + configured + ")");
        boolean canAdd = current < configured;
        btnAdd.setEnabled(canAdd);
        btnAdd.setAlpha(canAdd ? 1f : 0.5f);
    }

    private void styleTab(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_table_area_chip_selected);
            tab.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            tab.setBackgroundResource(R.drawable.bg_table_area_chip);
            tab.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        }
    }

    private void reload() {
        types.clear();
        types.addAll(db.getTableTypes());
        areas.clear();
        areas.addAll(db.getDiningAreas());
        tables.clear();
        tables.addAll(db.getActivePosTables());
        adapter.notifyDataSetChanged();
        View empty = findViewById(R.id.noDataFound);
        int emptySub = R.string.empty_sub_table_master_areas;
        boolean hasData;
        if (currentTab == TAB_TABLES) {
            hasData = !tables.isEmpty();
            emptySub = R.string.empty_sub_table_master_tables;
            updateTablesAddButton();
        } else if (currentTab == TAB_TYPES) {
            hasData = !types.isEmpty();
            emptySub = R.string.empty_sub_table_master_types;
        } else {
            hasData = !areas.isEmpty();
        }
        recyclerView.setVisibility(hasData ? View.VISIBLE : View.GONE);
        EmptyListUi.bind(empty, hasData, emptySub);
    }

    private void onAddClicked() {
        if (currentTab == TAB_TABLES) {
            int configured = db.getConfiguredTableCount();
            int current = db.countActivePosTables();
            if (configured <= 0) {
                Toast.makeText(this,
                        "Set No of Table in Settings → Shop Details first",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (current >= configured) {
                Toast.makeText(this,
                        "Limit " + configured + " tables. Increase No of Table in Shop Details to add more.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            showTableForm(null);
        } else if (currentTab == TAB_TYPES) {
            showTypeForm(null);
        } else {
            showAreaForm(null);
        }
    }

    private void showAreaForm(DiningAreaResponse existing) {
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_table_master_form, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);
        TextView title = content.findViewById(R.id.dialogTitle);
        TextInputLayout oneLayout = content.findViewById(R.id.fieldOneLayout);
        TextInputEditText one = content.findViewById(R.id.fieldOne);
        content.findViewById(R.id.fieldTwoLayout).setVisibility(View.GONE);
        content.findViewById(R.id.fieldThreeLayout).setVisibility(View.GONE);
        content.findViewById(R.id.areaLabel).setVisibility(View.GONE);
        content.findViewById(R.id.areaDropdown).setVisibility(View.GONE);
        content.findViewById(R.id.typeLabel).setVisibility(View.GONE);
        content.findViewById(R.id.typeDropdown).setVisibility(View.GONE);

        title.setText(existing == null ? "Add Area" : "Edit Area");
        one.setHint("Area name");
        one.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        oneLayout.setHint("Area name");
        if (existing != null) {
            one.setText(existing.getAreaName());
        }

        content.findViewById(R.id.btnCancel).setOnClickListener(v -> sheet.dismiss());
        content.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = textOf(one);
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Enter area name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (existing == null) {
                if (db.insertDiningArea(name) <= 0) {
                    Toast.makeText(this, "Could not save area", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (!db.updateDiningArea(existing.getAreaId(), name)) {
                Toast.makeText(this, "Could not update area", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            reload();
        });
    }

    private void showTypeForm(TableTypeResponse existing) {
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_table_master_form, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);
        TextView title = content.findViewById(R.id.dialogTitle);
        TextInputEditText one = content.findViewById(R.id.fieldOne);
        TextInputEditText two = content.findViewById(R.id.fieldTwo);
        content.findViewById(R.id.fieldThreeLayout).setVisibility(View.GONE);
        content.findViewById(R.id.areaLabel).setVisibility(View.GONE);
        content.findViewById(R.id.areaDropdown).setVisibility(View.GONE);
        content.findViewById(R.id.typeLabel).setVisibility(View.GONE);
        content.findViewById(R.id.typeDropdown).setVisibility(View.GONE);

        title.setText(existing == null ? "Add Table Type" : "Edit Table Type");
        one.setHint("Type name");
        one.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        two.setHint("Default seats");
        two.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        ((TextInputLayout) content.findViewById(R.id.fieldOneLayout)).setHint("Type name");
        ((TextInputLayout) content.findViewById(R.id.fieldTwoLayout)).setHint("Default seats");

        if (existing != null) {
            one.setText(existing.getTableTypeName());
            two.setText(existing.getDefaultCapacity());
        } else {
            two.setText("4");
        }

        content.findViewById(R.id.btnCancel).setOnClickListener(v -> sheet.dismiss());
        content.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = textOf(one);
            int seats = parseSeats(textOf(two), 4);
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Enter type name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (existing == null) {
                if (db.insertTableType(name, seats) <= 0) {
                    Toast.makeText(this, "Could not save type", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (!db.updateTableType(existing.getTableTypeId(), name, seats)) {
                Toast.makeText(this, "Could not update type", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            reload();
        });
    }

    private void showTableForm(PosTableResponse existing) {
        if (areas.isEmpty()) {
            Toast.makeText(this, "Add an Area first (Hall / AC / …)", Toast.LENGTH_SHORT).show();
            selectTab(TAB_AREAS);
            return;
        }
        if (types.isEmpty()) {
            Toast.makeText(this, "Add a Table Type first (2 Seater / 4 Seater)", Toast.LENGTH_SHORT).show();
            selectTab(TAB_TYPES);
            return;
        }

        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_table_master_form, null);
        BottomSheetDialog sheet = BottomSheetUi.showContent(this, content, true);
        TextView title = content.findViewById(R.id.dialogTitle);
        TextInputEditText number = content.findViewById(R.id.fieldOne);
        TextInputEditText name = content.findViewById(R.id.fieldTwo);
        TextInputEditText seats = content.findViewById(R.id.fieldThree);
        SearchableDropdownView areaDropdown = content.findViewById(R.id.areaDropdown);
        SearchableDropdownView typeDropdown = content.findViewById(R.id.typeDropdown);

        title.setText(existing == null ? "Add Table" : "Edit Table");
        ((TextInputLayout) content.findViewById(R.id.fieldOneLayout)).setHint("Table No");
        ((TextInputLayout) content.findViewById(R.id.fieldTwoLayout)).setHint("Table Name");
        ((TextInputLayout) content.findViewById(R.id.fieldThreeLayout)).setHint("Seats");

        List<String> areaLabels = new ArrayList<>();
        for (DiningAreaResponse a : areas) {
            areaLabels.add(a.getAreaName());
        }
        List<String> typeLabels = new ArrayList<>();
        for (TableTypeResponse t : types) {
            typeLabels.add(t.getTableTypeName() + " (" + safe(t.getDefaultCapacity()) + " seats)");
        }
        areaDropdown.setItems(areaLabels);
        typeDropdown.setItems(typeLabels);
        areaDropdown.setHint("Select area");
        typeDropdown.setHint("Select type");

        final int[] selectedArea = {0};
        final int[] selectedType = {0};
        areaDropdown.setOnItemSelectedListener((pos, label) -> selectedArea[0] = pos);
        typeDropdown.setOnItemSelectedListener((pos, label) -> {
            selectedType[0] = pos;
            if (existing == null && pos >= 0 && pos < types.size()) {
                seats.setText(types.get(pos).getDefaultCapacity());
            }
        });

        if (existing != null) {
            number.setText(existing.getTableNumber());
            name.setText(existing.getTableName());
            seats.setText(existing.getCapacity());
            selectedArea[0] = indexOfArea(existing.getAreaId());
            selectedType[0] = indexOfType(existing.getTableTypeId());
            if (selectedArea[0] >= 0) {
                areaDropdown.setSelectedIndex(selectedArea[0]);
            }
            if (selectedType[0] >= 0) {
                typeDropdown.setSelectedIndex(selectedType[0]);
            }
        } else {
            int next = db.nextSuggestedTableNumber();
            number.setText(String.valueOf(next));
            name.setText("T" + next);
            areaDropdown.setSelectedIndex(0);
            typeDropdown.setSelectedIndex(0);
            seats.setText(types.get(0).getDefaultCapacity());
            selectedArea[0] = 0;
            selectedType[0] = 0;
        }

        content.findViewById(R.id.btnCancel).setOnClickListener(v -> sheet.dismiss());
        content.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String tableNo = textOf(number);
            String tableName = textOf(name);
            int capacity = parseSeats(textOf(seats), 4);
            if (TextUtils.isEmpty(tableNo)) {
                Toast.makeText(this, "Enter table number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedArea[0] < 0 || selectedArea[0] >= areas.size()) {
                Toast.makeText(this, "Select area", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedType[0] < 0 || selectedType[0] >= types.size()) {
                Toast.makeText(this, "Select table type", Toast.LENGTH_SHORT).show();
                return;
            }
            String areaId = areas.get(selectedArea[0]).getAreaId();
            String typeId = types.get(selectedType[0]).getTableTypeId();
            if (existing == null) {
                long id = db.insertPosTable(tableNo, tableName, areaId, typeId, capacity);
                if (id == -2) {
                    Toast.makeText(this, "Table number already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (id <= 0) {
                    Toast.makeText(this, "Could not save table", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (!db.updatePosTable(existing.getTableId(), tableNo, tableName, areaId, typeId, capacity)) {
                Toast.makeText(this, "Could not update (number may already exist)", Toast.LENGTH_SHORT).show();
                return;
            }
            sheet.dismiss();
            reload();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
    }

    private int indexOfArea(String areaId) {
        if (areaId == null) {
            return 0;
        }
        for (int i = 0; i < areas.size(); i++) {
            if (areaId.equals(areas.get(i).getAreaId())) {
                return i;
            }
        }
        return 0;
    }

    private int indexOfType(String typeId) {
        if (typeId == null) {
            return 0;
        }
        for (int i = 0; i < types.size(); i++) {
            if (typeId.equals(types.get(i).getTableTypeId())) {
                return i;
            }
        }
        return 0;
    }

    private static String textOf(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private static int parseSeats(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "0" : value.trim();
    }

    private class MasterAdapter extends RecyclerView.Adapter<MasterAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_table_master_row, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (currentTab == TAB_TABLES) {
                PosTableResponse table = tables.get(position);
                holder.title.setText(table.getDisplayCode() + "  ·  No " + safe(table.getTableNumber()));
                holder.subtitle.setText(
                        safe(table.getAreaName()) + "  ·  "
                                + safe(table.getTableTypeName()) + "  ·  "
                                + safe(table.getCapacity()) + " seats");
                holder.edit.setOnClickListener(v -> showTableForm(table));
                holder.delete.setOnClickListener(v -> {
                    db.deactivatePosTable(table.getTableId());
                    reload();
                });
            } else if (currentTab == TAB_TYPES) {
                TableTypeResponse type = types.get(position);
                holder.title.setText(type.getTableTypeName());
                holder.subtitle.setText(safe(type.getDefaultCapacity()) + " seats default");
                holder.edit.setOnClickListener(v -> showTypeForm(type));
                holder.delete.setOnClickListener(v -> {
                    db.deactivateTableType(type.getTableTypeId());
                    reload();
                });
            } else {
                DiningAreaResponse area = areas.get(position);
                holder.title.setText(area.getAreaName());
                holder.subtitle.setText("Floor area filter");
                holder.edit.setOnClickListener(v -> showAreaForm(area));
                holder.delete.setOnClickListener(v -> {
                    db.deactivateDiningArea(area.getAreaId());
                    reload();
                });
            }
        }

        @Override
        public int getItemCount() {
            if (currentTab == TAB_TABLES) {
                return tables.size();
            }
            if (currentTab == TAB_TYPES) {
                return types.size();
            }
            return areas.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView title, subtitle, edit, delete;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.rowTitle);
                subtitle = itemView.findViewById(R.id.rowSubtitle);
                edit = itemView.findViewById(R.id.rowActionEdit);
                delete = itemView.findViewById(R.id.rowActionDelete);
            }
        }
    }
}
