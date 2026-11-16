package com.sgp.cliente.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Official J D FIBRA brand palette.
val BrandNavy = Color(0xFF061A4A)     // azul-marinho — identidade/fundos principais
val BrandRoyal = Color(0xFF005BFF)    // azul royal — destaques, botões, títulos
val BrandElectric = Color(0xFF008CFF) // azul elétrico — luzes, contornos, efeitos
val BrandCyan = Color(0xFF00D9FF)     // ciano — detalhes, ícones, chamadas
val BrandSky = Color(0xFF7ADFFF)      // azul-claro — degradês, elementos secundários
val BrandInkDark = Color(0xFF050B18)  // preto azulado — sombras, fundos escuros

// Tonal ramp built on the official hues (dark → light).
internal val Brand10 = BrandInkDark
internal val Brand20 = BrandNavy
internal val Brand30 = Color(0xFF033EAE)
internal val Brand40 = BrandRoyal
internal val Brand50 = BrandElectric
internal val Brand80 = BrandSky
internal val Brand90 = Color(0xFFC9F0FF)
internal val Brand95 = Color(0xFFEAF9FF)
internal val BrandInk = BrandNavy

internal val Slate05 = Color(0xFF060B10)
internal val Slate10 = Color(0xFF0D151C)
internal val Slate15 = Color(0xFF131D24)
internal val Slate20 = Color(0xFF1B2730)
internal val Slate25 = Color(0xFF222E38)
internal val Slate40 = Color(0xFF5B707C)
internal val Slate60 = Color(0xFF8899A5)
internal val Slate90 = Color(0xFFE5EAEE)
internal val Slate95 = Color(0xFFEFF3F6)
internal val Slate97 = Color(0xFFF4F7F9)
internal val Slate99 = Color(0xFFFAFCFE)

internal val Error40 = Color(0xFFBA1A1A)
internal val Error80 = Color(0xFFFFB4AB)
internal val Error90 = Color(0xFFFFDAD6)

val BrandDeep = BrandNavy

val StatusPaid = Color(0xFF1E7F4F)
val StatusPaidContainer = Color(0xFFBDEDD2)
val StatusOpen = BrandRoyal
val StatusOpenContainer = Brand95
val StatusOverdue = Color(0xFFB3261E)
val StatusOverdueContainer = Color(0xFFF9DEDC)
val StatusCancelled = Color(0xFF5F6368)
val StatusCancelledContainer = Color(0xFFE3E5E8)
