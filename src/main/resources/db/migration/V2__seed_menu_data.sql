-- V2: Seed menu data from CSV

INSERT INTO menu_item (item_id, category, name, description, price_m, price_l, available) VALUES
-- Trà Sữa
('TS01', 'Trà Sữa', 'Trà Sữa Trân Châu Đen', 'Trà sữa thơm béo với trân châu đen dai ngon', 35000, 45000, true),
('TS02', 'Trà Sữa', 'Trà Sữa Trân Châu Trắng', 'Trà sữa mịn với trân châu trắng thơm', 35000, 45000, true),
('TS03', 'Trà Sữa', 'Trà Sữa Truyền Thống', 'Trà sữa nguyên chất không topping', 30000, 40000, true),
('TS04', 'Trà Sữa', 'Trà Sữa Khoai Môn', 'Trà sữa vị khoai môn hấp dẫn', 38000, 48000, true),
('TS05', 'Trà Sữa', 'Trà Sữa Bạc Hà', 'Trà sữa thơm mát bạc hà tươi', 35000, 45000, true),
-- Trà Trái Cây
('TTG01', 'Trà Trái Cây', 'Trà Dâu Tây', 'Trà tươi với vị dâu tây ngọt ngào', 32000, 42000, true),
('TTG02', 'Trà Trái Cây', 'Trà Mâm Xôi', 'Trà mâm xôi tươi mát giải khát', 32000, 42000, true),
('TTG03', 'Trà Trái Cây', 'Trà Chanh Leo', 'Trà chanh leo chua nhẹ sảng khoái', 30000, 40000, true),
('TTG04', 'Trà Trái Cây', 'Trà Vải Thiều', 'Trà vải thiều ngọt hương lạ', 33000, 43000, true),
('TTG05', 'Trà Trái Cây', 'Trà Xoài', 'Trà xoài tươi thơm vị nhiệt đới', 32000, 42000, true),
-- Cà Phê
('CF01', 'Cà Phê', 'Cà Phê Đen', 'Cà phê đen đậm đà truyền thống', 25000, 30000, true),
('CF02', 'Cà Phê', 'Cà Phê Sữa', 'Cà phê sữa béo ngậy hạnh phúc', 28000, 33000, true),
('CF03', 'Cà Phê', 'Cà Phê Caramel', 'Cà phê với ít caramel mượt mà', 30000, 35000, true),
('CF04', 'Cà Phê', 'Cà Phê Mocha', 'Cà phê với chocolate ngọt dịu', 32000, 37000, true),
('CF05', 'Cà Phê', 'Cà Phê Macchiato', 'Cà phê espresso với ít sữa', 27000, 32000, true),
-- Đá Xay
('DX01', 'Đá Xay', 'Đá Xay Dâu Tây', 'Đá xay mịn vị dâu tây tươi mát', 35000, 45000, true),
('DX02', 'Đá Xay', 'Đá Xay Dừa', 'Đá xay vị dừa ngọt dễ chịu', 35000, 45000, true),
('DX03', 'Đá Xay', 'Đá Xay Matcha', 'Đá xay matcha tươi giải khát', 38000, 48000, true),
('DX04', 'Đá Xay', 'Đá Xay Sôcôla', 'Đá xay sôcôla ngọt thơm', 36000, 46000, true),
-- Topping
('TOP01', 'Topping', 'Trân Châu Đen', 'Trân châu đen dai ngon', 5000, 5000, true),
('TOP02', 'Topping', 'Trân Châu Trắng', 'Trân châu trắng mềm dẻo', 5000, 5000, true),
('TOP03', 'Topping', 'Thạch Cà Chua', 'Thạch cà chua mọng nước', 4000, 4000, true),
('TOP04', 'Topping', 'Thạch Xanh', 'Thạch xanh mát lạnh tươi', 4000, 4000, true),
('TOP05', 'Topping', 'Nước Cốt Dừa', 'Nước cốt dừa béo ngậy', 6000, 6000, true),
('TOP06', 'Topping', 'Kem Tươi', 'Kem tươi mịn ngọt dịu', 8000, 8000, true),
('TOP07', 'Topping', 'Gelée Khoai Môn', 'Gelée khoai môn vị độc đáo', 5000, 5000, true),
('TOP08', 'Topping', 'Bột Trà Xanh', 'Bột trà xanh thơm ngậy', 3000, 3000, true);
