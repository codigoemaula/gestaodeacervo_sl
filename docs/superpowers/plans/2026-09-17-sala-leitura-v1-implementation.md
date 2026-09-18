# Sala de Leitura Android V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar um aplicativo Android offline-first para gestão de acervo, pessoas, empréstimos, devoluções, renovações, inventário, leitura de códigos e backup das Salas de Leitura da SME-SP.

**Architecture:** Android nativo em Kotlin, interface Jetpack Compose, Room/SQLite como fonte local de verdade e serviços externos isolados atrás de interfaces opcionais. Regras de circulação e inventário ficam no domínio, independentes da UI; câmera, consulta bibliográfica e backup são adaptadores que podem falhar sem impedir a operação offline.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose/Material 3, Room, Coroutines/Flow, Navigation Compose, CameraX, ML Kit Barcode Scanning bundled, Retrofit/OkHttp apenas para metadados online, kotlinx.serialization, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-17-sala-leitura-android-design.md`

## Global Constraints

- Android nativo em Kotlin com Jetpack Compose.
- Persistência local obrigatória em Room/SQLite.
- Operação offline integral para cadastro, consulta, empréstimo, devolução, renovação, inventário, relatórios, QR e backup.
- Uma instalação representa uma escola; dados não se misturam entre escolas/backups.
- Prazo de empréstimo permitido somente em 7 ou 14 dias.
- CameraX + ML Kit Barcode Scanning com modelo incorporado ao APK.
- Sem Firebase e sem login externo na V1.
- Open Library é a fonte primária opcional de metadados; Google Books é fallback.
- Minimizar dados pessoais: sem CPF, endereço ou telefone.
- Backup explícito pelo seletor de documentos; nada é enviado automaticamente à nuvem.
- Estados de exemplar: AVAILABLE, LOANED, DAMAGED, LOST, MAINTENANCE, WITHDRAWN.

---

## File Structure

- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` — configuração do build.
- `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml` — módulo Android e permissões.
- `app/src/main/java/br/gov/sp/sme/salaleitura/MainActivity.kt` — activity única Compose.
- `.../core/model/*` — enums e modelos de domínio puros.
- `.../core/logic/*` — regras de ISBN, prazos, circulação e inventário.
- `.../data/local/*` — Room database, entidades, DAOs, migrations.
- `.../data/repository/*` — repositórios locais e orquestração transacional.
- `.../data/remote/*` — clientes Open Library/Google Books/ISBN ranges.
- `.../feature/setup/*` — configuração da escola.
- `.../feature/dashboard/*` — indicadores.
- `.../feature/catalog/*` — obras e exemplares.
- `.../feature/people/*` — pessoas/turmas e CSV.
- `.../feature/circulation/*` — empréstimo, devolução, renovação e pendências.
- `.../feature/inventory/*` — sessões e conferência.
- `.../feature/scanner/*` — câmera/ML Kit.
- `.../feature/backup/*` — exportação/restauração `.slbackup`.
- `app/src/test/...` — testes unitários de domínio/repositórios.

---

### Task 1: Bootstrap do projeto e domínio de circulação

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/br/gov/sp/sme/salaleitura/core/model/CirculationModels.kt`
- Create: `app/src/main/java/br/gov/sp/sme/salaleitura/core/logic/LoanPolicy.kt`
- Test: `app/src/test/java/br/gov/sp/sme/salaleitura/core/logic/LoanPolicyTest.kt`

**Interfaces:**
- Produces: `enum class LoanPeriod(val days: Long) { SEVEN(7), FOURTEEN(14) }`
- Produces: `fun dueDate(start: LocalDate, period: LoanPeriod): LocalDate`
- Produces: `fun canLoan(personActive: Boolean, copyStatus: CopyStatus): Boolean`

- [ ] **Step 1: Write failing tests** for 7/14-day due dates and blocking inactive people/non-available copies.
- [ ] **Step 2: Run `./gradlew testDebugUnitTest`** and confirm the tests fail before implementation.
- [ ] **Step 3: Implement minimal domain code** with `LoanPeriod`, `CopyStatus`, `PersonType`, and `LoanPolicy`.
- [ ] **Step 4: Run unit tests** and confirm pass.
- [ ] **Step 5: Commit** `feat: bootstrap android project and circulation domain`.

### Task 2: Room schema and DAOs

**Files:**
- Create: `.../data/local/entity/Entities.kt`
- Create: `.../data/local/dao/SchoolDao.kt`
- Create: `.../data/local/dao/CatalogDao.kt`
- Create: `.../data/local/dao/PeopleDao.kt`
- Create: `.../data/local/dao/LoanDao.kt`
- Create: `.../data/local/dao/InventoryDao.kt`
- Create: `.../data/local/AppDatabase.kt`
- Create: `.../data/local/Converters.kt`
- Test: `.../data/local/SchemaMappingTest.kt`

**Interfaces:**
- Produces entities `SchoolEntity`, `ClassGroupEntity`, `PersonEntity`, `BookEditionEntity`, `BookCopyEntity`, `LoanEntity`, `LoanRenewalEntity`, `InventorySessionEntity`, `InventoryItemEntity`, `AuditLogEntity`, `AppSettingsEntity`.
- `LoanEntity.loanPeriodDays` must be exactly 7 or 14.

- [ ] **Step 1:** Write mapping/constraint tests around statuses and loan periods.
- [ ] **Step 2:** Run tests and verify failure.
- [ ] **Step 3:** Implement Room entities, indices, foreign keys, DAOs and database version 1.
- [ ] **Step 4:** Run tests.
- [ ] **Step 5:** Commit `feat: add local room schema`.

### Task 3: Repositories, book-copy generation and circulation transactions

**Files:**
- Create: `.../data/repository/CatalogRepository.kt`
- Create: `.../data/repository/PeopleRepository.kt`
- Create: `.../data/repository/CirculationRepository.kt`
- Create: `.../data/repository/InventoryRepository.kt`
- Create: `.../core/logic/CopyCodeGenerator.kt`
- Test: `.../core/logic/CopyCodeGeneratorTest.kt`
- Test: `.../data/repository/CirculationRulesTest.kt`

**Interfaces:**
- Produces: `fun generateCopyCode(prefix: String, editionId: Long, sequence: Int): String`.
- Produces repository operations `checkout(personId, copyId, period, now)`, `returnCopy(copyId, returnStatus, now)`, `renew(loanId, period, now)`.

- [ ] **Step 1:** Write tests for deterministic unique copy codes and circulation rules.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement repositories with Room transactions and audit-log insertion.
- [ ] **Step 4:** Verify tests.
- [ ] **Step 5:** Commit `feat: implement catalog and circulation repositories`.

### Task 4: ISBN normalization and optional bibliographic metadata

**Files:**
- Create: `.../core/logic/Isbn.kt`
- Create: `.../data/remote/BibliographicService.kt`
- Create: `.../data/remote/OpenLibraryService.kt`
- Create: `.../data/remote/GoogleBooksService.kt`
- Create: `.../data/repository/MetadataRepository.kt`
- Test: `.../core/logic/IsbnTest.kt`

**Interfaces:**
- Produces: `fun normalizeIsbn(raw: String): IsbnResult` validating ISBN-10/13 check digits.
- Produces: `suspend fun lookup(isbn13: String): BookMetadata?` that never blocks local catalog access.

- [ ] **Step 1:** Write valid/invalid ISBN-10 and ISBN-13 tests.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement check-digit logic and remote adapters with timeouts/fallback.
- [ ] **Step 4:** Verify tests.
- [ ] **Step 5:** Commit `feat: add isbn validation and metadata lookup`.

### Task 5: Compose shell, setup and dashboard

**Files:**
- Create: `.../MainActivity.kt`
- Create: `.../ui/App.kt`
- Create: `.../ui/navigation/AppNav.kt`
- Create: `.../ui/theme/*`
- Create: `.../feature/setup/SchoolSetupScreen.kt`
- Create: `.../feature/setup/SchoolSetupViewModel.kt`
- Create: `.../feature/dashboard/DashboardScreen.kt`
- Create: `.../feature/dashboard/DashboardViewModel.kt`

**Interfaces:**
- Setup validates default period as `LoanPeriod.SEVEN` or `LoanPeriod.FOURTEEN`.
- Dashboard exposes counts for editions, copies, available, active loans, overdue, damaged/lost and last inventory.

- [ ] **Step 1:** Add ViewModel state tests for valid school setup and dashboard aggregates.
- [ ] **Step 2:** Verify failure.
- [ ] **Step 3:** Implement Compose navigation, setup and dashboard.
- [ ] **Step 4:** Run unit tests and lint where available.
- [ ] **Step 5:** Commit `feat: add setup and dashboard ui`.

### Task 6: Catálogo, pessoas, turmas e importação CSV

**Files:**
- Create: `.../feature/catalog/CatalogScreen.kt`
- Create: `.../feature/catalog/BookFormScreen.kt`
- Create: `.../feature/catalog/CatalogViewModel.kt`
- Create: `.../feature/people/PeopleScreen.kt`
- Create: `.../feature/people/PersonFormScreen.kt`
- Create: `.../feature/people/ClassGroupScreen.kt`
- Create: `.../feature/people/CsvImporter.kt`
- Test: `.../feature/people/CsvImporterTest.kt`

**Interfaces:**
- CSV accepts headers `nome,tipo,identificador,turma,ano,turno,funcao` and returns preview rows plus validation errors before persistence.
- Batch catalog registration creates N independent `BookCopyEntity` records.

- [ ] **Step 1:** Write CSV parsing/validation tests and batch-copy tests.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement screens, forms, filtering, batch quantity and CSV preview/import.
- [ ] **Step 4:** Verify tests.
- [ ] **Step 5:** Commit `feat: add catalog people classes and csv import`.

### Task 7: Empréstimos, devoluções, renovações e pendências

**Files:**
- Create: `.../feature/circulation/CheckoutScreen.kt`
- Create: `.../feature/circulation/ReturnScreen.kt`
- Create: `.../feature/circulation/LoansScreen.kt`
- Create: `.../feature/circulation/CirculationViewModel.kt`
- Test: `.../feature/circulation/CirculationViewModelTest.kt`

**Interfaces:**
- Checkout defaults to school setting but exposes explicit 7/14 toggle before confirmation.
- Return identifies active loan from copy code.
- Renewal records `LoanRenewalEntity` and recomputes due date from renewal date.

- [ ] **Step 1:** Write ViewModel tests for 7/14 toggle, due dates, blocked checkout, return and renewal.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement circulation UI/state/actions.
- [ ] **Step 4:** Verify tests.
- [ ] **Step 5:** Commit `feat: add circulation screens`.

### Task 8: Scanner offline com CameraX + ML Kit

**Files:**
- Create: `.../feature/scanner/BarcodeScannerScreen.kt`
- Create: `.../feature/scanner/BarcodeAnalyzer.kt`
- Create: `.../feature/scanner/ScanRouter.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `.../feature/scanner/ScanRouterTest.kt`

**Interfaces:**
- Scanner recognizes ISBN/EAN and internal QR payload `SL:<copyCode>`/`PERSON:<personCode>`.
- `ScanRouter` routes scan results without requiring network.

- [ ] **Step 1:** Write scan-routing tests.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement permission handling, CameraX preview and bundled ML Kit analyzer.
- [ ] **Step 4:** Verify tests and compile manifest.
- [ ] **Step 5:** Commit `feat: add offline barcode scanner`.

### Task 9: Inventário e relatórios locais

**Files:**
- Create: `.../feature/inventory/InventoryScreen.kt`
- Create: `.../feature/inventory/InventoryViewModel.kt`
- Create: `.../feature/reports/ReportsScreen.kt`
- Create: `.../core/logic/InventorySummary.kt`
- Test: `.../core/logic/InventorySummaryTest.kt`

**Interfaces:**
- Summary exposes `registered`, `found`, `loaned`, `missing`, `damaged`, `withdrawn`.
- Scanning the same copy twice in one inventory is idempotent.

- [ ] **Step 1:** Write inventory summary/idempotency tests.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement sessions, scanning, close-session summary and local reports.
- [ ] **Step 4:** Verify tests.
- [ ] **Step 5:** Commit `feat: add inventory and reports`.

### Task 10: Backup/restauração, labels and release verification

**Files:**
- Create: `.../feature/backup/BackupManager.kt`
- Create: `.../feature/backup/BackupScreen.kt`
- Create: `.../feature/labels/LabelGenerator.kt`
- Create: `.../feature/settings/SettingsScreen.kt`
- Test: `.../feature/backup/BackupManagerTest.kt`
- Create: `README.md`

**Interfaces:**
- `.slbackup` contains manifest with schema version, school identity, timestamp, SHA-256 and serialized export payload/database snapshot.
- Restore rejects incompatible schema/corrupt checksum before replacing active data.
- QR labels encode stable internal copy/person identifiers.

- [ ] **Step 1:** Write backup manifest/checksum tests.
- [ ] **Step 2:** Verify failures.
- [ ] **Step 3:** Implement export/import via Storage Access Framework, settings, labels and README.
- [ ] **Step 4:** Run `./gradlew testDebugUnitTest lintDebug assembleDebug`; install or inspect APK if SDK/device support exists.
- [ ] **Step 5:** Commit `feat: complete v1 backup labels and release build`.

