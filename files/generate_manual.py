from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    HRFlowable, PageBreak, Image
)
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY, TA_RIGHT
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import os

BASE_DIR   = os.path.dirname(__file__)
OUTPUT     = os.path.join(BASE_DIR, 'Attendly_Manual_ID.pdf')
FRONT_IMG  = os.path.join(BASE_DIR, 'design', 'front.png')

# ── Styles ──────────────────────────────────────────────────────────────────
styles = getSampleStyleSheet()

PRIMARY   = colors.HexColor('#1565C0')
SECONDARY = colors.HexColor('#1E88E5')
ACCENT    = colors.HexColor('#E3F2FD')
DARK      = colors.HexColor('#212121')
MUTED     = colors.HexColor('#757575')
WHITE     = colors.white
WARN_BG   = colors.HexColor('#FFF8E1')
WARN_BORDER = colors.HexColor('#F9A825')
TIP_BG    = colors.HexColor('#E8F5E9')
TIP_BORDER  = colors.HexColor('#388E3C')

def style(name, **kw):
    s = ParagraphStyle(name, **kw)
    return s

H1 = style('H1', fontSize=22, leading=28, textColor=WHITE,
           fontName='Helvetica-Bold', alignment=TA_CENTER, spaceAfter=4)
H2 = style('H2', fontSize=14, leading=18, textColor=PRIMARY,
           fontName='Helvetica-Bold', spaceBefore=14, spaceAfter=6)
H3 = style('H3', fontSize=11, leading=15, textColor=SECONDARY,
           fontName='Helvetica-Bold', spaceBefore=8, spaceAfter=4)
BODY = style('BODY', fontSize=10, leading=15, textColor=DARK,
             fontName='Helvetica', spaceAfter=5, alignment=TA_JUSTIFY)
BULLET = style('BULLET', fontSize=10, leading=15, textColor=DARK,
               fontName='Helvetica', leftIndent=16, spaceAfter=3,
               bulletIndent=4)
CODE = style('CODE', fontSize=9, leading=13, textColor=colors.HexColor('#BF360C'),
             fontName='Courier', backColor=colors.HexColor('#FBE9E7'),
             leftIndent=8, rightIndent=8, spaceAfter=4)
CAPTION = style('CAPTION', fontSize=8, leading=11, textColor=MUTED,
                fontName='Helvetica-Oblique', alignment=TA_CENTER, spaceAfter=6)
COVER_SUB = style('COVER_SUB', fontSize=12, leading=16, textColor=WHITE,
                  fontName='Helvetica', alignment=TA_CENTER)
COVER_VER = style('COVER_VER', fontSize=10, leading=14,
                  textColor=colors.HexColor('#BBDEFB'),
                  fontName='Helvetica', alignment=TA_CENTER)

# ── Helper builders ──────────────────────────────────────────────────────────

def hr():
    return HRFlowable(width='100%', thickness=1, color=colors.HexColor('#BBDEFB'),
                      spaceAfter=6, spaceBefore=2)

def sp(h=6):
    return Spacer(1, h)

def h2(text):
    return Paragraph(text, H2)

def h3(text):
    return Paragraph(text, H3)

def p(text):
    return Paragraph(text, BODY)

def b(text):
    return Paragraph(f'• {text}', BULLET)

def note(text, bg=WARN_BG, border=WARN_BORDER, icon='⚠'):
    s = ParagraphStyle('note', fontSize=9, leading=13, textColor=DARK,
                       fontName='Helvetica', leftIndent=10, rightIndent=10,
                       spaceAfter=6, spaceBefore=4,
                       backColor=bg, borderColor=border,
                       borderWidth=1, borderPadding=6, borderRadius=4)
    return Paragraph(f'<b>{icon}</b>  {text}', s)

def tip(text):
    return note(text, bg=TIP_BG, border=TIP_BORDER, icon='💡')

def step_table(rows):
    """Numbered step table: [(num, title, desc), ...]"""
    data = []
    for num, title, desc in rows:
        num_para  = Paragraph(f'<b>{num}</b>', ParagraphStyle(
            'sn', fontSize=14, leading=18, textColor=WHITE,
            fontName='Helvetica-Bold', alignment=TA_CENTER))
        body_para = Paragraph(f'<b>{title}</b><br/>{desc}', ParagraphStyle(
            'sb', fontSize=10, leading=14, textColor=DARK, fontName='Helvetica'))
        data.append([num_para, body_para])

    t = Table(data, colWidths=[1.2*cm, 14.3*cm])
    t.setStyle(TableStyle([
        ('BACKGROUND',  (0, 0), (0, -1), SECONDARY),
        ('VALIGN',      (0, 0), (-1, -1), 'MIDDLE'),
        ('ROWBACKGROUNDS', (1, 0), (1, -1), [ACCENT, WHITE]),
        ('BOX',         (0, 0), (-1, -1), 0.5, colors.HexColor('#90CAF9')),
        ('INNERGRID',   (0, 0), (-1, -1), 0.3, colors.HexColor('#BBDEFB')),
        ('TOPPADDING',  (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING',(0, 0), (-1, -1), 6),
        ('LEFTPADDING', (1, 0), (1, -1), 10),
    ]))
    return t

def two_col(left_items, right_items):
    """Two-column layout using a table."""
    def col(items):
        return [Paragraph(i, BODY) if isinstance(i, str) else i for i in items]
    from reportlab.platypus import KeepInFrame
    lf = KeepInFrame(7.5*cm, 40*cm, col(left_items))
    rf = KeepInFrame(7.5*cm, 40*cm, col(right_items))
    t = Table([[lf, rf]], colWidths=[7.8*cm, 7.8*cm])
    t.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 0),
        ('BOTTOMPADDING', (0,0), (-1,-1), 0),
        ('LINEAFTER', (0,0), (0,-1), 0.5, colors.HexColor('#BBDEFB')),
        ('RIGHTPADDING', (0,0), (0,-1), 10),
        ('LEFTPADDING', (1,0), (1,-1), 10),
    ]))
    return t

# ── Cover page ───────────────────────────────────────────────────────────────

def cover_page():
    """Full-page cover drawn directly on the canvas via a custom flowable."""
    from reportlab.platypus.flowables import Flowable

    class CoverFlowable(Flowable):
        def __init__(self):
            Flowable.__init__(self)
            # Will be sized to fill the whole page content area
            self.width  = A4[0]
            self.height = A4[1]

        def wrap(self, availW, availH):
            return (A4[0], A4[1])

        def draw(self):
            c = self.canv
            W, H = A4

            # ── Background: full page deep blue ──────────────────────────
            c.setFillColor(PRIMARY)
            c.rect(0, 0, W, H, fill=1, stroke=0)

            # ── Left panel: phone mockup image ───────────────────────────
            img_w = 8.5 * cm
            img_h = img_w * (1142 / 576)          # keep aspect ratio
            img_x = 0.8 * cm
            img_y = (H - img_h) / 2 - 0.5 * cm   # vertically centred, slight offset down

            # Soft white glow behind the phone
            c.setFillColor(colors.HexColor('#1E88E5'))
            c.roundRect(img_x - 0.3*cm, img_y - 0.3*cm,
                        img_w + 0.6*cm, img_h + 0.6*cm,
                        radius=12, fill=1, stroke=0)

            c.drawImage(FRONT_IMG, img_x, img_y, width=img_w, height=img_h,
                        preserveAspectRatio=True, mask='auto')

            # ── Right panel: text content ─────────────────────────────────
            rx = img_x + img_w + 1.0 * cm   # left edge of text area
            rw = W - rx - 1.0 * cm          # available text width

            # App name
            c.setFillColor(WHITE)
            c.setFont('Helvetica-Bold', 30)
            c.drawString(rx, H * 0.72, 'Attendly')

            # Thin accent line under app name
            c.setStrokeColor(colors.HexColor('#64B5F6'))
            c.setLineWidth(2)
            c.line(rx, H * 0.71, rx + rw, H * 0.71)

            # Subtitle
            c.setFont('Helvetica', 13)
            c.setFillColor(colors.HexColor('#BBDEFB'))
            c.drawString(rx, H * 0.675, 'Panduan Pengguna')

            # Tagline
            c.setFont('Helvetica', 10)
            c.setFillColor(colors.HexColor('#90CAF9'))
            c.drawString(rx, H * 0.645, 'Aplikasi Absensi QR Code untuk Android')

            # Version badge
            badge_y = H * 0.595
            badge_h = 0.55 * cm
            badge_w = 4.2 * cm
            c.setFillColor(colors.HexColor('#1565C0'))
            c.roundRect(rx, badge_y, badge_w, badge_h, radius=4, fill=1, stroke=0)
            c.setFillColor(colors.HexColor('#64B5F6'))
            c.setFont('Helvetica-Bold', 9)
            c.drawCentredString(rx + badge_w / 2, badge_y + 0.14*cm, 'Versi 1.1.0')

            # Feature list
            features = [
                ('📷', 'Scan Kode QR'),
                ('📊', 'Google Sheets'),
                ('📦', 'Mode Offline'),
                ('🌙', 'Dark Mode'),
                ('🌐', 'Dua Bahasa'),
            ]
            fy = H * 0.52
            for icon, label in features:
                # bullet dot
                c.setFillColor(colors.HexColor('#64B5F6'))
                c.circle(rx + 0.18*cm, fy + 0.18*cm, 0.08*cm, fill=1, stroke=0)
                c.setFillColor(WHITE)
                c.setFont('Helvetica', 10)
                c.drawString(rx + 0.4*cm, fy, f'{icon}  {label}')
                fy -= 0.55 * cm

            # Bottom strip
            strip_h = 1.4 * cm
            c.setFillColor(colors.HexColor('#0D47A1'))
            c.rect(0, 0, W, strip_h, fill=1, stroke=0)
            c.setFillColor(colors.HexColor('#90CAF9'))
            c.setFont('Helvetica', 8)
            c.drawCentredString(W / 2, 0.5 * cm, 'Bahasa Indonesia  •  MIT License  •  Android 7.0+')

    return [CoverFlowable(), PageBreak()]

# ── TOC ──────────────────────────────────────────────────────────────────────

def toc_page():
    toc_style = ParagraphStyle('toc', fontSize=10, leading=18, textColor=DARK,
                               fontName='Helvetica', leftIndent=8)
    toc_num   = ParagraphStyle('tocn', fontSize=10, leading=18, textColor=MUTED,
                               fontName='Helvetica', alignment=TA_CENTER)
    rows = [
        ('1', 'Pengenalan Aplikasi'),
        ('2', 'Tampilan Utama'),
        ('3', 'Cara Scan Kode QR'),
        ('4', 'Entri ID Manual'),
        ('5', 'Mode Penyimpanan'),
        ('6', 'Data Lokal & Unggah ke Google Sheets'),
        ('7', 'Pengaturan'),
        ('8', 'Persiapan Google Sheets (Satu Kali)'),
        ('9', 'Ganti Acara / Tab Sheet'),
        ('10', 'Pemecahan Masalah'),
        ('11', 'Informasi Aplikasi'),
    ]
    data = [[Paragraph(f'<b>{n}</b>', toc_num),
             Paragraph(t, toc_style)] for n, t in rows]
    t = Table(data, colWidths=[1.2*cm, 14.3*cm])
    t.setStyle(TableStyle([
        ('ROWBACKGROUNDS', (0,0), (-1,-1), [ACCENT, WHITE]),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#90CAF9')),
        ('INNERGRID', (0,0), (-1,-1), 0.3, colors.HexColor('#BBDEFB')),
    ]))
    return [
        h2('Daftar Isi'),
        hr(),
        t,
        PageBreak(),
    ]

# ── Section 1 – Pengenalan ───────────────────────────────────────────────────

def section_intro():
    return [
        h2('1.  Pengenalan Aplikasi'),
        hr(),
        p('Attendly adalah aplikasi Android untuk mencatat kehadiran menggunakan kode QR. '
          'Host (penyelenggara) cukup mengarahkan kamera ke kode QR peserta — data langsung '
          'tersimpan ke Google Sheet atau ke perangkat secara offline.'),
        sp(4),
        step_table([
            ('📷', 'Scan Kode QR', 'Baca kode QR peserta dengan kamera.'),
            ('📊', 'Google Sheets', 'Data langsung masuk ke spreadsheet tanpa API key.'),
            ('📦', 'Mode Offline', 'Simpan data lokal saat tidak ada internet, unggah nanti.'),
            ('🌙', 'Dark Mode', 'Tampilan gelap untuk kenyamanan mata.'),
            ('🌐', 'Dua Bahasa', 'Antarmuka tersedia dalam Bahasa Indonesia dan English.'),
        ]),
        sp(8),
        note('Attendly membutuhkan Android 7.0 (API 24) atau lebih baru dan izin kamera.'),
        sp(10),
    ]

# ── Section 2 – Tampilan Utama ───────────────────────────────────────────────

def section_home():
    return [
        h2('2.  Tampilan Utama'),
        hr(),
        p('Saat aplikasi dibuka, Anda akan melihat enam tombol utama:'),
        sp(4),
        step_table([
            ('📷', 'Scan Kode QR', 'Buka kamera untuk memindai kode QR peserta.'),
            ('📊', 'Tabel / Data Lokal', 'Lihat semua data yang tersimpan di perangkat.'),
            ('⚙', 'Pengaturan', 'Konfigurasi URL Apps Script, nama sheet, dan preferensi lain.'),
            ('❓', 'Bantuan', 'Panduan langkah demi langkah cara menggunakan aplikasi.'),
            ('ℹ', 'Info', 'Informasi versi dan lisensi aplikasi.'),
        ]),
        sp(6),
        p('Di bagian bawah layar terdapat kolom teks untuk <b>entri ID manual</b> dan tombol '
          '<b>Kirim</b>. Di atasnya ditampilkan status scan terakhir.'),
        sp(10),
    ]

# ── Section 3 – Scan QR ──────────────────────────────────────────────────────

def section_scan():
    return [
        h2('3.  Cara Scan Kode QR'),
        hr(),
        step_table([
            ('1', 'Pastikan sudah dikonfigurasi',
             'URL Apps Script harus sudah diisi di Pengaturan, atau aktifkan Mode Penyimpanan Lokal.'),
            ('2', 'Ketuk tombol Scan Kode QR',
             'Kamera akan terbuka secara otomatis.'),
            ('3', 'Arahkan ke kode QR',
             'Posisikan kode QR peserta di dalam bingkai kamera.'),
            ('4', 'Tunggu bunyi bip',
             'Aplikasi berbunyi dan data langsung dikirim ke Google Sheet atau disimpan lokal.'),
            ('5', 'Cek status',
             'Layar utama menampilkan ID dan waktu scan terakhir.'),
        ]),
        sp(6),
        tip('Pastikan pencahayaan cukup agar kamera dapat membaca kode QR dengan cepat.'),
        sp(10),
    ]

# ── Section 4 – Entri Manual ─────────────────────────────────────────────────

def section_manual():
    return [
        h2('4.  Entri ID Manual'),
        hr(),
        p('Gunakan fitur ini jika kode QR peserta tidak dapat dipindai (rusak, layar redup, dll).'),
        sp(4),
        step_table([
            ('1', 'Ketuk kolom teks di bawah layar',
             'Keyboard akan muncul secara otomatis.'),
            ('2', 'Ketik ID atau nama peserta',
             'Masukkan teks yang ingin dicatat.'),
            ('3', 'Ketuk tombol Kirim atau tekan Enter',
             'Data dikirim ke Google Sheet atau disimpan lokal, sama seperti hasil scan.'),
        ]),
        sp(10),
    ]

# ── Section 5 – Mode Penyimpanan ─────────────────────────────────────────────

def section_storage_mode():
    return [
        h2('5.  Mode Penyimpanan'),
        hr(),
        p('Attendly mendukung dua mode penyimpanan. Pilih sesuai kondisi jaringan Anda:'),
        sp(6),
        two_col(
            [
                Paragraph('<b>Mode Google Sheets (Default)</b>', H3),
                Paragraph(
                    'Setiap scan langsung dikirim ke Google Sheet melalui internet. '
                    'Cocok saat koneksi stabil.', BODY),
                sp(4),
                Paragraph('✅ Data langsung tersedia di spreadsheet', BULLET),
                Paragraph('✅ Tidak perlu langkah unggah tambahan', BULLET),
                Paragraph('⚠ Butuh koneksi internet aktif', BULLET),
            ],
            [
                Paragraph('<b>Mode Penyimpanan Lokal</b>', H3),
                Paragraph(
                    'Data disimpan di perangkat terlebih dahulu. Unggah ke Google Sheet '
                    'kapan saja saat internet tersedia.', BODY),
                sp(4),
                Paragraph('✅ Bekerja tanpa internet', BULLET),
                Paragraph('✅ Aman dari gangguan jaringan', BULLET),
                Paragraph('⚠ Perlu unggah manual setelahnya', BULLET),
            ]
        ),
        sp(8),
        note('Untuk mengaktifkan Mode Lokal: buka <b>Pengaturan → Penyimpanan → '
             'Simpan data secara lokal</b>, lalu aktifkan toggle-nya.'),
        sp(10),
    ]

# ── Section 6 – Data Lokal ───────────────────────────────────────────────────

def section_local_data():
    return [
        h2('6.  Data Lokal & Unggah ke Google Sheets'),
        hr(),
        p('Halaman <b>Data Lokal</b> menampilkan semua catatan yang tersimpan di perangkat '
          'saat Mode Penyimpanan Lokal aktif.'),
        sp(4),
        step_table([
            ('1', 'Buka Data Lokal',
             'Ketuk tombol Tabel / Data Lokal di layar utama.'),
            ('2', 'Lihat daftar catatan',
             'Setiap baris menampilkan nomor urut, waktu scan, dan ID peserta.'),
            ('3', 'Unggah ke Google Sheet',
             'Ketuk tombol Unggah ke Google Sheet. Semua data dikirim sekaligus.'),
            ('4', 'Data terhapus otomatis',
             'Setelah unggah berhasil, data lokal dihapus dari perangkat secara otomatis.'),
        ]),
        sp(6),
        note('Pastikan URL Apps Script sudah diisi di Pengaturan sebelum mengunggah.'),
        sp(10),
    ]

# ── Section 7 – Pengaturan ───────────────────────────────────────────────────

def section_settings():
    return [
        h2('7.  Pengaturan'),
        hr(),
        p('Buka <b>Pengaturan</b> dari layar utama untuk mengonfigurasi aplikasi.'),
        sp(6),

        h3('7.1  Sheet (Konfigurasi Google)'),
        p('Isi dua kolom berikut:'),
        sp(2),
        step_table([
            ('🔗', 'URL Web App Apps Script',
             'Paste URL yang didapat setelah deploy Apps Script '
             '(diawali https://script.google.com/macros/...).'),
            ('📄', 'Nama Tab Sheet',
             'Ketik nama tab sheet persis seperti di Google Sheet, '
             'misalnya: Acara-1 (huruf besar/kecil harus sama).'),
        ]),
        sp(4),
        tip('Ketuk teks biru "Lihat bantuan" untuk membuka panduan konfigurasi Google langsung dari Pengaturan.'),
        sp(8),

        h3('7.2  Penyimpanan'),
        p('Toggle <b>Simpan data secara lokal</b> — aktifkan untuk beralih ke Mode Penyimpanan Lokal.'),
        sp(8),

        h3('7.3  Mode Gelap'),
        p('Toggle <b>Aktifkan mode gelap</b> — tampilan berubah seketika tanpa perlu restart.'),
        sp(8),

        h3('7.4  Bahasa'),
        p('Pilih <b>Indonesia</b> atau <b>English</b>. Aplikasi akan restart otomatis untuk menerapkan perubahan.'),
        sp(10),
    ]

# ── Section 8 – Setup Google Sheets ─────────────────────────────────────────

def section_google_setup():
    return [
        h2('8.  Persiapan Google Sheets (Satu Kali)'),
        hr(),
        p('Lakukan langkah ini sekali saja sebelum pertama kali menggunakan aplikasi. '
          'Proses ini membutuhkan sekitar 5 menit.'),
        sp(6),

        h3('Langkah 1 — Buat Google Sheet'),
        step_table([
            ('a', 'Buka sheets.google.com',
             'Buat spreadsheet baru atau gunakan yang sudah ada.'),
            ('b', 'Beri nama tab sheet',
             'Klik kanan tab di bagian bawah → Ganti nama, misalnya: Acara-1.'),
            ('c', 'Tambahkan header',
             'Di baris 1: kolom A isi "Timestamp", kolom B isi "ID".'),
        ]),
        sp(8),

        h3('Langkah 2 — Buka Apps Script'),
        p('Di Google Sheet, klik menu <b>Ekstensi → Apps Script</b>. '
          'Editor skrip akan terbuka di tab baru.'),
        sp(8),

        h3('Langkah 3 — Paste Skrip'),
        step_table([
            ('a', 'Dapatkan skrip',
             'Buka https://s.id/gappscr di browser untuk mendapatkan kode skrip.'),
            ('b', 'Hapus kode lama',
             'Hapus semua kode yang ada di editor Apps Script.'),
            ('c', 'Paste dan simpan',
             'Paste kode baru, lalu tekan Ctrl+S untuk menyimpan.'),
        ]),
        sp(8),

        h3('Langkah 4 — Deploy sebagai Web App'),
        step_table([
            ('1', 'Klik Deploy → Deployment baru',
             'Tombol Deploy ada di pojok kanan atas editor Apps Script.'),
            ('2', 'Pilih tipe Web App',
             'Klik ikon roda gigi → pilih "Web App".'),
            ('3', 'Atur izin akses',
             '"Jalankan sebagai": pilih diri sendiri (Me). '
             '"Siapa yang dapat mengakses": pilih Semua Orang (Anyone).'),
            ('4', 'Klik Deploy',
             'Izinkan akses saat diminta. Salin URL Web App yang muncul.'),
        ]),
        sp(6),
        note('URL Web App terlihat seperti: https://script.google.com/macros/s/AKfy.../exec — '
             'simpan URL ini, Anda akan membutuhkannya di langkah berikutnya.'),
        sp(8),

        h3('Langkah 5 — Konfigurasi di Aplikasi'),
        step_table([
            ('1', 'Buka Pengaturan di Attendly',
             'Ketuk tombol Pengaturan di layar utama.'),
            ('2', 'Paste URL Web App',
             'Tempel URL yang disalin ke kolom "URL Web App Apps Script".'),
            ('3', 'Isi nama tab sheet',
             'Ketik nama tab persis seperti di Google Sheet, misalnya: Acara-1.'),
            ('4', 'Kembali untuk menyimpan',
             'Tekan tombol kembali — pengaturan tersimpan otomatis.'),
        ]),
        sp(10),
    ]

# ── Section 9 – Ganti Acara ──────────────────────────────────────────────────

def section_switch_event():
    return [
        h2('9.  Ganti Acara / Tab Sheet'),
        hr(),
        p('Untuk setiap acara baru, Anda tidak perlu membuat Apps Script baru. '
          'Cukup tambahkan tab baru di Google Sheet dan ubah nama tab di Pengaturan.'),
        sp(4),
        step_table([
            ('1', 'Buka Google Sheet',
             'Tambahkan tab baru, misalnya: Acara-2. Tambahkan header Timestamp dan ID di baris 1.'),
            ('2', 'Buka Pengaturan di Attendly',
             'Ketuk tombol Pengaturan di layar utama.'),
            ('3', 'Ubah nama tab sheet',
             'Ganti isi kolom "Nama tab sheet" menjadi Acara-2.'),
            ('4', 'Kembali untuk menyimpan',
             'Tekan tombol kembali. Scan berikutnya akan masuk ke tab Acara-2.'),
        ]),
        sp(6),
        tip('URL Apps Script tidak perlu diubah — satu URL bisa digunakan untuk semua acara.'),
        sp(10),
    ]

# ── Section 10 – Troubleshooting ─────────────────────────────────────────────

def section_troubleshoot():
    data = [
        [Paragraph('<b>Masalah</b>', ParagraphStyle('th', fontSize=10, fontName='Helvetica-Bold',
                   textColor=WHITE, alignment=TA_CENTER)),
         Paragraph('<b>Solusi</b>', ParagraphStyle('th2', fontSize=10, fontName='Helvetica-Bold',
                   textColor=WHITE, alignment=TA_CENTER))],
        [p('"Sheet tab not found: Acara-1"'),
         p('Nama tab di Pengaturan harus sama persis dengan nama tab di Google Sheet (huruf besar/kecil diperhatikan).')],
        [p('Error HTTP 302 atau redirect'),
         p('Apps Script belum di-deploy dengan akses "Semua Orang". Lakukan deployment ulang.')],
        [p('"Apps Script error" di aplikasi'),
         p('Buka Apps Script → View → Executions untuk melihat log error.')],
        [p('Aplikasi meminta konfigurasi sheet'),
         p('Paste URL Web App di Pengaturan dan tekan tombol kembali untuk menyimpan.')],
        [p('Bahasa tidak berubah'),
         p('Aplikasi restart otomatis setelah ganti bahasa — tunggu beberapa detik.')],
        [p('Kamera tidak bisa membaca QR'),
         p('Pastikan pencahayaan cukup. Coba entri manual sebagai alternatif.')],
        [p('Data tidak muncul di Google Sheet'),
         p('Periksa koneksi internet. Jika menggunakan Mode Lokal, unggah data dari halaman Data Lokal.')],
    ]
    t = Table(data, colWidths=[7.5*cm, 8.5*cm])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), PRIMARY),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [ACCENT, WHITE]),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 7),
        ('BOTTOMPADDING', (0,0), (-1,-1), 7),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#90CAF9')),
        ('INNERGRID', (0,0), (-1,-1), 0.3, colors.HexColor('#BBDEFB')),
    ]))
    return [
        h2('10.  Pemecahan Masalah'),
        hr(),
        t,
        sp(10),
    ]

# ── Section 11 – Info ────────────────────────────────────────────────────────

def section_info():
    return [
        h2('11.  Informasi Aplikasi'),
        hr(),
        step_table([
            ('📋', 'Nama Aplikasi', 'Attendly'),
            ('🔢', 'Versi', '1.1.0'),
            ('📱', 'Minimum Android', '7.0 (API 24)'),
            ('📄', 'Lisensi', 'MIT License — bebas digunakan, dimodifikasi, dan didistribusikan.'),
        ]),
        sp(8),
        note('Attendly adalah proyek open source. Kode sumber tersedia secara bebas '
             'untuk dipelajari dan dikembangkan lebih lanjut.', bg=TIP_BG, border=TIP_BORDER, icon='ℹ'),
        sp(10),
    ]

# ── Page numbering ───────────────────────────────────────────────────────────

def on_page(canvas, doc):
    canvas.saveState()
    if doc.page == 1:
        # Cover page — no footer/header chrome
        canvas.restoreState()
        return
    # Footer line
    canvas.setStrokeColor(colors.HexColor('#BBDEFB'))
    canvas.setLineWidth(0.5)
    canvas.line(2*cm, 1.5*cm, A4[0]-2*cm, 1.5*cm)
    # Page number (cover is page 1, so subtract 1 for display)
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(MUTED)
    canvas.drawCentredString(A4[0]/2, 1.1*cm, f'Attendly — Panduan Pengguna  |  Halaman {doc.page - 1}')
    # App name top-right
    canvas.setFont('Helvetica-Bold', 8)
    canvas.setFillColor(PRIMARY)
    canvas.drawRightString(A4[0]-2*cm, A4[1]-1.2*cm, 'Attendly v1.1.0')
    canvas.restoreState()

# ── Build PDF ────────────────────────────────────────────────────────────────

def build():
    from reportlab.platypus import BaseDocTemplate, Frame, PageTemplate, NextPageTemplate

    # ── Frame definitions ────────────────────────────────────────────────────
    # Cover: zero margins so CoverFlowable can paint edge-to-edge
    cover_frame = Frame(0, 0, A4[0], A4[1], leftPadding=0, rightPadding=0,
                        topPadding=0, bottomPadding=0, id='cover')

    # Inner pages: 2 cm margins
    inner_frame = Frame(2*cm, 2.2*cm, A4[0]-4*cm, A4[1]-4.2*cm,
                        leftPadding=0, rightPadding=0,
                        topPadding=0, bottomPadding=0, id='inner')

    def cover_bg(canvas, doc):
        pass   # cover draws itself; no chrome needed

    def inner_bg(canvas, doc):
        on_page(canvas, doc)

    cover_tpl = PageTemplate(id='Cover', frames=[cover_frame], onPage=cover_bg)
    inner_tpl = PageTemplate(id='Inner', frames=[inner_frame], onPage=inner_bg)

    doc = BaseDocTemplate(
        OUTPUT,
        pagesize=A4,
        pageTemplates=[cover_tpl, inner_tpl],
        title='Attendly — Panduan Pengguna',
        author='Attendly',
        subject='Manual Pengguna Bahasa Indonesia',
    )

    story = []
    story += cover_page()                        # ends with PageBreak
    story.insert(len(story) - 1,                 # switch template before the PageBreak
                 NextPageTemplate('Inner'))
    story += toc_page()
    story += section_intro()
    story += section_home()
    story += section_scan()
    story += section_manual()
    story += section_storage_mode()
    story += section_local_data()
    story += section_settings()
    story += section_google_setup()
    story += section_switch_event()
    story += section_troubleshoot()
    story += section_info()

    doc.build(story)
    print(f'PDF created: {os.path.abspath(OUTPUT)}')

if __name__ == '__main__':
    build()
