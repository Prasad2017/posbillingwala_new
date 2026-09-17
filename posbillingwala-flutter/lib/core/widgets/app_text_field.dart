import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:pos_billingwala_v2/core/widgets/app_svg.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_strings.dart';
import 'package:pos_billingwala_v2/core/widgets/widget_theme.dart';

class AppTextField extends StatefulWidget {
  const AppTextField({
    super.key,
    this.controller,
    this.label,
    this.hint,
    this.helperText,
    this.prefixIcon,
    this.prefixSvg,
    this.prefixText,
    this.validator,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.minLines,
    this.maxLines = 1,
    this.maxLength,
    this.showCounter = true,
    this.inputFormatters,
    this.onChanged,
    this.onSubmitted,
    this.readOnly = false,
    this.enabled = true,
    this.autofocus = false,
    this.focusNode,
    this.textCapitalization = TextCapitalization.none,
    this.textAlign = TextAlign.start,
  });

  final TextEditingController? controller;
  final String? label;
  final String? hint;
  final String? helperText;
  final IconData? prefixIcon;
  /* Optional SVG asset path (preferred over [prefixIcon] when set). */
  final String? prefixSvg;
  final String? prefixText;
  final String? Function(String?)? validator;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final int? minLines;
  final int maxLines;
  final int? maxLength;
  final bool showCounter;
  final List<TextInputFormatter>? inputFormatters;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final bool readOnly;
  final bool enabled;
  final bool autofocus;
  final FocusNode? focusNode;
  final TextCapitalization textCapitalization;
  final TextAlign textAlign;

  @override
  State<AppTextField> createState() => AppTextFieldState();
}

class AppTextFieldState extends State<AppTextField> {
  late bool obscure;
  /* Always own the field controller so parent dispose-during-route-pop
   * cannot crash TextFormField with "used after being disposed". */
  late final TextEditingController owned;
  VoidCallback? externalListener;
  TextEditingController? attachedExternal;
  var syncing = false;

  @override
  void initState() {
    super.initState();
    obscure = widget.obscureText;
    owned = TextEditingController(text: widget.controller?.text ?? '');
    owned.addListener(onOwnedChanged);
    attachExternal(widget.controller);
  }

  @override
  void didUpdateWidget(covariant AppTextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.obscureText != widget.obscureText) {
      obscure = widget.obscureText;
    }
    if (oldWidget.controller != widget.controller) {
      attachExternal(widget.controller);
      final next = widget.controller?.text;
      if (next != null && next != owned.text) {
        syncing = true;
        owned.value = widget.controller!.value;
        syncing = false;
      }
    }
  }

  @override
  void dispose() {
    detachExternal();
    owned.removeListener(onOwnedChanged);
    owned.dispose();
    super.dispose();
  }

  void attachExternal(TextEditingController? external) {
    detachExternal();
    if (external == null) return;
    attachedExternal = external;
    externalListener = () {
      if (!mounted || syncing) return;
      if (owned.value == external.value) return;
      syncing = true;
      owned.value = external.value;
      syncing = false;
    };
    external.addListener(externalListener!);
  }

  void detachExternal() {
    final external = attachedExternal;
    final listener = externalListener;
    attachedExternal = null;
    externalListener = null;
    if (external == null || listener == null) return;
    try {
      external.removeListener(listener);
    } catch (_) {
      /* External may already be disposed by the parent. */
    }
  }

  void onOwnedChanged() {
    if (syncing) return;
    final external = attachedExternal;
    if (external != null) {
      try {
        if (external.value != owned.value) {
          syncing = true;
          external.value = owned.value;
          syncing = false;
        }
      } catch (_) {
        /* Parent disposed the external controller — keep typing locally. */
        detachExternal();
      }
    }
    widget.onChanged?.call(owned.text);
  }

  @override
  Widget build(BuildContext context) {
    final prefixColor = context.textPrimary;
    final hint = widget.hint ??
        (widget.label != null
            ? WidgetStrings.enterFieldHint(widget.label!)
            : null);

    return TextFormField(
      controller: owned,
      focusNode: widget.focusNode,
      validator: widget.validator,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      obscureText: obscure,
      keyboardType: widget.keyboardType,
      textInputAction: widget.textInputAction,
      textCapitalization: widget.textCapitalization,
      textAlign: widget.textAlign,
      minLines: widget.minLines,
      maxLines: widget.maxLines,
      maxLength: widget.maxLength,
      autofocus: widget.autofocus,
      enabled: widget.enabled,
      textAlignVertical: widget.maxLines > 1
          ? TextAlignVertical.top
          : TextAlignVertical.center,
      buildCounter: widget.showCounter
          ? null
          : (
              BuildContext context, {
              required int currentLength,
              required bool isFocused,
              required int? maxLength,
            }) =>
              null,
      inputFormatters: widget.inputFormatters,
      onFieldSubmitted: widget.onSubmitted,
      readOnly: widget.readOnly,
      style: TextStyle(color: context.textPrimary),
      decoration: InputDecoration(
        labelText: widget.label,
        hintText: hint,
        floatingLabelBehavior: FloatingLabelBehavior.auto,
        helperText: widget.helperText,
        alignLabelWithHint: widget.maxLines > 1,
        prefixIcon: widget.prefixSvg != null
            ? Padding(
                padding: const EdgeInsets.all(12),
                child: AppSvg(
                  widget.prefixSvg!,
                  width: 20,
                  height: 20,
                  color: prefixColor,
                ),
              )
            : widget.prefixIcon != null
                ? Icon(widget.prefixIcon, size: 20, color: prefixColor)
                : null,
        prefixText: widget.prefixText,
        prefixStyle: TextStyle(
          color: prefixColor,
          fontSize: 16,
          fontWeight: FontWeight.w500,
        ),
        iconColor: prefixColor,
        suffixIcon: widget.obscureText
            ? IconButton(
                icon: Icon(
                  obscure ? Icons.visibility_off : Icons.visibility,
                  size: 20,
                ),
                onPressed: () => setState(() => obscure = !obscure),
              )
            : null,
      ),
    );
  }
}
