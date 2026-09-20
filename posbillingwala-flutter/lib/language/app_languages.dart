import 'package:flutter/material.dart';

/* Supported app UI languages (assets/locale/{code}.json). */
class AppLanguageOption {
  const AppLanguageOption({
    required this.code,
    required this.label,
    required this.nativeLabel,
  });

  final String code;
  final String label;
  final String nativeLabel;

  String get displayLabel =>
      label == nativeLabel ? label : '$label ($nativeLabel)';
}

abstract final class AppLanguages {
  AppLanguages._();

  static const options = <AppLanguageOption>[
    AppLanguageOption(code: 'en', label: 'English', nativeLabel: 'English'),
    AppLanguageOption(code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी'),
    AppLanguageOption(code: 'mr', label: 'Marathi', nativeLabel: 'मराठी'),
    AppLanguageOption(code: 'gu', label: 'Gujarati', nativeLabel: 'ગુજરાતી'),
    AppLanguageOption(code: 'bn', label: 'Bengali', nativeLabel: 'বাংলা'),
    AppLanguageOption(code: 'ta', label: 'Tamil', nativeLabel: 'தமிழ்'),
    AppLanguageOption(code: 'te', label: 'Telugu', nativeLabel: 'తెలుగు'),
    AppLanguageOption(code: 'kn', label: 'Kannada', nativeLabel: 'ಕನ್ನಡ'),
  ];

  static const supportedCodes = ['en', 'hi', 'mr', 'gu', 'bn', 'ta', 'te', 'kn'];

  static Locale localeFromCode(String? code) {
    final normalized = (code ?? 'en').trim().toLowerCase();
    if (supportedCodes.contains(normalized)) {
      return Locale(normalized);
    }
    return const Locale('en');
  }

  static bool isSupported(String? code) =>
      supportedCodes.contains((code ?? '').trim().toLowerCase());
}
