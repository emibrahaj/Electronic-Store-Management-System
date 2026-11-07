# StorePOS

JavaFX point of sale and store management application with SQLite transactions, stock history, employee roles, and CSV reporting.

## Run from source (Windows)

Install JDK 21, then run from the repository root:

```powershell
.\build.ps1 -Run
```

On a fresh checkout, the build downloads pinned JavaFX 21 and SQLite JDBC dependencies from Maven Central into `.build/`. You can download them separately with `.\setup.ps1`. If you have a JavaFX 21 SDK, pass `-JavaFxLib "C:\path\to\javafx-sdk-21\lib"` or set `JAVAFX_LIB`.

With Maven installed, import `Store/pom.xml` into your IDE or run:

```powershell
cd Store
mvn compile javafx:run
```

The legacy Eclipse `.classpath` expects a user library named `javafx21`. Maven import is recommended because it also resolves SQLite JDBC.

## Login and first-run setup

Sign in using **username and password only**. The application determines the role from the stored account. Existing accounts remain usable; email, phone, and role are no longer login fields.

A new store opens an administrator setup screen. Choose your own username, password, name, email, and phone number. No default accounts are created. Sign in after setup and use the administrator dashboard to create managers and cashiers.

An administrator can add, edit, and delete employees. Leave the password blank when editing to preserve it. The application prevents deletion or demotion of the last administrator and prevents changing the active administrator's username or role. Employee operations and checkout/inventory mutations enforce role permissions.

New passwords are salted PBKDF2 hashes. Legacy plaintext passwords remain accepted for compatibility; after compiling, you can migrate them with:

```powershell
java "-Dstore.home=$PWD/Store" -cp .build/classes DAO.PasswordMigrationUtil
```

This preserves existing hashes and creates `passwords.txt.backup` once. Keep credentials and that backup private.

## Checkout

1. Click a product tile to add one unit. Search by product name/category, scan a barcode, or enter a SKU in the single search field. Press Enter to add an exact match or the only search result. Category filters narrow the catalog; Ctrl+F focuses search.
2. Change quantities with the inline minus/plus buttons. Remove deletes a line. Tiles show stock remaining after the current order, including low-stock and unavailable states.
3. Complete Sale shows the order total and stays disabled while the order is empty. It records the sale and reduces stock in **one SQLite transaction**. A failed write rolls back stock, bill history, and stock movements; the order stays available for retry.
4. Completion shows a brief banner and clears the order for the next customer. Cancel Order discards the draft after confirmation; leaving an active order through Log Out also asks for confirmation.
5. Sales Today shows completed sales and the Process Return action. Returns restore the original product's stock and reduce the net bill value in one transaction.

Receipts are text files. Prices and purchase costs are snapshots, so later product-price changes do not change historical sale values. Monetary calculations use `BigDecimal`; SQLite stores integer cents. Price inputs accept at most two decimal places.

## Products and stock

Managers add products and suppliers, edit prices/stock/barcodes, add stock, and remove products. Every product has a permanent generated SKU and an optional unique barcode. Barcodes accept up to 64 letters, digits, underscores, or hyphens. Keyboard-style barcode scanners work with the checkout scan field.

Stock movements record the SKU, product, quantity change, timestamp, operation reason, and signed-in username. Sales and returns include the bill number in the reason. Only one application process may open a given store at a time, preventing competing windows from overwriting each other's state.

## Reports and exports

Managers open View Statistics; administrators open View Reports, then Open Sales & Inventory Reports.

- Choose inclusive start/end dates to see net sales, unit counts, revenue, and profit.
- Sales lists each bill and its net value after returns.
- Best Sellers aggregates net units, revenue, and profit by SKU.
- Low Stock shows current products with five or fewer units; it is independent of the chosen dates.
- Stock History shows movements in the selected dates, including reasons and users.
- Export Selected Report saves the active table as UTF-8 CSV, with escaped fields suitable for spreadsheet use.

Reports attribute a returned sale's revised net value to the original sale date. Profit is selling price minus purchase cost; it does not include tax, overhead, or fees.

## Data, migration, and backups

Source runs store data under `Store/src/DAO/`:

- `store.db`: SQLite products, bills, line items, suppliers, stock movements, and migration metadata.
- `passwords.txt`: employee accounts.
- `audit.log`: application audit log.
- `store.lock`: process lock; the file can remain after exit, while the operating-system lock is released.

Receipts are under `Store/src/Database/PrintableBills/`. Override the store location when launching Java with `-Dstore.home=/absolute/path/to/StoreHome`.

On first database access, the app imports existing `inventory.dat`, `bills.dat`, and `suppliers.dat` together in a transaction. Duplicate legacy supplier-product links are deduplicated. The originals remain unchanged, and completed migration is recorded so restarting does not import them again. An invalid legacy file cancels migration rather than silently discarding records. Already-corrupted historical prices or quantities cannot be reconstructed automatically.

Packaged applications use `%USERPROFILE%\.storepos` by default. To move an existing store, close the application and copy its `src/DAO` and receipt directories into that store home. Copy old `.dat` files before the packaged app's first run if you want them imported. If its database has already been initialized, use a fresh store home instead of replacing records in a live database.

Back up `store.db`, `passwords.txt`, and receipts while the app is closed. The app is intended for one local store process, not simultaneous clients or shared network drives.

## Build the bundled application and installer

```powershell
.\package.ps1            # Self-contained app folder
.\package.ps1 -Installer # App folder plus Windows MSI installer
```

Output goes into a timestamped folder under `dist/`. Run `StorePOS/StorePOS.exe` directly, or install the MSI. Both include the Java runtime, JavaFX, and SQLite; end users do not need a JDK or environment variables. The MSI is per-user and creates menu/desktop shortcuts. The app does not include development data or credentials.

The packaging script uses JDK 21 `jpackage`. For MSI output, it downloads WiX build tools into `.build/tools` if they are unavailable. Nothing is installed globally by the build. Packages are unsigned. Build Windows packages on Windows; other platforms need their own build and matching JavaFX libraries.

## Tests and CI

```powershell
.\build.ps1 -Test -UiTest
```

The standalone suite checks setup/authentication, role restrictions, employee persistence, exact totals, cart edits/cancellation, barcode lookup, refunds, reporting, CSV escaping, and failed-write rollback. A separate test imports isolated copies of the repository's original serialized data. JavaFX smoke checks open the main screens and save previews under `.build/ui-preview/`. All tests use temporary store homes, never the real store's records.

`.github/workflows/ci.yml` runs these checks on Windows for pushes and pull requests and uploads screen previews. The suite uses standalone Java runners, not JUnit; `mvn test` is not a substitute for the command above.

## Layout

- `Store/src/Controller`: operations and login routing.
- `Store/src/DAO`: SQLite schema, transactions, legacy import, passwords, audit logging.
- `Store/src/Model`: products, inventory, users, suppliers, bills.
- `Store/src/Util`: money, session permissions, reporting, navigation.
- `Store/src/View`: JavaFX screens and shared theme.
- `Store/test`: regression, migration, and UI checks.
- `setup.ps1`, `build.ps1`, `package.ps1`: dependencies, validation, distribution.

## Build references

- [Oracle jpackage documentation](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc)
- [WiX build tools](https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm)

Developed at Epoka University by Emi Brahaj, Era Malaj, and Erla Luzi.
