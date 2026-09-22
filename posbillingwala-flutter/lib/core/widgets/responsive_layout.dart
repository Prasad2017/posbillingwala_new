import 'package:flutter/material.dart';
import 'package:pos_billingwala_v2/core/theme/app_breakpoints.dart';
import 'package:pos_billingwala_v2/core/theme/app_dimensions.dart';

/* Centers [child] within a max width that grows with [AppWidthClass]. */
class ResponsiveContent extends StatelessWidget {
  const ResponsiveContent({
    super.key,
    required this.child,
    this.maxWidth,
    this.dashboard = false,
    this.padding,
    this.alignment = Alignment.topCenter,
  });

  final Widget child;
  final double? maxWidth;
  final bool dashboard;
  final EdgeInsetsGeometry? padding;
  final AlignmentGeometry alignment;

  @override
  Widget build(BuildContext context) {
    final w = context.widthClass;
    final resolvedMax =
        maxWidth ??
        (dashboard
            ? AppBreakpoints.dashboardMaxWidthFor(w)
            : AppBreakpoints.contentMaxWidthFor(w));
    final resolvedPadding =
        padding ??
        EdgeInsets.symmetric(horizontal: AppBreakpoints.pagePaddingFor(w));

    return Align(
      alignment: alignment,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: resolvedMax),
        child: Padding(padding: resolvedPadding, child: child),
      ),
    );
  }
}

/* Scrollable page body with responsive max-width — default for hub / form screens. */
/* */
/* Use as `Scaffold.body`. Always scrollable (keyboard + overflow safe). */
class ResponsivePageBody extends StatelessWidget {
  const ResponsivePageBody({
    super.key,
    required this.child,
    this.padding,
    this.maxWidth,
    this.dashboard = false,
    this.physics,
    this.controller,
    this.primary,
  });

  /* When [child] is already a scrollable (e.g. [ListView]), prefer */
  /* [ResponsivePageBody.sliver] or wrap with [ResponsiveContent] instead. */
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final double? maxWidth;
  final bool dashboard;
  final ScrollPhysics? physics;
  final ScrollController? controller;
  final bool? primary;

  @override
  Widget build(BuildContext context) {
    final w = context.widthClass;
    final hPad = AppBreakpoints.pagePaddingFor(w);
    final resolvedPadding =
        padding ??
        EdgeInsets.fromLTRB(hPad, AppDimensions.lg, hPad, AppDimensions.xxl);

    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          controller: controller,
          primary: primary,
          physics: physics ?? const AlwaysScrollableScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: ResponsiveContent(
              maxWidth: maxWidth,
              dashboard: dashboard,
              padding: resolvedPadding,
              child: child,
            ),
          ),
        );
      },
    );
  }
}

/* Wraps an existing vertical [ListView]/[CustomScrollView] with centered max-width. */
class ResponsiveScrollShell extends StatelessWidget {
  const ResponsiveScrollShell({
    super.key,
    required this.child,
    this.maxWidth,
    this.dashboard = true,
  });

  final Widget child;
  final double? maxWidth;
  final bool dashboard;

  @override
  Widget build(BuildContext context) {
    final w = context.widthClass;
    final resolvedMax =
        maxWidth ??
        (dashboard
            ? AppBreakpoints.dashboardMaxWidthFor(w)
            : AppBreakpoints.contentMaxWidthFor(w));

    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: resolvedMax),
        child: child,
      ),
    );
  }
}

/* Simple responsive grid that picks column count from a callback. */
class ResponsiveGrid extends StatelessWidget {
  const ResponsiveGrid({
    super.key,
    required this.itemCount,
    required this.itemBuilder,
    required this.columnsFor,
    this.mainAxisSpacing = 12,
    this.crossAxisSpacing = 12,
    this.childAspectRatio = 1,
    this.mainAxisExtent,
    this.shrinkWrap = true,
    this.physics = const NeverScrollableScrollPhysics(),
    this.padding,
    this.minItemWidth,
  });

  final int itemCount;
  final IndexedWidgetBuilder itemBuilder;
  final int Function(AppWidthClass widthClass) columnsFor;
  final double mainAxisSpacing;
  final double crossAxisSpacing;
  final double childAspectRatio;
  final double? mainAxisExtent;
  final bool shrinkWrap;
  final ScrollPhysics? physics;
  final EdgeInsetsGeometry? padding;

  /* When set, columns are derived from available width instead of [columnsFor]. */
  final double? minItemWidth;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final cols = minItemWidth != null
            ? AppBreakpoints.columnsForWidth(
                constraints.maxWidth,
                minItemWidth: minItemWidth!,
                minColumns: 1,
                maxColumns: 8,
                spacing: crossAxisSpacing,
              )
            : columnsFor(context.widthClass).clamp(1, 12);
        return GridView.builder(
          shrinkWrap: shrinkWrap,
          physics: physics,
          padding: padding,
          itemCount: itemCount,
          gridDelegate: mainAxisExtent != null
              ? SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: cols,
                  mainAxisSpacing: mainAxisSpacing,
                  crossAxisSpacing: crossAxisSpacing,
                  mainAxisExtent: mainAxisExtent,
                )
              : SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: cols,
                  mainAxisSpacing: mainAxisSpacing,
                  crossAxisSpacing: crossAxisSpacing,
                  childAspectRatio: childAspectRatio,
                ),
          itemBuilder: itemBuilder,
        );
      },
    );
  }
}

/* Wraps form fields into 1–N columns based on width without changing field widgets. */
class ResponsiveFormColumns extends StatelessWidget {
  const ResponsiveFormColumns({
    super.key,
    required this.children,
    this.maxColumns = 2,
    this.spacing = 12,
    this.runSpacing = 12,
  });

  final List<Widget> children;
  final int maxColumns;
  final double spacing;
  final double runSpacing;

  @override
  Widget build(BuildContext context) {
    final cols = AppBreakpoints.formColumnsFor(
      context.widthClass,
      maxColumns: maxColumns,
    );
    if (cols <= 1 || children.length <= 1) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          for (var i = 0; i < children.length; i++) ...[
            if (i > 0) SizedBox(height: runSpacing),
            children[i],
          ],
        ],
      );
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final itemWidth =
            (constraints.maxWidth - spacing * (cols - 1)) / cols;
        return Wrap(
          spacing: spacing,
          runSpacing: runSpacing,
          children: [
            for (final child in children)
              SizedBox(width: itemWidth, child: child),
          ],
        );
      },
    );
  }
}

/* Two-column layout on large widths; stacks on compact/medium. */
class ResponsiveSplit extends StatelessWidget {
  const ResponsiveSplit({
    super.key,
    required this.primary,
    required this.secondary,
    this.breakpoint = AppWidthClass.large,
    this.primaryFlex = 1,
    this.secondaryFlex = 1,
    this.spacing = 16,
    this.crossAxisAlignment = CrossAxisAlignment.start,
  });

  final Widget primary;
  final Widget secondary;
  final AppWidthClass breakpoint;
  final int primaryFlex;
  final int secondaryFlex;
  final double spacing;
  final CrossAxisAlignment crossAxisAlignment;

  @override
  Widget build(BuildContext context) {
    final split = context.widthClass.index >= breakpoint.index;
    if (!split) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          primary,
          SizedBox(height: spacing),
          secondary,
        ],
      );
    }
    return Row(
      crossAxisAlignment: crossAxisAlignment,
      children: [
        Expanded(flex: primaryFlex, child: primary),
        SizedBox(width: spacing),
        Expanded(flex: secondaryFlex, child: secondary),
      ],
    );
  }
}
