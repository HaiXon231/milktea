---
status: resolved
trigger: "hiện tại khi tôi bảo muốn thêm trân châu đen size M (topping) thì nó lại đi thêm trà sữa, khi tôi muốn xóa 1 món đã order trước dó \"tôi muốn hủy trà xoài\" thì lại nhận được \"Lỗi: không tìm thấy món nào giống 'tra xoai'. Con nhắn lại tên món giúp mẹ nha!\""
---

## Symptoms
- **Expected behavior**: 
  1. Adding "trân châu đen size M (topping)" should add the topping itself, not default to milk tea.
  2. "tôi muốn hủy trà xoài" should remove the "trà xoài" item from the cart.
- **Actual behavior**:
  1. Adding "trân châu đen" adds "trà sữa" (or something else).
  2. Canceling "trà xoài" results in "Lỗi: không tìm thấy món nào giống 'tra xoai'".
- **Error messages**: "Lỗi: không tìm thấy món nào giống 'tra xoai'. Con nhắn lại tên món giúp mẹ nha!"
- **Reproduction**: Try adding "trân châu đen", then "tôi muốn hủy trà xoài".

## Root Cause
1. **Fuzzy matching logic**: `MenuService.findByName` matched every word in the query against the item's name. "trà sữa trân châu đen" contains all the words in "trân châu đen", so both it and the topping "trân châu đen" achieved the same match score. Since "trà sữa" appeared first in the database, it was always picked.
2. **Intent extraction for removal**: `IntentDetector.isRemoveItemRequest` only worked if the message literally *started* with "hủy " or "xóa ". A phrase like "tôi muốn hủy trà xoài" fell through the fast-path and was sent to Gemini.
3. **Auto-Add Bug**: When Gemini tried to fulfill the "hủy" intent, it called the `find_item_by_name` tool to lookup the item ID for "tra xoai" (without diacritics). However, a recent hardcoded fix caused `find_item_by_name` to **always** automatically add the item to the cart! Furthermore, `findByName` didn't handle diacritic-less queries like "tra xoai", returning the error.

## Fix
1. **Normalized String & Better Scoring**: Updated `MenuService.findByName` to remove Vietnamese diacritics before matching. The scoring system now penalizes long names so that exact matches or closer matches win (e.g., "trân châu đen" now beats "trà sữa trân châu đen").
2. **Improved Fast-Path Intent Extraction**: Modified `IntentDetector` to recognize removal intents even if they appear in the middle of the sentence (e.g., "tôi muốn hủy").
3. **Safe Tool Execution**: Added an `intent` parameter (`add`, `remove`, `info`) to the `find_item_by_name` tool definition for Gemini. The `GroqService` now only auto-adds to the cart if `intent == "add"` or is missing.

## Files Changed
- `src/main/java/com/casso/milktea/service/MenuService.java`
- `src/main/java/com/casso/milktea/ai/IntentDetector.java`
- `src/main/java/com/casso/milktea/ai/GroqService.java`
