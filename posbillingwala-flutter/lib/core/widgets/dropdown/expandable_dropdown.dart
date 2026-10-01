import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/constants/app_colors.dart';
import 'package:pos_billingwala_v2/core/widgets/dropdown/dropdown_theme.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_strings.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_theme.dart';

const int kDropdownSearchMinOptionCount = 6;

enum ExpandableDropdownMode { single, multi }

class ExpandableDropdownField<T> extends StatefulWidget {
  const ExpandableDropdownField({
    super.key,
    required this.label,
    required this.hint,
    required this.items,
    required this.itemLabel,
    this.itemComparer,
    this.enableSearch = true,
    this.searchHint,
    this.maxListHeight = 220,
    this.mode = ExpandableDropdownMode.single,
    this.value,
    this.onChanged,
    this.selectedValues,
    this.onSelectionChanged,
    this.actionLabel,
    this.onActionTap,
    this.emptyText,
    this.showLabel = true,
    this.multiSelectLabelBuilder,
    this.hasError = false,
    this.fitContent = false,
  }) : assert(
         mode == ExpandableDropdownMode.single
             ? onChanged != null
             : onSelectionChanged != null && selectedValues != null,
         'Provide onChanged for single select or selectedValues/onSelectionChanged for multi select.',
       );

  final String label;
  final String hint;
  final List<T> items;
  final String Function(T item) itemLabel;
  final bool Function(T a, T b)? itemComparer;
  final bool enableSearch;
  final String? searchHint;
  final double maxListHeight;
  final ExpandableDropdownMode mode;
  final T? value;
  final ValueChanged<T?>? onChanged;
  final Set<T>? selectedValues;
  final ValueChanged<Set<T>>? onSelectionChanged;
  final String? actionLabel;
  final VoidCallback? onActionTap;
  final String? emptyText;
  final bool showLabel;
  final String Function(int count, T? singleValue)? multiSelectLabelBuilder;
  final bool hasError;

  /* When true, the field sizes to its content instead of stretching full width. */
  final bool fitContent;

  @override
  State<ExpandableDropdownField<T>> createState() =>
      ExpandableDropdownFieldState<T>();
}

class ExpandableDropdownFieldState<T>
    extends State<ExpandableDropdownField<T>> {
  var expanded = false;
  final searchController = TextEditingController();

  bool itemsEqual(T a, T b) => widget.itemComparer?.call(a, b) ?? a == b;

  bool isSelected(T item) {
    if (widget.mode == ExpandableDropdownMode.multi) {
      return widget.selectedValues!.any((value) => itemsEqual(value, item));
    }
    final selected = widget.value;
    return selected != null && itemsEqual(selected, item);
  }

  List<T> filteredItems() {
    final query = searchController.text.trim().toLowerCase();
    if (query.isEmpty) return widget.items;
    return widget.items
        .where((item) => widget.itemLabel(item).toLowerCase().contains(query))
        .toList();
  }

  String expandableDropdownTriggerLabel() {
    if (widget.mode == ExpandableDropdownMode.multi) {
      final selected = widget.selectedValues!;
      if (selected.isEmpty) return widget.hint;
      if (widget.multiSelectLabelBuilder != null) {
        return widget.multiSelectLabelBuilder!(
          selected.length,
          selected.length == 1 ? selected.first : null,
        );
      }
      if (selected.length == 1) {
        return widget.itemLabel(selected.first);
      }
      return WidgetStrings.selectedCount(selected.length);
    }

    final selected = widget.value;
    if (selected == null) return widget.hint;
    return widget.itemLabel(selected);
  }

  bool get hasSelection {
    if (widget.mode == ExpandableDropdownMode.multi) {
      return widget.selectedValues!.isNotEmpty;
    }
    return widget.value != null;
  }

  void closeDropdown() {
    if (!expanded) return;
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() {
      expanded = false;
      searchController.clear();
    });
  }

  void toggleExpanded() {
    setState(() {
      expanded = !expanded;
      if (!expanded) {
        searchController.clear();
      }
    });
  }

  void handleItemTap(T item) {
    if (widget.mode == ExpandableDropdownMode.multi) {
      final next = {...widget.selectedValues!};
      if (isSelected(item)) {
        next.removeWhere((value) => itemsEqual(value, item));
      } else {
        next.add(item);
      }
      widget.onSelectionChanged!(next);
      closeDropdown();
      return;
    }

    widget.onChanged!(item);
    closeDropdown();
  }

  @override
  void dispose() {
    /* Collapse first so the search TextField detaches before dispose. */
    expanded = false;
    searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bodyStyle =
        Theme.of(context).textTheme.bodyMedium ?? const TextStyle();
    final bodySm =
        Theme.of(context).textTheme.bodySmall ?? const TextStyle(fontSize: 12);
    final items = filteredItems();

    final triggerLabel = Text(
      expandableDropdownTriggerLabel(),
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: bodyStyle.copyWith(
        color: hasSelection ? context.textPrimary : context.textSecondary,
        fontSize: 14,
      ),
    );

    final trigger = InkWell(
      onTap: toggleExpanded,
      borderRadius: BorderRadius.circular(AppFieldBorders.radius),
      child: Row(
        mainAxisSize: widget.fitContent ? MainAxisSize.min : MainAxisSize.max,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          if (widget.fitContent) triggerLabel else Expanded(child: triggerLabel),
          const SizedBox(width: 8),
          Icon(
            expanded
                ? Icons.keyboard_arrow_up_rounded
                : Icons.keyboard_arrow_down_rounded,
            color: AppColors.textSecondary,
          ),
        ],
      ),
    );

    final panel = Column(
      crossAxisAlignment: widget.fitContent
          ? CrossAxisAlignment.start
          : CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: [
        trigger,
        if (expanded) ...[
          const SizedBox(height: 8),
          Divider(height: 1, color: AppColors.border.withValues(alpha: .9)),
          if (widget.enableSearch && !widget.fitContent)
            Padding(
              padding: const EdgeInsets.fromLTRB(0, 8, 0, 6),
              child: TextField(
                controller: searchController,
                textCapitalization: TextCapitalization.words,
                onChanged: (_) => setState(() {}),
                cursorColor: context.textPrimary,
                style: bodyStyle.copyWith(
                  color: context.textPrimary,
                  fontSize: 14,
                ),
                decoration: InputDecoration(
                  isDense: true,
                  hintText: widget.searchHint ?? WidgetStrings.searchOptions,
                  hintStyle: bodyStyle.copyWith(
                    color: context.textSecondary,
                    fontSize: 14,
                  ),
                  prefixIcon: Icon(
                    Icons.search_rounded,
                    size: 20,
                    color: context.textSecondary,
                  ),
                  filled: true,
                  fillColor: AppColors.glassSolid,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 10,
                  ),
                  border: AppFieldBorders.enabled,
                  enabledBorder: AppFieldBorders.enabled,
                  focusedBorder: AppFieldBorders.focused,
                ),
              ),
            ),
          ConstrainedBox(
            constraints: BoxConstraints(maxHeight: widget.maxListHeight),
            child: items.isEmpty
                ? Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 4,
                      vertical: 16,
                    ),
                    child: Text(
                      widget.emptyText ?? WidgetStrings.noResultsFound,
                      style: bodySm.copyWith(color: context.textSecondary),
                      textAlign: TextAlign.center,
                    ),
                  )
                /* fitContent must not use ListView/viewport — parents may */
                /* measure intrinsics, and ShrinkWrappingViewport forbids that. */
                : widget.fitContent
                ? Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      for (var index = 0; index < items.length; index++)
                        buildOption(
                          context: context,
                          bodyStyle: bodyStyle,
                          item: items[index],
                          isFirst: index == 0,
                        ),
                      if (widget.actionLabel != null) buildAction(bodyStyle),
                    ],
                  )
                : ListView.builder(
                    shrinkWrap: true,
                    padding: EdgeInsets.zero,
                    physics: const ClampingScrollPhysics(),
                    itemCount:
                        items.length + (widget.actionLabel != null ? 1 : 0),
                    itemBuilder: (context, index) {
                      if (widget.actionLabel != null &&
                          index == items.length) {
                        return buildAction(bodyStyle);
                      }
                      return buildOption(
                        context: context,
                        bodyStyle: bodyStyle,
                        item: items[index],
                        isFirst: index == 0,
                      );
                    },
                  ),
          ),
        ],
      ],
    );

    final decoration = appDropdownDecoration(
      context,
      label: widget.label,
      hint: widget.hint,
      showLabel: widget.showLabel && !widget.fitContent,
      hasError: widget.hasError,
      isDense: widget.fitContent,
    );

    final field = TapRegion(
      onTapOutside: (_) => closeDropdown(),
      child: InputDecorator(
        isFocused: expanded,
        isHovering: false,
        isEmpty: !hasSelection,
        decoration: decoration,
        child: panel,
      ),
    );

    if (widget.fitContent) {
      return Align(alignment: Alignment.centerLeft, child: field);
    }
    return field;
  }

  Widget buildAction(TextStyle bodyStyle) {
    return InkWell(
      onTap: () {
        widget.onActionTap?.call();
        closeDropdown();
      },
      child: Padding(
        padding: const EdgeInsets.fromLTRB(4, 6, 4, 10),
        child: Text(
          widget.actionLabel!,
          style: bodyStyle.copyWith(
            color: AppColors.primary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }

  Widget buildOption({
    required BuildContext context,
    required TextStyle bodyStyle,
    required T item,
    required bool isFirst,
  }) {
    final selected = isSelected(item);
    return Material(
      color: selected && widget.mode == ExpandableDropdownMode.multi
          ? context.subtleBackground
          : Colors.transparent,
      child: InkWell(
        onTap: () => handleItemTap(item),
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            widget.fitContent ? 4 : 4,
            isFirst ? 6 : 8,
            widget.fitContent ? 4 : 4,
            8,
          ),
          child: Text(
            widget.itemLabel(item),
            style: bodyStyle.copyWith(
              color: selected ? AppColors.primary : context.textPrimary,
              fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
            ),
          ),
        ),
      ),
    );
  }
}
