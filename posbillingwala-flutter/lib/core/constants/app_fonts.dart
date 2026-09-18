import 'package:flutter/painting.dart';

/* Bundled Poppins family (`assets/font`) — clean, readable for POS UI. */
/* Receipt print/preview uses [printFallbacks] so user-entered product / */
/* customer / shop text in Marathi, Hindi, Tamil, etc. still glyphs correctly. */
/* (App UI language is separate — see LocaleCatalog / AppStrings.) */
abstract final class AppFonts {
  static const family = 'Poppins';

  /* System / platform fonts that cover Indic + common scripts. */
  static const printFallbacks = <String>[
    'Noto Sans Devanagari',
    'Noto Sans',
    'Roboto',
    'Helvetica',
    'Arial Unicode MS',
    'Arial',
    'sans-serif',
  ];

  /* Same style for thermal raster, invoice preview, and share PNG. */
  static TextStyle printBody({
    double fontSize = 22,
    FontWeight weight = FontWeight.w500,
    double height = 1.25,
    Color color = const Color(0xFF000000),
  }) => TextStyle(
    color: color,
    fontFamily: family,
    fontFamilyFallback: printFallbacks,
    fontSize: fontSize,
    height: height,
    fontWeight: weight,
  );

  static TextStyle printBold({
    double fontSize = 22,
    double height = 1.25,
    Color color = const Color(0xFF000000),
  }) => printBody(
    fontSize: fontSize,
    height: height,
    color: color,
    weight: FontWeight.w700,
  );
}
