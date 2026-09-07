# Phase 10 - Universal Printer

Upgrade existing printer code.

Support Bluetooth, WiFi, USB and Network.

Roles:
INVOICE, KOT, KITCHEN, BOT, BAR, TOKEN, REPORT.

Per printer:
Name, Connection, Identifier, Role, Paper Size, Copies, Auto Print, Auto Cutter, Cash Drawer, Language, Logo, QR, Preview.

Routing:
Food -> Kitchen
Beverage -> Bar
Invoice -> Invoice Printer
Mess Token -> Token Printer

Never crash on disconnected Bluetooth or null socket.
Show friendly device error and log technical details.

Cash Drawer and Auto Cutter must be configured per printer and document type.
