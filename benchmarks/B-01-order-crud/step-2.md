Extend the order service with the following:

## 1. Nullable vs. Absent Field

Add a `discount: BigDecimal?` field to OrderItem.
- Absent in JSON → no discount (field not set)
- Present as `null` → explicitly no discount
- Present with value → apply discount
Track the three states. Use Jackson configuration to distinguish null from absent.

## 2. Polymorphic Payment Info

Add a `paymentInfo` field to Order. `PaymentInfo` is a Kotlin sealed class:
- `CreditCard(last4: String, brand: String)`
- `BankTransfer(iban: String)`

Serialize with a `"type"` discriminator field using `@JsonTypeInfo` and `@JsonSubTypes`.

## 3. Receipt Endpoint

Add `GET /api/orders/{id}/receipt` — returns:
```json
{
  "orderId": 1,
  "customerEmail": "...",
  "items": [{"productName": "...", "quantity": 2, "unitPrice": 10.00, "discount": 1.00, "lineTotal": 19.00}],
  "subtotal": 19.00,
  "total": 19.00,
  "paymentInfo": {"type": "credit_card", "last4": "4242", "brand": "VISA"},
  "issuedAt": "2024-01-15T10:30:00Z"
}
```

## 4. Date/Time

All date/time fields serialized as ISO 8601 with UTC timezone.

## 5. Tests

Add tests for ALL new functionality:
- Serialization round-trip tests for each PaymentInfo subtype
- Test: absent discount vs null discount vs present discount
- Test: receipt calculation correctness
- Test: dates are ISO 8601 format in JSON response

All existing and new tests must pass.
