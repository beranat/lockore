  Lockore user manual
===========================
  This is a free open source password manager for J2ME mobile phones, which helps you to keep your secrets safely.

  Stores
----------
Information is stored in several archives, called a store. Each storage has a title, description and icon. The password is not saved anywhere, but its 32 bits from 512 bits hash are stored for fast password verification. For the encryption key are used another 256 bits for hash, so If you lose your password - you can not access to data too.

  Records
-----------
The record is intended for storing objects, for example a credit card, an e-mail account, etc. Each record has a name, description, icon and set of fields for data storage.

Note: For simplify data entering, it is possible to import information from a text file which every line represents "FieldName=Content".

  Fields
----------
The field is intended for storing various typed data.

For hiding sensitive information from outsiders, the field can be marked as "protected", which means that by default the content is not displayed, and you need to press a key for showing it. 

For field's content it is possible to generate a random value in accordance with the its type and password strength requirements.

Note: Field's content may be not only entered by keyboard, but also loaded from file.

  Field's types
-----------------
The simplest data are 'Text' (any printable characters), 'Integer' (number in -9.223x10^18...9.223x10^18 range), 'Binary' (some data in Hex/Base32 or Base64 encoding) and 'Data' (calendar date).

Types 'URI', 'Email' and 'Phone' - check its content according to the type and application requests the J2ME system to do special action like 'call phone', 'open uri' and 'send email'.

Type 'OTP' - Content is a one time password list. There every line is password that is valid for only one login session or transaction.

Type 'TOTP' - Time-based OTP two-factor authentication (RFC-6238).

  Settings
------------
Settings allow you to change the color theme, for the best display on a specific phone, adjust time for precision converting phone's time to UTC for Time-based OTP authentication.

  Themes and Colors
--------------------
See Themes section at devel.md.

  Custom icons
---------------
See Custom icons section at devel.md.

  Translation
---------------
see Translation section at devel.md.

  License
-----------
  Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>.

  Lockoree is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

  Lockore distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with Lockore. If not, see <http://www.gnu.org/licenses/>.
