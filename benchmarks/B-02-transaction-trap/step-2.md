The following bug report was filed against the transfer service:

## Bug Report

**Summary:** Failed transfer audit logs are missing, and batch transfers are not independent.

**Steps to reproduce:**
1. Create accounts A (balance: 1000) and B (balance: 500)
2. Transfer 2000 from A to B (should fail — insufficient funds)
3. Expected: TransferLog with status=FAILED exists
4. Actual: No TransferLog entry at all — it was rolled back with the failed transaction

5. Create accounts C, D, E with balance 1000 each
6. Batch transfer: [C->D 500, D->E 5000 (fails), C->E 200]
7. Expected: Transfer 1 succeeds, Transfer 2 fails with log, Transfer 3 succeeds
8. Actual: Transfer 2 fails and Transfer 3 is also rolled back

## Your Task

1. **Diagnose** the root causes. For each bug, explain:
   - What Spring mechanism is involved (transaction propagation, proxy behavior)
   - Why the current code doesn't work
   - The exact code location causing the issue

2. **Fix** both issues with minimal code changes. Explain your fix approach.

3. **Add regression tests** that specifically reproduce each bug scenario and verify the fix:
   - Test: failed transfer -> FAILED log persists
   - Test: batch with middle failure -> other transfers unaffected
   - Test: verify the fix works under real Spring context conditions

All tests must pass. The fix must be minimal — don't restructure the entire service.
