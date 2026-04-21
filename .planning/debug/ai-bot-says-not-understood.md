---
status: resolved
trigger: "mô hình chat bot AI đang hoạt động không hiệu quả, tôi có thể ví dụ 1 trường hợp lỗi trước đó như sau:\n- tôi order các món ví dụ như \"1 trà sữa trân châu trắng size M\", nó liên tục trả lời \"Mẹ nghe con rồi nhưng chưa hiểu lắm 😅 Con nhắn rõ hơn giúp mẹ nha!\", nhưng khi tôi bảo nó cho tôi xem giỏ hàng thì kết quả vẫn có đủ những món đã gọi trước đó"
---

## Symptoms
- **Expected behavior**: Bot should naturally acknowledge the order and confirm it was added to the cart.
- **Actual behavior**: Bot replies with a fallback error message ("Mẹ nghe con rồi nhưng chưa hiểu lắm..."), but the item is actually added to the cart successfully.
- **Error messages**: "Mẹ nghe con rồi nhưng chưa hiểu lắm 😅 Con nhắn rõ hơn giúp mẹ nha!"
- **Reproduction**: Send order message like "1 trà sữa trân châu trắng size M".

## Root Cause
When the user sends a direct order like "1 trà sữa trân châu trắng size M", the `IntentDetector` correctly calls `find_item_by_name`. In recent updates to prevent a "double add" bug, `find_item_by_name` was changed to instantly invoke `add_to_cart` server-side, and then Turn 2 of the AI generation was intentionally skipped (so Gemini wouldn't hallucinate another cart addition). 

However, when Turn 2 was skipped, the response `text` variable was left empty (since Turn 1 text was empty due to calling a tool), causing it to fall through to the final safety fallback `if (text == null || text.isBlank())`, which hardcodes the generic "chưa hiểu lắm" response.

## Fix
Updated `GroqService.java` to capture the output of the tool execution (`lastToolResult`) in Turn 1. If Turn 2 is skipped because `find_item_by_name` was invoked, `text` is explicitly assigned to `lastToolResult` instead of remaining `null`. This surfaces the successful "Đã thêm trà sữa trân châu trắng size M (1 ly) vào giỏ" message back to the user seamlessly.

## Files Changed
- `src/main/java/com/casso/milktea/ai/GroqService.java`
