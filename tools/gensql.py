#!/usr/bin/env python3
"""Công cụ của nhóm (Python 3, không cần thư viện ngoài).
Sinh 03_du_lieu_mau.sql (T-SQL) + bản Derby (chỉ để kiểm thử nội bộ).
Ngày tháng tương đối với ngày chạy script: T-SQL dùng biến @homNay; Derby dùng mã {{D:n}} để trình nạp kiểm thử thay bằng ngày chạy + n."""
import re, sys, hashlib, base64, os
from datetime import date, timedelta
from decimal import Decimal

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DERBY = os.path.join(ROOT, 'src', 'test', 'resources', 'derby')
os.makedirs(OUT_DERBY, exist_ok=True)
TODAY = date.today()

# ---------- giá trị ----------
def D(n): return ('d', n)
def DT(n, hm): return ('dt', n, hm)
def T(hm): return ('t', hm)

def pw(raw, salt):
    s = salt.encode()
    h = hashlib.pbkdf2_hmac('sha256', raw.encode(), s, 65536, 32)
    return 'pbkdf2$65536$%s$%s' % (base64.b64encode(s).decode(), base64.b64encode(h).decode())

def lit(v, dia):
    if v is None: return 'NULL'
    if isinstance(v, bool): return '1' if v else '0'
    if isinstance(v, (int, Decimal)): return str(v)
    if isinstance(v, tuple):
        k = v[0]
        if k == 'd':
            if dia == 't': return 'DATEADD(DAY,%d,@homNay)' % v[1]
            return "DATE('{{D:%d}}')" % v[1]  # trình nạp Derby thay bằng ngày chạy kiểm thử + n
        if k == 'dt':
            h, m = map(int, v[2].split(':'))
            if dia == 't': return 'DATEADD(MINUTE,%d,CAST(DATEADD(DAY,%d,@homNay) AS DATETIME2(0)))' % (h * 60 + m, v[1])
            return "TIMESTAMP('{{D:%d}} %02d:%02d:00')" % (v[1], h, m)
        if k == 't':
            return "'%s:00'" % v[1] if dia == 't' else "TIME('%s:00')" % v[1]
    s = str(v).replace("'", "''")
    return ("N'%s'" if dia == 't' else "'%s'") % s

STMTS = []  # (table, cols, rows)
def ins(table, cols, rows):
    STMTS.append((table, [c.strip() for c in cols.split(',')], rows))

# ---------- tham số & bộ đếm ----------
ins('ThamSoHeThong', 'ten,giaTri,moTa', [
    ('SO_LAN_SAI_TOI_DA', '5', 'Số lần nhập sai mật khẩu liên tiếp trước khi tự khóa (Figma QL-12)'),
    ('SO_CA_TOI_DA_NGAY', '2', 'Số ca tối đa một nhân viên trong một ngày (giả định)'),
    ('TAI_KHAM_NGUONG_NGAY', '14', 'Hạng mục điều trị được xem là đến hạn tái khám khi còn ≤ số ngày này'),
    ('HSD_CANH_BAO_NGAY', '30', 'Cảnh báo lô sắp hết hạn trong số ngày này'),
    ('CC_NGUONG_HSD_NGAY', '30', 'Hộp cấp cứu: mục đạt khi HSD còn trên số ngày này (giả định, cấu hình được)'),
    ('CC_CHU_KY_KIEM_TRA_NGAY', '7', 'Hộp cấp cứu: chu kỳ kiểm tra định kỳ (giả định, cấu hình được)'),
    ('KHUNG_GIO_HEN', '08:00,08:30,09:00,09:30,10:00,10:30,11:00,13:30,14:00,14:30,15:00,15:30,16:00,16:30', 'Các khung giờ hẹn khám'),
])

# ---------- nhân viên & tài khoản ----------
NV = [
 ('NV0000','Quản trị viên','0988000000','AD','DANG_LAM_VIEC',D(-12000),'NAM','admin@n08dental.vn',D(-1700),'Quản trị hệ thống',None),
 ('NV0001','BS. Trần Minh','0903111201','BS','DANG_LAM_VIEC',D(-14000),'NAM','tranminh@n08dental.vn',D(-1680),'Nha tổng quát, nhổ răng',None),
 ('NV0002','BS. Phạm Anh','0903111202','BS','DANG_LAM_VIEC',D(-13500),'NU','phamanh@n08dental.vn',D(-1550),'Nội nha, điều trị tủy',None),
 ('NV0003','BS. Nguyễn Hà','0903111203','BS','DANG_LAM_VIEC',D(-12500),'NU','nguyenha@n08dental.vn',D(-1350),'Phẫu thuật nhổ răng khôn',None),
 ('NV0004','BS. Lê Thu','0903111204','BS','DANG_LAM_VIEC',D(-11800),'NU','lethu@n08dental.vn',D(-1120),'Phục hình, răng sứ',None),
 ('NV0005','Đỗ Thị Hạnh','0912222305','YT','DANG_LAM_VIEC',D(-10500),'NU','dohanh@n08dental.vn',D(-1290),'Phụ tá ghế',None),
 ('NV0006','Võ Văn Phúc','0912222306','YT','DANG_LAM_VIEC',D(-10200),'NAM','vophuc@n08dental.vn',D(-1210),'Vô khuẩn dụng cụ',None),
 ('NV0007','Lý Ngọc Trâm','0912222307','YT','DANG_LAM_VIEC',D(-9500),'NU','lytram@n08dental.vn',D(-1000),'Phụ tá phẫu thuật',None),
 ('NV0008','Lê Thị An','0934333408','LT','DANG_LAM_VIEC',D(-9800),'NU','lethian@n08dental.vn',D(-1620),'Tiếp đón, thu ngân',None),
 ('NV0009','Nguyễn Thị Mai','0934333409','LT','DANG_LAM_VIEC',D(-9300),'NU','nguyenmai@n08dental.vn',D(-1150),'Tiếp đón, thu ngân',None),
 ('NV0010','Trần Văn An','0934333410','LT','DANG_LAM_VIEC',D(-8700),'NAM','tranvanan@n08dental.vn',D(-590),'Tiếp đón',None),
 ('NV0011','Nguyễn Minh Tâm','0988444511','QL','DANG_LAM_VIEC',D(-13000),'NAM','minhtam@n08dental.vn',D(-1700),'Quản lý vận hành',None),
 ('NV0012','Phan Quốc Khánh','0988444512','KHO','DANG_LAM_VIEC',D(-12200),'NAM','quockhanh@n08dental.vn',D(-1590),'Kho, thuốc và vật tư',None),
 ('NV0013','Đặng Văn Lực','0912222313','YT','NGHI_VIEC',D(-9900),'NAM','dangluc@n08dental.vn',D(-1100),'Phụ tá ghế',D(-11)),
]
ins('NhanVien','maNhanVien,hoTen,soDienThoai,chucDanh,trangThai,ngaySinh,gioiTinh,email,ngayVaoLam,chuyenMon,ngayNghiViec', NV)
PW_ADMIN = pw('Admin@123', 'n08-admin-salt')
PW_OTHER = pw('Nhakhoa@123', 'n08-staff-salt')
TK = [
 ('TK0001','admin','HOAT_DONG','AD','NV0000',0,None),
 ('TK0002','tranminh','HOAT_DONG','BS','NV0001',0,None),
 ('TK0003','phamanh','HOAT_DONG','BS','NV0002',0,None),
 ('TK0004','nguyenha','HOAT_DONG','BS','NV0003',0,None),
 ('TK0005','lethu','HOAT_DONG','BS','NV0004',0,None),
 ('TK0006','dohanh','HOAT_DONG','YT','NV0005',0,None),
 ('TK0007','vophuc','HOAT_DONG','YT','NV0006',0,None),
 ('TK0008','lytram','HOAT_DONG','YT','NV0007',0,None),
 ('TK0009','lethian','HOAT_DONG','LT','NV0008',0,None),
 ('TK0010','nguyenmai','HOAT_DONG','LT','NV0009',0,None),
 ('TK0011','tranvanan','BI_KHOA','LT','NV0010',5,'Tự động khóa do nhập sai mật khẩu 5 lần liên tiếp'),
 ('TK0012','minhtam','HOAT_DONG','QL','NV0011',0,None),
 ('TK0013','quockhanh','HOAT_DONG','KHO','NV0012',0,None),
 ('TK0014','dangluc','VO_HIEU','YT','NV0013',0,'Vô hiệu khi nhân viên nghỉ việc'),
]
ins('TaiKhoan','maTaiKhoan,tenDangNhap,matKhau,trangThai,vaiTro,maNhanVien,soLanSai,lyDoKhoa',
    [(t[0],t[1],PW_ADMIN if t[1]=='admin' else PW_OTHER,t[2],t[3],t[4],t[5],t[6]) for t in TK])
TKU = {t[1]: t[0] for t in TK}

ins('CaLamViec','maCa,tenCa,gioBatDau,gioKetThuc,soBacSi,soYTa,soLeTan', [
 ('CA1','Ca sáng',T('07:30'),T('11:30'),2,2,1),('CA2','Ca chiều',T('13:00'),T('17:00'),2,2,1),('CA3','Ca tối',T('17:30'),T('20:30'),1,1,1)])

# ---------- phân công: hôm qua (đã xong) đến 6 ngày tới ----------
def seed(di, ca):
    g = {'BS': ['NV0001','NV0002'] if ca=='CA1' else ['NV0003','NV0004'] if ca=='CA2' else ['NV0001'],
         'YT': ['NV0005','NV0006'] if ca=='CA1' else ['NV0005','NV0007'] if ca=='CA2' else ['NV0006'],
         'LT': ['NV0008'] if ca=='CA1' else ['NV0009'] if ca=='CA2' else ['NV0010']}
    if di % 2 == 1 and ca == 'CA1': g['BS'] = ['NV0003','NV0002']
    if di == 0 and ca == 'CA2': g['YT'] = ['NV0005']
    if di == 0 and ca == 'CA3': g['YT'] = []
    return g
NVU = {'BS':'Khám & điều trị ghế %d','YT':'Phụ tá ghế %d','LT':'Tiếp đón, thu ngân'}
PC = []; n = 0
for di in range(-1, 7):
    for ca in ['CA1','CA2','CA3']:
        for k, ids in seed(di, ca).items():
            for i, nv in enumerate(ids):
                n += 1
                nvu = (NVU[k] % (i+1)) if '%d' in NVU[k] else NVU[k]
                if ca == 'CA3' and k == 'YT': nvu = 'Phụ tá ca tối'
                PC.append(('PC%06d' % n, nv, ca, D(di), nvu, 'HOAN_THANH' if di < 0 else 'CHO_HIEU_LUC', None, None))
for ca, ly in [('CA2','Nghỉ phép trong ngày'),('CA3','Xin nghỉ đột xuất')]:
    n += 1; PC.append(('PC%06d' % n, 'NV0007', ca, D(0), 'Phụ tá ghế 2' if ca=='CA2' else 'Phụ tá ca tối', 'DA_HUY', ly, None))
ins('PhanCong','maPhanCong,maNhanVien,maCa,ngayLam,nhiemVu,trangThai,lyDoHuy,maPhanCongGoc', PC)
N_PC = n

ins('BanGiaoCa','maBanGiao,thoiGian,noiDung,trangThai,ngay,maNguoiGiao,maNguoiNhan,maCaGiao,maCaNhan,coChenhLech,maTaiKhoanTao,maTaiKhoanXacNhan,thoiGianXacNhan,ghiChuXacNhan,maTaiKhoanHuy,thoiGianHuy,lyDoHuy', [
 ('BG000001',DT(-1,'17:10'),'Tạo nhầm người nhận.','DA_HUY',D(-1),'NV0007','NV0006','CA2','CA3',False,TKU['minhtam'],None,None,None,TKU['minhtam'],DT(-1,'17:20'),'Tạo nhầm người nhận'),
 ('BG000002',DT(-1,'20:40'),'Quầy đã đóng sổ; 1 lịch hẹn cần gọi xác nhận.','DA_BAN_GIAO',D(-1),'NV0009','NV0008','CA3','CA1',False,TKU['nguyenmai'],TKU['lethian'],DT(0,'07:25'),'Đã nhận sổ quầy',None,None,None),
 ('BG000003',DT(0,'11:32'),'BN Trần Thị Mai đang chờ trám răng 26 (PK000003).','DA_BAN_GIAO',D(0),'NV0002','NV0003','CA1','CA2',False,TKU['phamanh'],TKU['nguyenha'],DT(0,'12:55'),None,None,None,None),
 ('BG000004',DT(0,'11:40'),'BN đang chờ: 3 · Lịch hẹn còn lại ca chiều: 6 · Tiền mặt tại quầy: hệ thống 4.350.000 ₫, thực tế 4.300.000 ₫ (đã thối nhầm cho BN000014, đang liên hệ lại).','CHO_BAN_GIAO',D(0),'NV0008','NV0009','CA1','CA2',True,TKU['lethian'],None,None,None,None,None,None),
 ('BG000005',DT(0,'11:35'),'Còn 2 bộ dụng cụ chưa hấp; ghế 2 thiếu túi rác y tế.','CHO_BAN_GIAO',D(0),'NV0006','NV0005','CA1','CA2',False,TKU['vophuc'],None,None,None,None,None,None),
])

# ---------- đánh giá nhân sự: kỳ quý trước ----------
q = (TODAY.month - 1) // 3 + 1
py, pq = (TODAY.year, q - 1) if q > 1 else (TODAY.year - 1, 4)
KY = '%d-Q%d' % (py, pq)
TIEUCHI = ['Chuyên môn, tay nghề','Tuân thủ quy trình vô khuẩn','Thái độ với bệnh nhân','Phối hợp đồng nghiệp','Chuyên cần, đúng giờ']
DGD = [('NV0001',[5,5,5,4,5],'DA_GUI','Tay nghề vững, bệnh nhân hài lòng'),('NV0002',[5,4,5,4,5],'DA_GUI',None),('NV0003',[5,5,4,4,5],'DA_GUI',None),
       ('NV0004',[4,4,4,4,5],'DA_GUI',None),('NV0005',[4,5,5,4,4],'DA_GUI',None),('NV0006',[4,5,4,0,0],'NHAP','Chuẩn bị dụng cụ cẩn thận, hỗ trợ tốt ca nhổ răng khôn.'),
       ('NV0008',[4,4,4,4,4],'DA_GUI',None),('NV0009',[4,4,4,4,3],'DA_GUI',None)]
rows = []; ct = []
for i, (nv, d, st, nx) in enumerate(DGD, 1):
    m = 'DG%06d' % i
    v = [x for x in d if x]
    rows.append((m, nv, 'NV0011', KY, D(-5), Decimal('%.2f' % (sum(v)/len(v))), nx, st))
    for j, x in enumerate(d, 1):
        if x: ct.append((m, j, TIEUCHI[j-1], x))
ins('DanhGiaNhanSu','maDanhGia,maNhanVien,maNguoiDanhGia,kyDanhGia,ngayDanhGia,diem,nhanXet,trangThai', rows)
ins('ChiTietDanhGia','maDanhGia,thuTu,tieuChi,diem', ct)
N_DG = len(DGD)

# ---------- bệnh nhân & hồ sơ ----------
BN = [
 ('BN000001','Nguyễn Thị Lan',D(-10380),'NU','0909123456','123 Nguyễn Văn Cừ, Q.5, TP.HCM',None,'Viêm lợi tái phát','Penicillin',-940),
 ('BN000002','Lê Văn Hùng',D(-13100),'NAM','0903456789','45 Lê Lợi, Bà Rịa',None,None,None,-860),
 ('BN000003','Trần Thị Mai',D(-11420),'NU','0912345678','Ngãi Giao, Châu Đức','Chồng: Võ Tấn Lộc · 0912000111','Tăng huyết áp',None,-630),
 ('BN000004','Phạm Quốc Bảo',D(-14120),'NAM','0908765432','Kim Long, Châu Đức',None,None,None,-14),
 ('BN000005','Hoàng Thị Ngọc',D(-9500),'NU','0934567890','Phú Mỹ, BR-VT',None,None,'Lidocaine (nhẹ)',-6),
 ('BN000006','Vũ Minh Đức',D(-12170),'NAM','0987654321','Bình Ba, Châu Đức',None,'Tiểu đường type 2',None,-21),
 ('BN000007','Đỗ Thị Hương',D(-15170),'NU','0911222333','Xuân Sơn, Châu Đức',None,None,None,-24),
 ('BN000008','Trần Văn Nam',D(-17200),'NAM','0905888111','Long Điền, BR-VT',None,None,'Aspirin',-41),
 ('BN000009','Ngô Thanh Tùng',D(-9080),'NAM','0977111222','Bà Rịa, BR-VT',None,None,None,-63),
 ('BN000010','Bùi Thu Trang',D(-11120),'NU','0966333444','Ngãi Giao, Châu Đức',None,None,None,-82),
]
ins('BenhNhan','maBenhNhan,hoTen,ngaySinh,gioiTinh,soDienThoai,diaChi,nguoiLienHe,ngayTao,maTaiKhoanTao',
    [(b[0],b[1],b[2],b[3],b[4],b[5],b[6],DT(b[9],'09:00'),TKU['lethian']) for b in BN])
ins('HoSoBenhAn','maHoSo,maBenhNhan,ngayLap,tienSuBenh,diUng', [('HS'+b[0][2:], b[0], D(b[9]), b[7], b[8]) for b in BN])
HS = lambda bn: 'HS' + bn[2:]

ins('DichVu','maDichVu,tenDichVu,donGia,moTa,laCanLamSang,dangApDung', [
 ('DV01','Khám tổng quát',200000,None,False,True),('DV02','Chụp X-quang cận chóp',300000,None,True,True),
 ('DV03','Cạo vôi răng',500000,None,False,True),('DV04','Trám răng composite',400000,None,False,True),
 ('DV05','Nhổ răng khôn',1500000,None,False,True),('DV06','Điều trị tủy',1200000,None,False,True),
 ('DV07','Bọc răng sứ',3500000,None,False,True),('DV08','Tẩy trắng răng',2000000,None,False,True),
 ('DV09','Chụp X-quang toàn cảnh',450000,None,True,True)])
GIA = {'DV01':200000,'DV02':300000,'DV03':500000,'DV04':400000,'DV05':1500000,'DV06':1200000,'DV07':3500000,'DV08':2000000,'DV09':450000}

# ---------- kho ----------
ins('Thuoc','maThuoc,tenThuoc,donViTinh,kiemSoatDacBiet,hamLuong,hoatChat,dinhMucToiThieu,daDatHang', [
 ('T001','Amoxicillin 500mg','Viên',False,'500mg','Amoxicillin',200,False),
 ('T002','Paracetamol 500mg','Viên',False,'500mg','Paracetamol',300,False),
 ('T003','Lidocaine 2% (ống 1,8ml)','Ống',False,'2%','Lidocaine HCl',50,False),
 ('T004','Tramadol 50mg','Viên',True,'50mg','Tramadol HCl',20,False),
 ('T005','Adrenaline 1mg/ml','Ống',True,'1mg/ml','Epinephrine',6,False)])
ins('VatTu','maVatTu,tenVatTu,donViTinh,moTa,dinhMucToiThieu,daDatHang', [
 ('V001','Găng tay y tế (hộp 100)','Hộp',None,10,False),('V002','Mũi khoan kim cương','Cái',None,20,False),
 ('V003','Composite A2 (tuýp 4g)','Tuýp',None,8,False),('V004','Khẩu trang y tế (hộp 50)','Hộp',None,15,True)])
ins('NhaCungCap','maNhaCungCap,tenNhaCungCap,diaChi,soDienThoai,email,trangThai', [
 ('NCC01','Công ty TNHH Nha Phát','Q.10, TP.HCM','02838621122','sales@nhaphat.vn','DANG_HOP_TAC'),
 ('NCC02','Dược phẩm Minh Châu','Bà Rịa, BR-VT','02543725336','order@minhchau.vn','DANG_HOP_TAC'),
 ('NCC03','Vật tư y tế Đại Tín','Biên Hòa, Đồng Nai','02513822909','info@daitin.vn','NGUNG_HOP_TAC')])
# lô: (maLo, soLo, nsx, hsd, thuoc, vattu, ncc, soLuongNhapBanDau, donGia, ngayNhap)
LO = [
 ('LO000001','L2405',D(-164),D(566),'T001',None,'NCC02',420,1500,-10),
 ('LO000002','L2311',D(-337),D(393),'T002',None,'NCC02',152,800,-60),
 ('LO000003','LD0915',D(-393),D(16),'T003',None,'NCC01',30,9000,-120),
 ('LO000004','LD0402',D(-193),D(537),'T003',None,'NCC01',38,9500,-40),
 ('LO000005','TR0120',D(-265),D(465),'T004',None,'NCC02',64,6000,-10),
 ('LO000006','AD0326',D(-223),D(22),'T005',None,'NCC02',10,25000,-120),
 ('LO000007','GT0801',D(-438),D(658),None,'V001','NCC03',24,95000,-200),
 ('LO000008','MK0610',D(-124),None,None,'V002','NCC01',40,35000,-30),
 ('LO000009','CP0311',D(-580),D(-31),None,'V003','NCC01',5,180000,-300),
 ('LO000010','CP0702',D(-102),D(628),None,'V003','NCC01',11,185000,-60),
 ('LO000011','KT0901',D(-41),D(1054),None,'V004','NCC03',9,60000,-35),
]
ton = {l[0]: l[7] for l in LO}
PN = []; CTPN = []
for i, l in enumerate(LO, 1):
    PN.append(('PN%06d' % i, DT(l[9],'09:15'), 'HOAN_TAT', 'NHAP_NCC', l[6], None, 'NV0012', 'Nhập kho đầu kỳ (dữ liệu mẫu)'))
    CTPN.append(('PN%06d' % i, l[0], l[7], l[8]))
PX = [('PX000001',DT(-7,'08:10'),'Hộp cấp cứu','Võ Văn Phúc','Bổ sung hộp cấp cứu','NV0012',[('LO000006',2)]),
      ('PX000002',DT(-1,'16:20'),'Phòng thủ thuật 2','Lý Ngọc Trâm',None,'NV0012',[('LO000008',3),('LO000010',2)]),
      ('PX000003',DT(0,'07:40'),'Phòng thủ thuật 1','Đỗ Thị Hạnh',None,'NV0012',[('LO000003',6),('LO000007',2),('LO000005',4)])]
CTPX = []
for p in PX:
    for lo, q2 in p[6]:
        CTPX.append((p[0], lo, q2)); ton[lo] -= q2
# hoàn trả 1 mũi khoan nguyên seal từ PX000002
npn = len(LO) + 1
PN.append(('PN%06d' % npn, DT(-1,'17:30'), 'HOAN_TAT', 'HOAN_TRA', None, 'PX000002', 'NV0012', 'Hoàn trả hàng còn nguyên seal'))
CTPN.append(('PN%06d' % npn, 'LO000008', 1, 0)); ton['LO000008'] += 1
N_PN = npn
# điều chỉnh đã duyệt
DC = [('DC000001','HET_HAN',TKU['quockhanh'],DT(-27,'09:00'),'Lô hết hạn sử dụng','DA_DUYET',TKU['minhtam'],DT(-27,'10:30'),'Đồng ý, tiêu hủy theo quy định',[('LO000009',5,3)]),
      ('DC000002','KIEM_KE',TKU['quockhanh'],DT(-2,'16:30'),'Kiểm kê định kỳ','DA_DUYET',TKU['minhtam'],DT(-2,'17:05'),'Đồng ý',[('LO000002',152,150),('LO000011',9,8)])]
for d in DC:
    for lo, a, b in d[9]: ton[lo] += b - a
# phiếu hư hỏng chờ duyệt: số hệ thống = tồn hiện tại
DC.append(('DC000003','HU_HONG',TKU['quockhanh'],DT(0,'08:15'),'Hộp găng bị ẩm, mũi khoan mẻ, 2 ống thuốc tê vỡ khi vận chuyển','CHO_DUYET',None,None,None,
           [('LO000007',ton['LO000007'],ton['LO000007']-2),('LO000008',ton['LO000008'],ton['LO000008']-3),('LO000004',ton['LO000004'],ton['LO000004']-2)]))
ins('LoHang','maLo,soLo,ngaySanXuat,hanSuDung,maThuoc,maVatTu,maNhaCungCap,soLuongTon', [(l[0],l[1],l[2],l[3],l[4],l[5],l[6],ton[l[0]]) for l in LO])
ins('PhieuXuatKho','maPhieuXuat,ngayXuat,boPhanNhan,nguoiNhan,lyDo,trangThai,maNhanVien', [(p[0],p[1],p[2],p[3],p[4],'HOAN_TAT',p[5]) for p in PX])
ins('ChiTietPhieuXuat','maPhieuXuat,maLo,soLuong', CTPX)
ins('PhieuNhapKho','maPhieuNhap,ngayNhap,trangThai,loaiNhap,maNhaCungCap,maPhieuXuatGoc,maNhanVien,ghiChu', PN)
ins('ChiTietPhieuNhap','maPhieuNhap,maLo,soLuong,donGiaNhap', CTPN)
ins('PhieuDieuChinhKho','maPhieu,loai,maTaiKhoanTao,thoiGianTao,lyDo,trangThai,maTaiKhoanDuyet,thoiGianDuyet,ghiChuDuyet', [d[:9] for d in DC])
ins('ChiTietDieuChinhKho','maPhieu,maLo,soLuongHeThong,soLuongThucTe', [(d[0],lo,a,b) for d in DC for lo,a,b in d[9]])

GHICHU_MAU = 'Dữ liệu mẫu để chạy thử — không phải danh mục chuyên môn chính thức; cần quản lý kho xác nhận.'
CC = [('CC01','Adrenaline 1mg/ml','Ống',3,3,D(22),'T005',None),('CC02','Diphenhydramine 10mg/ml','Ống',2,2,D(610),None,None),
      ('CC03','Dexamethasone 4mg/ml','Ống',2,1,D(131),None,None),('CC04','Nước muối sinh lý 0,9% 500ml','Chai',2,2,D(415),None,None),
      ('CC05','Bơm tiêm 5ml','Cái',5,6,D(870),None,None),('CC06','Mặt nạ thở oxy','Cái',1,1,None,None,None)]
ins('DanhMucCapCuu','maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,maThuoc,maVatTu,dangApDung,laDuLieuMau,ghiChu',
    [c + (True, True, GHICHU_MAU) for c in CC])
KT = [('KT000001',DT(-21,'07:50'),'NV0006','DAT',None),('KT000002',DT(-14,'07:40'),'NV0012','DAT',None),
      ('KT000003',DT(-7,'07:45'),'NV0012','CAN_BO_SUNG','Dexamethasone còn 1/2 ống sau ca cấp cứu')]
ins('KiemTraCapCuu','maKiemTra,thoiGian,maNhanVien,ketQua,ghiChu', KT)
ctk = []
for k in KT:
    for c in CC:
        q3 = c[4] if k[0] == 'KT000003' else c[3] + (1 if c[0]=='CC05' else 0)
        if k[0] != 'KT000003' and c[0] == 'CC01': q3 = 3
        ctk.append((k[0], c[0], q3, c[5], q3 >= c[3]))
ins('ChiTietKiemTraCapCuu','maKiemTra,maMuc,soLuongThucTe,hanSuDung,dat', ctk)

# ---------- khám, kế hoạch, lịch hẹn ----------
PK = [
 ('PK000001','BN000004','NV0003',None,DT(-14,'09:05'),'Đau vùng răng khôn hàm dưới','Răng 38 và 48 mọc lệch, viêm lợi trùm','Nhổ lần lượt răng 38, 48','DA_KET_LUAN'),
 ('PK000002','BN000006','NV0002',None,DT(-21,'15:10'),'Đau nhức răng hàm dưới trái về đêm','Viêm tủy không hồi phục răng 36','Điều trị tủy răng 36, sau đó bọc sứ','DA_KET_LUAN'),
 ('PK000003','BN000009','NV0001',None,DT(-21,'08:40'),'Mắc thức ăn răng hàm dưới','Sâu ngà răng 36, 37','Trám răng 36, 37','DA_KET_LUAN'),
 ('PK000004','BN000008','NV0003',None,DT(-41,'10:00'),'Ê buốt khi ăn ngọt','Sâu ngà răng 46, 47','Trám răng 46, 47','DA_KET_LUAN'),
 ('PK000005','BN000001','NV0001','LH000001',DT(0,'07:58'),'Ê buốt răng hàm trên bên phải khi uống lạnh','Sâu ngà răng 16, viêm lợi nhẹ','Trám composite răng 16; cạo vôi định kỳ','DA_KET_LUAN'),
 ('PK000006','BN000002','NV0002','LH000002',DT(0,'08:31'),'Chảy máu chân răng khi chải','Vôi răng độ 2','Cạo vôi, đánh bóng; kiểm tra lại sau 6 tháng','DA_KET_LUAN'),
 ('PK000007','BN000003','NV0001','LH000003',DT(0,'08:58'),'Lỗ sâu răng 26, mắc thức ăn',None,None,'DANG_KHAM'),
]
KH = [
 ('KH000001','BN000008','PK000004',D(-41),'Trám răng 46, 47','HOAN_THANH','NV0003',[('DV04','46',-41,-41,'Ổn định','DA_THUC_HIEN','NV0003',None),('DV04','47',-34,-34,'Ổn định','DA_THUC_HIEN','NV0003',None)]),
 ('KH000002','BN000009','PK000003',D(-21),'Trám răng 36, 37','DANG_THUC_HIEN','NV0001',[('DV04','36',-21,-21,'Ổn định','DA_THUC_HIEN','NV0001',None),('DV04','37',-7,None,None,'CHUA_THUC_HIEN',None,None)]),
 ('KH000003','BN000006','PK000002',D(-21),'Điều trị tủy răng 36 và bọc sứ','DANG_THUC_HIEN','NV0002',[('DV06','36',-14,-14,'Lấy tủy lần 1, đặt thuốc','DA_THUC_HIEN','NV0002',None),('DV06','36',0,None,None,'CHUA_THUC_HIEN',None,None),('DV07','36',14,None,None,'CHUA_THUC_HIEN',None,None)]),
 ('KH000004','BN000004','PK000001',D(-14),'Nhổ răng khôn 38 và 48','DANG_THUC_HIEN','NV0003',[('DV02','38, 48',-14,-14,'Răng 38 mọc lệch 45°','DA_THUC_HIEN','NV0003',None),('DV05','38',-7,-7,'Nhổ thành công, khâu 2 mũi','DA_THUC_HIEN','NV0003',None),('DV01','38',0,None,None,'CHUA_THUC_HIEN',None,'Tái khám cắt chỉ'),('DV05','48',14,None,None,'CHUA_THUC_HIEN',None,None)]),
 ('KH000005','BN000001','PK000005',D(0),'Trám răng 16, cạo vôi định kỳ','DANG_THUC_HIEN','NV0001',[('DV03','Toàn hàm',0,0,'Đã làm sạch, lợi ổn','DA_THUC_HIEN','NV0001',None),('DV04','16',1,None,None,'CHUA_THUC_HIEN',None,None)]),
]
LH = [
 ('LH000001','BN000001','NV0001',DT(0,'08:00'),'Khám tổng quát','HOAN_THANH',None,None,None,'lethian'),
 ('LH000002','BN000002','NV0002',DT(0,'08:30'),'Cạo vôi răng','HOAN_THANH',None,None,None,'nguyenmai'),
 ('LH000003','BN000003','NV0001',DT(0,'09:00'),'Trám răng 26','DA_DEN',None,None,None,'lethian'),
 ('LH000004','BN000004','NV0003',DT(0,'09:30'),'Tái khám cắt chỉ răng 38','DA_XAC_NHAN',None,'KH000004',None,'lethian'),
 ('LH000005','BN000006','NV0002',DT(0,'15:00'),'Điều trị tủy răng 36 (lần 2)','DA_XAC_NHAN',None,'KH000003',None,'nguyenmai'),
 ('LH000006','BN000007','NV0004',DT(0,'15:30'),'Tư vấn bọc răng sứ','CHO_XAC_NHAN',None,None,None,'nguyenmai'),
 ('LH000007','BN000008','NV0003',DT(0,'16:00'),'Khám tổng quát','CHO_XAC_NHAN',None,None,None,'lethian'),
 ('LH000008','BN000005','NV0004',DT(0,'10:30'),'Tẩy trắng răng','DA_DOI_LICH',None,None,None,'lethian'),
 ('LH000009','BN000009','NV0001',DT(0,'08:30'),'Khám răng đau','VANG_MAT',None,None,None,'lethian'),
 ('LH000010','BN000010','NV0002',DT(0,'11:00'),'Khám tổng quát','DA_HUY',None,None,'Bệnh nhân bận công tác','nguyenmai'),
 ('LH000011','BN000001','NV0001',DT(1,'09:00'),'Trám răng 16','DA_XAC_NHAN',None,'KH000005',None,'lethian'),
 ('LH000012','BN000002','NV0004',DT(1,'10:00'),'Tư vấn tẩy trắng','CHO_XAC_NHAN',None,None,None,'lethian'),
 ('LH000013','BN000005','NV0004',DT(3,'09:00'),'Tẩy trắng răng','DA_XAC_NHAN','LH000008',None,None,'nguyenmai'),
 ('LH000014','BN000008','NV0004',DT(2,'14:00'),'Tư vấn phục hình','DA_XAC_NHAN',None,None,None,'lethian'),
]
# thứ tự chèn: KeHoach cần PhieuKham; LichHen cần KeHoach; PhieuKham.maLichHen cần LichHen
ins('PhieuKham','maPhieuKham,maBenhNhan,maHoSo,maNhanVien,maLichHen,thoiGianTiepNhan,trieuChung,chanDoan,ketLuan,trangThai',
    [(p[0],p[1],HS(p[1]),p[2],None,p[4],p[5],p[6],p[7],p[8]) for p in PK])
ins('KeHoachDieuTri','maKeHoach,maHoSo,maPhieuKham,ngayLap,mucTieu,trangThai,maNhanVien', [(k[0],HS(k[1]),k[2],k[3],k[4],k[5],k[6]) for k in KH])
ctdt = []
for k in KH:
    for c in k[7]:
        ctdt.append((k[0], c[0], c[1], D(c[2]), D(c[3]) if c[3] is not None else None, c[4], c[5], c[6], c[7]))
ins('ChiTietDieuTri','maKeHoach,maDichVu,viTriRang,ngayDuKien,ngayThucHien,ketQua,trangThai,maNhanVienThucHien,ghiChu', ctdt)
ins('LichHen','maLichHen,maBenhNhan,maNhanVien,thoiGianHen,lyDoHen,trangThai,maLichHenGoc,maKeHoach,lyDoHuy,maTaiKhoanTao,ngayTao',
    [(l[0],l[1],l[2],l[3],l[4],l[5],None,l[7],l[8],TKU[l[9]],DT(-3,'10:00')) for l in LH if l[6] is None] +
    [(l[0],l[1],l[2],l[3],l[4],l[5],l[6],l[7],l[8],TKU[l[9]],DT(-1,'09:20')) for l in LH if l[6] is not None])
UPD = [("UPDATE PhieuKham SET maLichHen = %s WHERE maPhieuKham = %s", (p[4], p[0])) for p in PK if p[3]]
UPD = [("UPDATE PhieuKham SET maLichHen = %s WHERE maPhieuKham = %s", (p[3], p[0])) for p in PK if p[3]]
ins('ChiDinhCanLamSang','maChiDinh,maPhieuKham,maDichVu,yeuCau,ketQua,maNhanVien,thoiGianChiDinh,thoiGianKetQua,trangThai', [
 ('CD000001','PK000005','DV02','X-quang cận chóp răng 16','Sâu ngà, chưa tổn thương tủy','NV0001',DT(0,'08:05'),DT(0,'08:20'),'DA_CO_KET_QUA'),
 ('CD000002','PK000001','DV02','X-quang răng 38, 48','Răng 38 mọc lệch 45°, 48 mọc lệch 30°','NV0003',DT(-14,'09:10'),DT(-14,'09:30'),'DA_CO_KET_QUA')])
ins('DonThuoc','maDonThuoc,maPhieuKham,maBenhNhan,maNhanVien,ngayKe,loiDan', [
 ('DT000001','PK000005','BN000001','NV0001',D(0),'Uống sau ăn. Bệnh nhân dị ứng Penicillin — không dùng nhóm Penicillin.'),
 ('DT000002','PK000001','BN000004','NV0003',D(-7),'Không dùng chung với rượu bia.')])
ins('ChiTietDonThuoc','maDonThuoc,maThuoc,soLuong,lieuDung,cachDung,soNgayDung', [
 ('DT000001','T002',10,'1 viên/lần','Khi đau, cách 6 giờ',5),('DT000002','T001',15,'1 viên/lần','Sáng – trưa – tối sau ăn',5),('DT000002','T004',6,'1 viên/lần','Khi đau nhiều, cách 6 giờ',3)])

# ---------- hóa đơn, thanh toán, yêu cầu hủy ----------
HD = [
 ('HD000001','NV0008','BN000001','PK000005',DT(0,'08:40'),'CHUA_THANH_TOAN',[('DV01',1),('DV02',1),('DV03',1)]),
 ('HD000002','NV0008','BN000002','PK000006',DT(0,'09:10'),'DA_THANH_TOAN',[('DV01',1),('DV03',1)]),
 ('HD000003','NV0009','BN000006','PK000002',DT(-14,'17:05'),'THANH_TOAN_MOT_PHAN',[('DV01',1),('DV06',1)]),
 ('HD000004','NV0008','BN000004','PK000001',DT(-7,'12:10'),'THANH_TOAN_MOT_PHAN',[('DV05',1)]),
 ('HD000005','NV0008','BN000007',None,DT(-24,'10:20'),'CHUA_THANH_TOAN',[('DV01',1),('DV02',1),('DV03',1)]),
 ('HD000006','NV0009','BN000005',None,DT(-6,'14:40'),'CHUA_THANH_TOAN',[('DV01',1),('DV02',1)]),
 ('HD000007','NV0009','BN000008','PK000004',DT(-34,'11:00'),'DA_THANH_TOAN',[('DV04',2)]),
 ('HD000008','NV0008','BN000010',None,DT(-82,'15:30'),'DA_HUY',[('DV01',1)]),
 ('HD000009','NV0008','BN000003',None,DT(-4,'10:00'),'DA_HUY',[('DV01',1)]),
]
ins('HoaDon','maHoaDon,maNhanVien,maBenhNhan,maPhieuKham,ngayLap,trangThai', [h[:6] for h in HD])
ins('ChiTietHoaDon','maHoaDon,maDichVu,soLuong,donGiaApDung', [(h[0], d, q4, GIA[d]) for h in HD for d, q4 in h[6]])
ins('ThanhToan','maThanhToan,maHoaDon,thoiGian,soTien,phuongThuc,maGiaoDich,tienKhachDua,trangThai,maNhanVien,maTaiKhoanHuy,thoiGianHuy,lyDoHuy', [
 ('TT000001','HD000002',DT(0,'09:12'),700000,'TIEN_MAT',None,700000,'THANH_CONG','NV0008',None,None,None),
 ('TT000002','HD000003',DT(-14,'17:08'),800000,'CHUYEN_KHOAN','FT26271880341',None,'THANH_CONG','NV0009',None,None,None),
 ('TT000003','HD000003',DT(-14,'17:10'),200000,'TIEN_MAT',None,200000,'DA_HUY','NV0009',TKU['minhtam'],DT(-14,'17:40'),'Nhập trùng — bệnh nhân chưa đưa tiền'),
 ('TT000004','HD000004',DT(-7,'12:12'),1000000,'THE_POS','POS 004512',None,'THANH_CONG','NV0008',None,None,None),
 ('TT000005','HD000007',DT(-34,'11:02'),800000,'CHUYEN_KHOAN','FT26251900127',None,'THANH_CONG','NV0009',None,None,None)])
ins('YeuCauHuyHoaDon','maYeuCau,maHoaDon,maTaiKhoanTao,thoiGianTao,lyDo,trangThai,maTaiKhoanDuyet,thoiGianDuyet,lyDoDuyet', [
 ('YC000001','HD000008',TKU['tranvanan'],DT(-82,'15:20'),'Bệnh nhân không đến','DA_DUYET',TKU['minhtam'],DT(-82,'17:00'),'Đồng ý'),
 ('YC000002','HD000007',TKU['nguyenmai'],DT(-34,'15:00'),'Bệnh nhân muốn đổi phương thức thanh toán','TU_CHOI',TKU['minhtam'],DT(-34,'16:05'),'Hóa đơn đã thanh toán đủ, không hủy được'),
 ('YC000003','HD000009',TKU['lethian'],DT(-4,'10:05'),'Sai thông tin bệnh nhân','DA_DUYET',TKU['minhtam'],DT(-4,'14:20'),'Đồng ý, lập lại hóa đơn'),
 ('YC000004','HD000006',TKU['admin'],DT(0,'11:10'),'Nhập trùng dịch vụ X-quang','CHO_DUYET',None,None,None),
 ('YC000005','HD000005',TKU['lethian'],DT(0,'09:42'),'Bệnh nhân đổi sang gói cạo vôi + đánh bóng, cần lập lại hóa đơn','CHO_DUYET',None,None,None)])

# ---------- nhật ký mẫu ----------
LOG = [
 (DT(-3,'10:00'),'lethian','Lịch hẹn','Tạo lịch hẹn','LH000001',None,None),
 (DT(0,'07:52'),'lethian','Tiếp nhận','Check-in','LH000001',None,None),
 (DT(0,'07:58'),'lethian','Tiếp nhận','Tạo phiếu khám từ lịch hẹn','LH000001','PK000005','NV0001'),
 (DT(0,'08:54'),'lethian','Tiếp nhận','Check-in','LH000003',None,None),
 (DT(0,'08:58'),'lethian','Tiếp nhận','Tạo phiếu khám từ lịch hẹn','LH000003','PK000007','NV0001'),
 (DT(-6,'14:00'),'lethian','Lịch hẹn','Tạo lịch hẹn','LH000008',None,None),
 (DT(-1,'09:20'),'nguyenmai','Lịch hẹn','Đổi lịch','LH000008','Sang LH000013',None),
 (DT(-1,'09:20'),'nguyenmai','Lịch hẹn','Tạo lịch do đổi lịch','LH000013','Từ LH000008',None),
 (DT(0,'09:00'),'lethian','Lịch hẹn','Đánh dấu vắng mặt','LH000009',None,None),
 (DT(-1,'16:30'),'nguyenmai','Lịch hẹn','Hủy lịch hẹn','LH000010','Bệnh nhân bận công tác',None),
 (DT(0,'09:12'),'lethian','Thu ngân','Thu 700.000 ₫ · Tiền mặt','HD000002','TT000001',None),
 (DT(0,'09:42'),'lethian','Thu ngân','Gửi yêu cầu hủy hóa đơn','YC000005','HD000005',None),
 (DT(0,'11:10'),'admin','Thu ngân','Gửi yêu cầu hủy hóa đơn','YC000004','HD000006',None),
 (DT(0,'08:15'),'quockhanh','Kho','Lập phiếu điều chỉnh (hư hỏng)','DC000003',None,None),
 (DT(-1,'17:05'),'tranvanan','Tài khoản','Tự động khóa sau 5 lần sai mật khẩu','TK0011',None,None),
]
ins('NhatKyThaoTac','thoiGian,maTaiKhoan,chucNang,hanhDong,doiTuong,noiDung,maNguoiChiuTrachNhiem', [(l[0],TKU[l[1]],l[2],l[3],l[4],l[5],l[6]) for l in LOG])

COUNTERS = [('NV','NV',4,13),('TK','TK',4,14),('BN','BN',6,10),('HS','HS',6,10),('LH','LH',6,len(LH)),('PK','PK',6,len(PK)),
 ('CD','CD',6,2),('DT','DT',6,2),('KH','KH',6,len(KH)),('HD','HD',6,len(HD)),('TT','TT',6,5),('YC','YC',6,5),
 ('PN','PN',6,N_PN),('PX','PX',6,len(PX)),('DC','DC',6,3),('LO','LO',6,len(LO)),('KT','KT',6,3),('PC','PC',6,N_PC),
 ('BG','BG',6,5),('DG','DG',6,N_DG),('DV','DV',2,9),('T','T',3,5),('V','V',3,4),('NCC','NCC',2,3),('CC','CC',2,6)]
ins('BoDemMa','loai,tienTo,doDai,giaTri', COUNTERS)

# ---------- xuất ----------
def emit(dia):
    out = []
    order = ['ThamSoHeThong','BoDemMa','NhanVien','TaiKhoan','CaLamViec','PhanCong','BanGiaoCa','DanhGiaNhanSu','ChiTietDanhGia',
             'BenhNhan','HoSoBenhAn','DichVu','Thuoc','VatTu','NhaCungCap','LoHang','PhieuXuatKho','ChiTietPhieuXuat','PhieuNhapKho',
             'ChiTietPhieuNhap','PhieuDieuChinhKho','ChiTietDieuChinhKho','DanhMucCapCuu','KiemTraCapCuu','ChiTietKiemTraCapCuu',
             'PhieuKham','KeHoachDieuTri','ChiTietDieuTri','LichHen','#UPD','ChiDinhCanLamSang','DonThuoc','ChiTietDonThuoc',
             'HoaDon','ChiTietHoaDon','ThanhToan','YeuCauHuyHoaDon','NhatKyThaoTac']
    by = {}
    for t, c, r in STMTS: by.setdefault(t, []).append((c, r))
    for t in order:
        if t == '#UPD':
            for sql, args in UPD:
                out.append((sql % tuple(lit(a, dia) for a in args)) + ';')
            continue
        for cols, rows in by[t]:
            for r in rows:
                out.append('INSERT INTO %s (%s) VALUES (%s);' % (t, ', '.join(cols), ', '.join(lit(v, dia) for v in r)))
    return out

tsql = ['/* =====================================================================',
 '   03_du_lieu_mau.sql — Dữ liệu khởi tạo và dữ liệu mẫu để chạy thử',
 '   - Ngày tháng tính tương đối theo ngày chạy script (@homNay), để màn hình',
 '     "hôm nay" luôn có dữ liệu minh họa.',
 '   - Tài khoản mẫu: admin / Admin@123 ; các tài khoản khác / Nhakhoa@123',
 '     (tranvanan đang bị khóa, dangluc đã vô hiệu). Đổi mật khẩu sau khi cài.',
 '   - Danh mục hộp cấp cứu là DỮ LIỆU MẪU (laDuLieuMau = 1), không phải',
 '     hướng dẫn chuyên môn chính thức.',
 '   - Chạy lại script: chạy lại 01 + 02 trước (database được tạo mới).',
 '   ===================================================================== */',
 'USE N08_NhaKhoa;', 'GO', 'SET NOCOUNT ON;', 'SET XACT_ABORT ON;', 'BEGIN TRANSACTION;',
 'DECLARE @homNay DATE = CAST(GETDATE() AS DATE);'] + emit('t') + ['COMMIT TRANSACTION;', 'GO']
open(ROOT + '/sql/03_du_lieu_mau.sql', 'w', encoding='utf-8').write('\n'.join(tsql) + '\n')
open(OUT_DERBY + '/seed.sql', 'w', encoding='utf-8').write('\n'.join(emit('d')) + '\n')

# ---------- DDL → Derby ----------
ddl = open(ROOT + '/sql/02_tao_bang.sql', encoding='utf-8').read()
ddl = re.sub(r'/\*.*?\*/', '', ddl, flags=re.S)
batches = [b.strip() for b in re.split(r'^\s*GO\s*$', ddl, flags=re.M) if b.strip()]
der = []
for b in batches:
    if b.startswith('USE '): continue
    if re.search(r'CREATE UNIQUE INDEX .* WHERE ', b): continue  # chỉ mục lọc: SQL Server
    s = b
    s = re.sub(r'NVARCHAR\((\d+)\)', r'VARCHAR(\1)', s)
    s = re.sub(r'DATETIME2\(0\)', 'TIMESTAMP', s)
    s = re.sub(r'TIME\(0\)', 'TIME', s)
    s = re.sub(r'\bBIT\b', 'SMALLINT', s)
    s = re.sub(r'(INT|BIGINT) IDENTITY\(1,1\) NOT NULL', r'\1 NOT NULL GENERATED BY DEFAULT AS IDENTITY', s)
    s = re.sub(r"(?<![\w])N'", "'", s)
    s = '\n'.join(re.sub(r'(?<!NOT)(?<!IS)\s+NULL(,?)\s*$', r'\1', ln) if re.match(r'^\s+\w+\s+\w', ln) and not ln.strip().startswith('CONSTRAINT') else ln for ln in s.split('\n'))
    s = s.replace('ALTER TABLE KeHoachDieuTri ADD CONSTRAINT', 'ALTER TABLE KeHoachDieuTri ADD CONSTRAINT')
    der.append(s.rstrip(';') + ';')
open(OUT_DERBY + '/schema.sql', 'w', encoding='utf-8').write('\n'.join(der) + '\n')
print('seed statements:', len(emit('t')), 'ddl statements:', len(der), 'today', TODAY, 'ky', KY)
