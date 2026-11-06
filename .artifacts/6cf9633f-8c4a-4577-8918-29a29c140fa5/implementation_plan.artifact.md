# Implement Delete History Feature

This plan outlines the steps to allow users to delete individual speed test results and clear the entire history.

## Proposed Changes

### Data Layer

#### [MODIFY] [TestResultDao.kt](file:///E:/My-Speed-Test-App/app/src/main/java/com/fdossena/speedtest/data/TestResultDao.kt)
- Add `@Delete` method to remove a `TestResult`.
- Add `@Query` method to delete all results: `DELETE FROM test_results`.

#### [MODIFY] [HistoryRepository.kt](file:///E:/My-Speed-Test-App/app/src/main/java/com/fdossena/speedtest/data/HistoryRepository.kt)
- Add `delete(testResult: TestResult)` method.
- Add `deleteAll()` method.

### ViewModel Layer

#### [MODIFY] [MainViewModel.kt](file:///E:/My-Speed-Test-App/app/src/main/java/com/fdossena/speedtest/ui/MainViewModel.kt)
- Add `deleteResult(result: TestResult)` function.
- Add `clearHistory()` function.

### UI Layer

#### [MODIFY] [SpeedtestApp.kt](file:///E:/My-Speed-Test-App/app/src/main/java/com/fdossena/speedtest/ui/SpeedtestApp.kt)
- Update `SpeedtestApp` parameters to include `onDeleteResult` and `onClearHistory`.
- Update `HistoryScreen` to:
    - Add a "Delete All" icon button in the top bar.
    - Add a trash icon button to each `HistoryItem` card.
    - (Optional) Add a confirmation dialog for "Delete All".

## Verification Plan

### Automated Tests
- Run existing build to ensure no regressions.
- (Optional) Add unit tests for `TestResultDao` deletion.

### Manual Verification
1. Open the app and run a speed test to ensure history is populated.
2. Go to the History screen.
3. Tap the delete icon on a specific result and verify it is removed.
4. Tap the "Clear All" icon in the header and verify all results are removed.
