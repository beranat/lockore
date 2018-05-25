  System requirements
=======================
* Java 1.8.0 Build 111 i386
* Oracle Java ME SDK 3.4
* NetBeans IDE 8.2

  Secure
==========
salt = PBKDF2(SHA-1, password)
AES-KEY = SHA-3 HMAC(password, salt) [000..256) 256 bits
IV-PARAM= SHA-3 HMAC(password, salt) [256..512) 256 bits
CHECK   = SHA-3 HMAC(password, salt) [496..512) 16 bits

SHA1 used only as hardware implemented (for Java ME) with acceptable speed for all weak phones.

  Themes
============================
Because for some phones a big difference between 'system like' and j2me controls - this section adds possibility to modify theme and j2me behavior for 'system like colors'.

The theme is string of a system colors' modification, where may defined up to 7 colors, separated by comma (,):
- Background
- Foreground
- Highlighted (Selected) item’s background
- Highlighted (Selected) item’s foreground
- Border
- Highlight border
- Default (for unexpected queries)

If some color is skipped - system color will be used instead.
Color saved in hex format Alpha Red Green Blue and if alpha is a zero then this color will not be drawn (full transparency).

  Themes location
-------------------
In file in /themes.txt "ThemeName=ColorsList".
In JAD-property 'Theme-Colors' - _only_ Colors List.

  Theme examples
-----------------
From a themes.txt file -
```
Light=00000000,,FF101010,FFFFFFFF,00000000,FF303030,FFB0B0B0
```
- don't draw background
- use system foreground (text) color
- dark highlighted background
- white highlight color
- no border for unselected item
- a small (a little more bright) border for cursor
- gray color for unexpected items (visible at any another theme colors)

From JAD-property -
```
00000000,FF3175E6,00000000,FF001B4D,00000000,,FFFFFFFF
```
- Don't draw background
- Light-blue for text
- Don't draw background for selected (allow system to draw it)
- Dark-blue for text
- No border for unselected
- Use system color for bordered selected item
- White for all other colors

  Custom icons
================
User may add custom icons to midlet for using in storages.
Icon must be added into jar file (it is a regular zip file) as:
* a single png file 'icon\_*id*.png' (any acceptable size)
* two png files: 'icon\_*id*-16.png' (16x16) and 'icon\_*id*-32.png' (32x32)

For adding user friendly name (instead icon\_id) should be added translation text into language file(s) as "icon\_*id*=Some name"

*NOTE* For resultion image application selects icon with nearest size and rescale it, so for preview images in list (size 12x12) the rescaled 16x16 will looks better then rescaled 32x32, and for big icon (36x36) - application uses rescaled 32x32 and at any way (12x12 and 36x36) the icon looks nice. 

  Translation and L10n
========================
Localization is carried out through a lang-file (l10n/_Language Code_.lang) in which for each line of the English language a string is defined in another language.

_Language Code_ is defined in RFC-1766 - primary code (ISO 639-1) and may be optional "-Subcode" (ISO 3166-1).
