from pathlib import Path
import hashlib
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate, Frame, KeepTogether, PageBreak, PageTemplate,
    Paragraph, Spacer, Table, TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.graphics.shapes import Circle, Drawing, Line, Polygon, Rect, String


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "documentation" / "Salon-Hup-Project-Documentation.md"
OUTPUT = ROOT / "output" / "pdf" / "Salon-Hup-Project-Documentation.pdf"
VERSION = "1.2.3"
UPDATED = "18 October 2026"
PAGE_WIDTH, PAGE_HEIGHT = A4

MIDNIGHT = colors.HexColor("#0B132B")
NAVY = colors.HexColor("#16213E")
BLUE = colors.HexColor("#2563EB")
CYAN = colors.HexColor("#06B6D4")
EMERALD = colors.HexColor("#10B981")
INK = colors.HexColor("#182230")
MUTED = colors.HexColor("#667085")
LINE = colors.HexColor("#D9E2EC")
SURFACE = colors.HexColor("#F5F8FC")
BLUE_TINT = colors.HexColor("#EFF6FF")
WHITE = colors.white


def register_fonts():
    candidates = {
        "DocRegular": Path("C:/Windows/Fonts/aptos.ttf"),
        "DocBold": Path("C:/Windows/Fonts/aptos-bold.ttf"),
        "DocMono": Path("C:/Windows/Fonts/consola.ttf"),
    }
    fallback = {"DocRegular": "Helvetica", "DocBold": "Helvetica-Bold", "DocMono": "Courier"}
    resolved = {}
    for name, path in candidates.items():
        if path.exists():
            pdfmetrics.registerFont(TTFont(name, str(path)))
            resolved[name] = name
        else:
            resolved[name] = fallback[name]
    return resolved


FONTS = register_fonts()


def inline(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<a href="\2" color="#2563EB"><u>\1</u></a>', text)
    text = re.sub(r"`([^`]+)`", r'<font name="%s" color="#0369A1">\1</font>' % FONTS["DocMono"], text)
    return re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="DocBody", parent=styles["BodyText"], fontName=FONTS["DocRegular"], fontSize=9.2, leading=13.6, textColor=INK, spaceAfter=7))
styles.add(ParagraphStyle(name="DocH1", parent=styles["Heading1"], fontName=FONTS["DocBold"], fontSize=18, leading=22, textColor=MIDNIGHT, spaceBefore=15, spaceAfter=9, borderColor=BLUE, borderWidth=0, borderLeftWidth=4, borderPadding=(0, 0, 0, 9), keepWithNext=True))
styles.add(ParagraphStyle(name="DocH2", parent=styles["Heading2"], fontName=FONTS["DocBold"], fontSize=12.2, leading=16, textColor=colors.HexColor("#087F8C"), spaceBefore=11, spaceAfter=6, keepWithNext=True))
styles.add(ParagraphStyle(name="DocH3", parent=styles["Heading3"], fontName=FONTS["DocBold"], fontSize=10.2, leading=14, textColor=NAVY, spaceBefore=8, spaceAfter=4, keepWithNext=True))
styles.add(ParagraphStyle(name="DocBullet", parent=styles["DocBody"], leftIndent=15, firstLineIndent=-8, bulletIndent=2, spaceAfter=4))
styles.add(ParagraphStyle(name="DocCode", parent=styles["Code"], fontName=FONTS["DocMono"], fontSize=8, leading=11, textColor=colors.HexColor("#E5EEFF"), backColor=MIDNIGHT, borderColor=NAVY, borderWidth=0.5, borderPadding=10, spaceBefore=5, spaceAfter=10))
styles.add(ParagraphStyle(name="Callout", parent=styles["DocBody"], fontSize=9, leading=13, textColor=NAVY, backColor=BLUE_TINT, borderColor=BLUE, borderWidth=0, borderLeftWidth=4, borderPadding=(8, 10, 8, 10), spaceBefore=5, spaceAfter=9))
styles.add(ParagraphStyle(name="TableHeader", fontName=FONTS["DocBold"], fontSize=7.5, leading=9.5, textColor=WHITE, alignment=TA_LEFT))
styles.add(ParagraphStyle(name="TableCell", fontName=FONTS["DocRegular"], fontSize=7.35, leading=9.5, textColor=INK))
styles.add(ParagraphStyle(name="ApiTableHeader", fontName=FONTS["DocBold"], fontSize=6.3, leading=8.1, textColor=WHITE, alignment=TA_LEFT))
styles.add(ParagraphStyle(name="ApiTableCell", fontName=FONTS["DocRegular"], fontSize=6.15, leading=8.0, textColor=INK))
styles.add(ParagraphStyle(name="ControlLabel", fontName=FONTS["DocBold"], fontSize=7.35, leading=9.5, textColor=WHITE))
styles.add(ParagraphStyle(name="TOCTitle", fontName=FONTS["DocBold"], fontSize=24, leading=29, textColor=MIDNIGHT, spaceAfter=7))
styles.add(ParagraphStyle(name="TOCLead", fontName=FONTS["DocRegular"], fontSize=9.5, leading=14, textColor=MUTED, spaceAfter=12))


class ProjectDocTemplate(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(
            filename, pagesize=A4, leftMargin=18 * mm, rightMargin=18 * mm,
            topMargin=22 * mm, bottomMargin=18 * mm,
            title="Salon Hup - Product and Engineering Specification",
            author="Salon Hup Development Team",
            subject="Production product, business, API, security, and delivery specification",
            keywords="salon, booking, hiring, spring boot, product specification",
        )
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="content", leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        self.addPageTemplates([
            PageTemplate(id="cover", frames=frame, onPage=draw_cover),
            PageTemplate(id="body", frames=frame, onPage=draw_body),
        ])

    def afterPage(self):
        if self.page == 1:
            self.handle_nextPageTemplate("body")

    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph) or flowable.style.name not in {"DocH1", "DocH2"}:
            return
        level = 0 if flowable.style.name == "DocH1" else 1
        heading = flowable.getPlainText()
        digest = hashlib.sha1(f"{level}:{heading}".encode("utf-8")).hexdigest()[:12]
        key = f"heading-{digest}"
        self.canv.bookmarkPage(key)
        if level == 0:
            self.canv.addOutlineEntry(heading, key, level=0, closed=False)
        self.notify("TOCEntry", (level, heading, self.page, key))


def draw_cover(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(MIDNIGHT)
    canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, stroke=0, fill=1)
    canvas.setFillColor(BLUE)
    canvas.rect(0, PAGE_HEIGHT - 9 * mm, PAGE_WIDTH, 9 * mm, stroke=0, fill=1)
    canvas.setFillColor(CYAN)
    canvas.rect(0, PAGE_HEIGHT - 9 * mm, PAGE_WIDTH * 0.38, 9 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.HexColor("#122451"))
    canvas.circle(PAGE_WIDTH + 6 * mm, PAGE_HEIGHT - 54 * mm, 54 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.HexColor("#173B78"))
    canvas.circle(PAGE_WIDTH - 12 * mm, PAGE_HEIGHT - 42 * mm, 32 * mm, stroke=0, fill=1)
    canvas.setFillColor(CYAN)
    canvas.circle(PAGE_WIDTH - 7 * mm, PAGE_HEIGHT - 28 * mm, 12 * mm, stroke=0, fill=1)
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 10)
    canvas.drawString(24 * mm, PAGE_HEIGHT - 34 * mm, "SALON HUP")
    canvas.setFillColor(colors.HexColor("#91A4C4"))
    canvas.setFont(FONTS["DocRegular"], 8)
    canvas.drawString(24 * mm, PAGE_HEIGHT - 41 * mm, "PRODUCT  /  PLATFORM  /  OPERATIONS")
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 32)
    canvas.drawString(24 * mm, PAGE_HEIGHT - 92 * mm, "Product & Engineering")
    canvas.drawString(24 * mm, PAGE_HEIGHT - 106 * mm, "Specification")
    canvas.setFillColor(colors.HexColor("#B8C5D9"))
    canvas.setFont(FONTS["DocRegular"], 13)
    canvas.drawString(24 * mm, PAGE_HEIGHT - 122 * mm, "Business rules, API contracts, architecture,")
    canvas.drawString(24 * mm, PAGE_HEIGHT - 130 * mm, "security, delivery plan, and production readiness")
    canvas.setFillColor(BLUE)
    canvas.roundRect(24 * mm, PAGE_HEIGHT - 153 * mm, 35 * mm, 9 * mm, 4 * mm, stroke=0, fill=1)
    canvas.setFillColor(EMERALD)
    canvas.roundRect(63 * mm, PAGE_HEIGHT - 153 * mm, 45 * mm, 9 * mm, 4 * mm, stroke=0, fill=1)
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 7.5)
    canvas.drawCentredString(41.5 * mm, PAGE_HEIGHT - 150.2 * mm, f"VERSION {VERSION}")
    canvas.drawCentredString(85.5 * mm, PAGE_HEIGHT - 150.2 * mm, "ACTIVE DEVELOPMENT")
    canvas.setFillColor(colors.HexColor("#8FA3C4"))
    canvas.setFont(FONTS["DocRegular"], 8.5)
    canvas.drawString(24 * mm, 41 * mm, "Prepared for product, engineering, QA, and deployment review")
    canvas.drawString(24 * mm, 34 * mm, f"Last updated  {UPDATED}")
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 8.5)
    canvas.drawRightString(PAGE_WIDTH - 20 * mm, 18 * mm, "CONFIDENTIAL - INTERNAL WORKING DOCUMENT")
    canvas.restoreState()


def draw_body(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(MIDNIGHT)
    canvas.rect(0, PAGE_HEIGHT - 5 * mm, PAGE_WIDTH, 5 * mm, stroke=0, fill=1)
    canvas.setFillColor(CYAN)
    canvas.rect(0, PAGE_HEIGHT - 5 * mm, 42 * mm, 5 * mm, stroke=0, fill=1)
    canvas.setFont(FONTS["DocBold"], 7.2)
    canvas.setFillColor(NAVY)
    canvas.drawString(doc.leftMargin, PAGE_HEIGHT - 13 * mm, "SALON HUP")
    canvas.setFont(FONTS["DocRegular"], 7.2)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(PAGE_WIDTH - doc.rightMargin, PAGE_HEIGHT - 13 * mm, "PRODUCT & ENGINEERING SPECIFICATION")
    canvas.setStrokeColor(LINE)
    canvas.line(doc.leftMargin, 12 * mm, PAGE_WIDTH - doc.rightMargin, 12 * mm)
    canvas.setFont(FONTS["DocRegular"], 7.2)
    canvas.drawString(doc.leftMargin, 7.5 * mm, f"VERSION {VERSION}  |  ACTIVE DEVELOPMENT")
    canvas.drawRightString(PAGE_WIDTH - doc.rightMargin, 7.5 * mm, f"{doc.page:02d}")
    canvas.restoreState()


def column_widths(rows, width):
    columns = len(rows[0])
    header = [cell.lower() for cell in rows[0]]
    if columns == 2:
        return [width * 0.30, width * 0.70]
    if columns == 3 and header[:2] == ["method", "route"]:
        return [width * 0.12, width * 0.36, width * 0.52]
    if columns == 3:
        return [width * 0.20, width * 0.29, width * 0.51]
    if columns == 4 and header[:2] == ["method", "route"]:
        return [width * 0.10, width * 0.28, width * 0.47, width * 0.15]
    if columns == 4:
        return [width * 0.15, width * 0.24, width * 0.45, width * 0.16]
    if columns == 5 and header[0] == "endpoint":
        return [width * 0.20, width * 0.12, width * 0.24, width * 0.20, width * 0.24]
    if columns == 5:
        return [width * 0.13, width * 0.19, width * 0.37, width * 0.19, width * 0.12]
    if columns == 6:
        return [width * 0.20, width * 0.16, width * 0.16, width * 0.16, width * 0.16, width * 0.16]
    return [width / columns] * columns


def make_table(rows, width):
    is_api_contract = len(rows[0]) == 5 and rows[0][0].lower() == "endpoint"
    converted = []
    for row_index, row in enumerate(rows):
        if is_api_contract:
            style = styles["ApiTableHeader"] if row_index == 0 else styles["ApiTableCell"]
        else:
            style = styles["TableHeader"] if row_index == 0 else styles["TableCell"]
        converted.append([Paragraph(inline(cell), style) for cell in row])
    table = Table(converted, colWidths=column_widths(rows, width), repeatRows=1, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, SURFACE]),
        ("LINEBELOW", (0, 0), (-1, 0), 1.2, CYAN),
        ("GRID", (0, 1), (-1, -1), 0.3, LINE),
        ("BOX", (0, 0), (-1, -1), 0.5, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


def document_control_table(width):
    rows = [
        ["Document", "Salon Hup Product & Engineering Specification"],
        ["Version", VERSION],
        ["Lifecycle", "Active development"],
        ["System", "React 19 web client + Spring Boot 4.1.0 API / Java 17"],
        ["Audience", "Product owner, engineering, QA, operations, and reviewers"],
        ["Source of truth", "Implemented frontend and backend behavior, integration contracts, and approved plans"],
    ]
    converted = [
        [Paragraph(inline(row[0]), styles["ControlLabel"]), Paragraph(inline(row[1]), styles["TableCell"])]
        for row in rows
    ]
    table = Table(converted, colWidths=[width * 0.25, width * 0.75])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), NAVY), ("TEXTCOLOR", (0, 0), (0, -1), WHITE),
        ("FONTNAME", (0, 0), (0, -1), FONTS["DocBold"]),
        ("ROWBACKGROUNDS", (1, 0), (1, -1), [WHITE, SURFACE]),
        ("GRID", (0, 0), (-1, -1), 0.4, LINE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return table


def make_toc():
    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(name="TOCLevel1", fontName=FONTS["DocBold"], fontSize=9.5, leading=14, textColor=NAVY, leftIndent=0, firstLineIndent=0, spaceBefore=5),
        ParagraphStyle(name="TOCLevel2", fontName=FONTS["DocRegular"], fontSize=8.3, leading=12, textColor=MUTED, leftIndent=12, firstLineIndent=0),
    ]
    return toc


def add_box(drawing, x, y, width, height, title, lines=(), fill=WHITE, stroke=LINE, title_color=NAVY):
    drawing.add(Rect(x, y, width, height, rx=7, ry=7, fillColor=fill, strokeColor=stroke, strokeWidth=1))
    drawing.add(String(x + width / 2, y + height - 15, title, fontName=FONTS["DocBold"], fontSize=8.2,
                       fillColor=title_color, textAnchor="middle"))
    line_y = y + height - 29
    line_color = colors.HexColor("#C8D4E8") if fill == MIDNIGHT else MUTED
    for line in lines:
        drawing.add(String(x + width / 2, line_y, line, fontName=FONTS["DocRegular"], fontSize=6.7,
                           fillColor=line_color, textAnchor="middle"))
        line_y -= 10


def add_arrow(drawing, x1, y1, x2, y2, label=None, color=BLUE):
    drawing.add(Line(x1, y1, x2, y2, strokeColor=color, strokeWidth=1.4))
    direction = 1 if x2 >= x1 else -1
    drawing.add(Polygon([
        x2, y2,
        x2 - direction * 7, y2 + 3.5,
        x2 - direction * 7, y2 - 3.5,
    ], fillColor=color, strokeColor=color))
    if label:
        drawing.add(String((x1 + x2) / 2, y1 + 6, label, fontName=FONTS["DocRegular"], fontSize=6.2,
                           fillColor=MUTED, textAnchor="middle"))


def add_actor(drawing, x, y, label):
    drawing.add(Circle(x, y + 29, 7, fillColor=BLUE_TINT, strokeColor=BLUE, strokeWidth=1.2))
    drawing.add(Line(x, y + 22, x, y + 3, strokeColor=NAVY, strokeWidth=1.2))
    drawing.add(Line(x - 10, y + 15, x + 10, y + 15, strokeColor=NAVY, strokeWidth=1.2))
    drawing.add(Line(x, y + 3, x - 8, y - 9, strokeColor=NAVY, strokeWidth=1.2))
    drawing.add(Line(x, y + 3, x + 8, y - 9, strokeColor=NAVY, strokeWidth=1.2))
    drawing.add(String(x, y - 22, label, fontName=FONTS["DocBold"], fontSize=6.7,
                       fillColor=NAVY, textAnchor="middle"))


def system_context_diagram(width):
    height = 245
    d = Drawing(width, height)
    actor_x = 36
    for y, label in [(175, "Customer"), (100, "Salon team"), (25, "Administrator")]:
        add_actor(d, actor_x, y, label)
    add_box(d, 88, 82, 105, 88, "React web client", ["Discovery and profiles", "Booking and reviews", "Owner workspace"], BLUE_TINT, BLUE)
    add_box(d, 225, 82, 118, 88, "Salon Hup API", ["JWT authorization", "Business workflows", "ApiResponse DTOs"], colors.HexColor("#ECFDF3"), EMERALD)
    add_box(d, 382, 142, 95, 58, "Database", ["MySQL target", "H2 local and test"], SURFACE, NAVY)
    add_box(d, 382, 52, 95, 58, "SMTP provider", ["Account email", "Recovery delivery"], SURFACE, NAVY)
    add_arrow(d, 193, 126, 225, 126, "HTTPS JSON")
    add_arrow(d, 343, 145, 382, 166, "JPA")
    add_arrow(d, 343, 105, 382, 81, "SMTP")
    for y in (204, 129, 54):
        add_arrow(d, 57, y, 88, 135 if y == 129 else (151 if y > 129 else 101), None, CYAN)
    return d


def use_case_diagram(width):
    height = 285
    d = Drawing(width, height)
    d.add(Rect(88, 15, width - 110, height - 30, rx=10, ry=10, fillColor=colors.HexColor("#FAFCFF"),
               strokeColor=BLUE, strokeWidth=1.2))
    d.add(String(104, height - 34, "SALON HUP USE CASES", fontName=FONTS["DocBold"], fontSize=8,
                 fillColor=BLUE))
    actors = [(30, 220, "Customer"), (30, 135, "Career user"), (30, 50, "Employee"),
              (width - 27, 205, "Owner"), (width - 27, 75, "Admin")]
    for x, y, label in actors:
        add_actor(d, x, y, label)
    cases = [
        (110, 205, 120, 38, "Discover salons", ["View active services"]),
        (110, 145, 120, 38, "Manage profile", ["Enable career mode"]),
        (110, 85, 120, 38, "Employment", ["Join, leave, transfer"]),
        (254, 205, 120, 38, "Manage salon", ["Profile and services"]),
        (254, 145, 120, 38, "Recruit team", ["Post, rank, decide"]),
        (254, 85, 120, 38, "Platform control", ["Approve, block, restore"]),
        (182, 30, 120, 38, "Book and review", ["Verified visit workflow"]),
    ]
    for x, y, w, h, title, lines in cases:
        fill = SURFACE if "Planned" in lines[0] else WHITE
        add_box(d, x, y, w, h, title, lines, fill, LINE)
    add_arrow(d, 51, 248, 110, 224, None, CYAN)
    add_arrow(d, 51, 163, 110, 164, None, CYAN)
    add_arrow(d, 51, 78, 110, 104, None, CYAN)
    add_arrow(d, width - 48, 233, 374, 224, None, EMERALD)
    add_arrow(d, width - 48, 207, 374, 164, None, EMERALD)
    add_arrow(d, width - 48, 103, 374, 104, None, EMERALD)
    return d


def component_diagram(width):
    height = 245
    d = Drawing(width, height)
    boxes = [
        (8, 172, 88, 48, "React features", ["Hooks and API adapters"]),
        (112, 172, 88, 48, "HTTP boundary", ["CORS and JWT filter"]),
        (216, 172, 88, 48, "Controllers", ["Binding and principal"]),
        (320, 172, 88, 48, "DTO contract", ["ApiResponse envelope"]),
        (112, 86, 120, 52, "Domain services", ["Validation", "Authorization and state"]),
        (264, 86, 120, 52, "Repositories", ["Queries and locking", "JPA persistence"]),
        (188, 12, 120, 46, "Relational database", ["MySQL / compatible H2"]),
    ]
    for x, y, w, h, title, lines in boxes:
        fill = BLUE_TINT if y > 150 else colors.HexColor("#ECFDF3") if y > 60 else SURFACE
        add_box(d, x, y, w, h, title, lines, fill, BLUE if y > 150 else EMERALD)
    add_arrow(d, 96, 196, 112, 196)
    add_arrow(d, 200, 196, 216, 196)
    add_arrow(d, 304, 196, 320, 196)
    add_arrow(d, 276, 172, 188, 138, "delegates")
    add_arrow(d, 232, 112, 264, 112)
    add_arrow(d, 324, 86, 270, 58, "entities")
    return d


def frontend_integration_diagram(width):
    height = 265
    d = Drawing(width, height)
    boxes = [
        (4, 178, 82, 58, "React route", ["Lazy page", "Protected navigation"]),
        (98, 178, 82, 58, "Feature hook", ["Form and state", "Toast feedback"]),
        (192, 178, 82, 58, "API adapter", ["Endpoint mapping", "DTO normalization"]),
        (286, 178, 82, 58, "Axios client", ["/api base", "JWT and timeout"]),
        (380, 178, 82, 58, "Spring API", ["Security filter", "Controller DTO"]),
    ]
    for x, y, w, h, title, lines in boxes:
        add_box(d, x, y, w, h, title, lines, BLUE_TINT, BLUE)
    for index in range(len(boxes) - 1):
        x, y, w, h, _, _ = boxes[index]
        next_x = boxes[index + 1][0]
        add_arrow(d, x + w, y + h / 2, next_x, y + h / 2, None, CYAN)

    add_box(d, 112, 72, 120, 62, "Domain services", ["Business validation", "Authorization and state"], colors.HexColor("#ECFDF3"), EMERALD)
    add_box(d, 270, 72, 120, 62, "Persistence", ["Repositories and locks", "MySQL / H2"], colors.HexColor("#ECFDF3"), EMERALD)
    add_arrow(d, 421, 178, 232, 134, "delegates", EMERALD)
    add_arrow(d, 232, 103, 270, 103, "JPA", EMERALD)
    d.add(Line(421, 178, 421, 34, strokeColor=BLUE, strokeWidth=1.4))
    add_arrow(d, 421, 34, 139, 34, "ApiResponse success / error", BLUE)
    add_arrow(d, 139, 34, 45, 178, "render", CYAN)
    d.add(String(width / 2, 9, "Local: Vite /api proxy -> localhost:8080. Production: same-origin reverse proxy or configured HTTPS API.",
                 fontName=FONTS["DocRegular"], fontSize=6.8, fillColor=MUTED, textAnchor="middle"))
    return d


def sequence_diagram(width, participants, messages):
    height = 42 + len(messages) * 24 + 38
    d = Drawing(width, height)
    margin = 24
    step = (width - 2 * margin) / (len(participants) - 1)
    xs = [margin + index * step for index in range(len(participants))]
    top = height - 25
    for x, participant in zip(xs, participants):
        d.add(Rect(x - 38, top - 13, 76, 26, rx=5, ry=5, fillColor=BLUE_TINT, strokeColor=BLUE))
        d.add(String(x, top - 3, participant, fontName=FONTS["DocBold"], fontSize=6.7,
                     fillColor=NAVY, textAnchor="middle"))
        life = Line(x, top - 13, x, 20, strokeColor=LINE, strokeWidth=0.8)
        life.strokeDashArray = [3, 3]
        d.add(life)
    y = top - 36
    for source, target, label, success in messages:
        color = EMERALD if success else BLUE
        if source == target:
            x = xs[source]
            d.add(Line(x, y, x + 26, y, strokeColor=color, strokeWidth=1.4))
            d.add(Line(x + 26, y, x + 26, y - 10, strokeColor=color, strokeWidth=1.4))
            add_arrow(d, x + 26, y - 10, x, y - 10, None, color)
            label_x = x + 32
        else:
            add_arrow(d, xs[source], y, xs[target], y, None, color)
            label_x = (xs[source] + xs[target]) / 2
        d.add(String(label_x, y + 6, label, fontName=FONTS["DocRegular"],
                     fontSize=6.1, fillColor=NAVY, textAnchor="middle"))
        y -= 24
    return d


def hiring_sequence_diagram(width):
    return sequence_diagram(width, ["Applicant", "Hiring API", "Profile", "Hiring service", "Employment"], [
        (0, 1, "Submit optional note", False),
        (1, 2, "Load career profile", False),
        (2, 3, "Return trusted profile data", True),
        (3, 3, "Validate, snapshot, rank", False),
        (3, 0, "Application and match result", True),
        (0, 1, "Owner accepts application", False),
        (1, 4, "Execute join or transfer", False),
        (4, 0, "Accepted or pending leave", True),
    ])


def service_sequence_diagram(width):
    return sequence_diagram(width, ["Owner", "Service API", "Domain service", "Repository", "Public client"], [
        (0, 1, "Create or update DTO", False),
        (1, 2, "Authenticated owner ID", False),
        (2, 2, "Validate salon and fields", False),
        (2, 3, "Save active service", False),
        (3, 0, "Return response DTO", True),
        (4, 1, "List salon services", False),
        (1, 2, "Verify accepted salon", False),
        (2, 3, "Query active only", False),
        (3, 4, "Ordered public DTOs", True),
        (0, 1, "Disable service", False),
        (2, 3, "Persist active=false", True),
    ])


def availability_flow_diagram(width):
    height = 270
    d = Drawing(width, height)
    box_width = 132
    left_x = 14
    right_x = width - box_width - 14
    center_x = width / 2 - box_width / 2
    add_box(d, left_x, 184, box_width, 58, "Request", ["Salon + services + date", "Optional employee"], BLUE_TINT, BLUE)
    add_box(d, center_x, 184, box_width, 58, "Duration and grid", ["Sum service durations", "Start every 15 minutes"], BLUE_TINT, BLUE)
    add_box(d, right_x, 184, box_width, 58, "Salon window", ["Accepted and unblocked", "Full duration fits"], BLUE_TINT, BLUE)
    add_box(d, right_x, 96, box_width, 58, "Employee eligibility", ["Active + all capabilities", "Schedule covers interval"], BLUE_TINT, BLUE)
    add_box(d, center_x, 96, box_width, 58, "Capacity", ["No accepted overlap", "Pending does not reserve"], BLUE_TINT, BLUE)
    add_box(d, left_x, 96, box_width, 58, "Lead time", ["Same day starts at least", "30 minutes from now"], BLUE_TINT, BLUE)
    add_box(d, width / 2 - 96, 22, 192, 48, "Available slot", ["Time + every eligible employee", "Revalidated during creation"], MIDNIGHT, MIDNIGHT, WHITE)
    add_arrow(d, left_x + box_width, 213, center_x, 213, "validate", EMERALD)
    add_arrow(d, center_x + box_width, 213, right_x, 213, "generate", EMERALD)
    add_arrow(d, right_x + box_width / 2, 184, right_x + box_width / 2, 154, None, EMERALD)
    add_arrow(d, right_x, 125, center_x + box_width, 125, None, EMERALD)
    add_arrow(d, center_x, 125, left_x + box_width, 125, None, EMERALD)
    add_arrow(d, left_x + box_width / 2, 96, width / 2, 70, None, EMERALD)
    d.add(String(width / 2, 7, "Invalid input returns ApiResponse.error; a valid request may return an empty slot list.",
                 fontName=FONTS["DocRegular"], fontSize=7, fillColor=MUTED, textAnchor="middle"))
    return d


def application_flow_diagram(width):
    height = 600
    d = Drawing(width, height)
    margin = 4
    lane_gap = 10
    lane_width = (width - margin * 2 - lane_gap * 2) / 3
    lane_xs = [margin + index * (lane_width + lane_gap) for index in range(3)]
    lane_top = height - 8
    lane_bottom = 10
    lane_height = lane_top - lane_bottom
    lane_colors = [BLUE_TINT, colors.HexColor("#ECFDF3"), SURFACE]
    lane_strokes = [BLUE, EMERALD, NAVY]
    lane_titles = ["CUSTOMER", "SALON OWNER", "CAREER + EMPLOYEE"]

    for x, fill, stroke, title in zip(lane_xs, lane_colors, lane_strokes, lane_titles):
        d.add(Rect(x, lane_bottom, lane_width, lane_height, rx=9, ry=9,
                   fillColor=fill, strokeColor=stroke, strokeWidth=1))
        d.add(String(x + lane_width / 2, lane_top - 17, title,
                     fontName=FONTS["DocBold"], fontSize=7.5,
                     fillColor=stroke, textAnchor="middle"))

    box_height = 40
    step = 48
    first_y = 530
    customer_steps = [
        ("Home and discovery", ["Explore salons"]),
        ("Browse salons", ["Choose a salon"]),
        ("Salon profile", ["Services and customer reviews"]),
        ("Sign in or register", ["Required before booking"]),
        ("Build a booking", ["Services + date", "Optional employee"]),
        ("Choose available time", ["Server slots + qualified staff", "15-minute interval"]),
        ("Send booking request", ["Request is PENDING"]),
        ("See the salon decision", ["Accepted or declined"]),
        ("See provider updates", ["Replacement employee + note"]),
        ("Attend appointment", ["Go to the salon"]),
        ("Rate the salon", ["After the owner marks it complete"]),
    ]
    owner_steps = [
        ("Owner dashboard", ["Manage the salon"]),
        ("Salon profile", ["Create and maintain details"]),
        ("Hiring", ["Posts and applications"]),
        ("Service catalog", ["Prices and durations"]),
        ("Employee setup", ["Services + weekly hours"]),
        ("Booking inbox", ["Review customer requests"]),
        ("Decide booking", ["Accept or decline"]),
        ("Reassign if unavailable", ["Choose qualified staff", "Add a note for the customer"]),
        ("Complete appointment", ["After the service is done"]),
    ]
    career_employee_steps = [
        ("Career profile", ["Set up professional details"]),
        ("Browse hiring posts", ["Find relevant opportunities"]),
        ("Apply", ["Send application to salon"]),
        ("Track applications", ["See application status"]),
        ("Owner decision", ["Application is reviewed"]),
        ("Employee account", ["Sign in as an employee"]),
        ("My bookings", ["View assigned appointments"]),
        ("Appointment details", ["Check date, time, services"]),
    ]

    def draw_vertical_steps(lane_index, steps, first_node_y=first_y):
        x = lane_xs[lane_index]
        center_x = x + lane_width / 2
        node_width = lane_width - 14
        node_x = x + 7
        node_ys = [first_node_y - index * step for index in range(len(steps))]
        for index, ((title, lines), y) in enumerate(zip(steps, node_ys)):
            add_box(d, node_x, y, node_width, box_height, title, lines,
                    WHITE, lane_strokes[lane_index])
            if index < len(node_ys) - 1:
                next_top = node_ys[index + 1] + box_height
                d.add(Line(center_x, y, center_x, next_top,
                           strokeColor=lane_strokes[lane_index], strokeWidth=1.2))
                d.add(Polygon([
                    center_x, next_top,
                    center_x - 3.5, next_top + 6,
                    center_x + 3.5, next_top + 6,
                ], fillColor=lane_strokes[lane_index], strokeColor=lane_strokes[lane_index]))
        return node_ys

    customer_ys = draw_vertical_steps(0, customer_steps)
    owner_ys = draw_vertical_steps(1, owner_steps)
    career_ys = draw_vertical_steps(2, career_employee_steps[:5])
    employee_ys = draw_vertical_steps(2, career_employee_steps[5:], first_node_y=230)
    career_lane_x = lane_xs[2]
    d.add(Line(career_lane_x + 8, 300, career_lane_x + lane_width - 8, 300,
               strokeColor=LINE, strokeWidth=0.8))
    d.add(String(career_lane_x + lane_width / 2, 283, "EMPLOYEE",
                 fontName=FONTS["DocBold"], fontSize=6.3,
                 fillColor=NAVY, textAnchor="middle"))

    def cross_arrow(source_lane, source_y, target_lane, target_y, label, color):
        source_x = lane_xs[source_lane] + (lane_width - 7 if target_lane > source_lane else 7)
        target_x = lane_xs[target_lane] + (7 if target_lane > source_lane else lane_width - 7)
        start_y = source_y + box_height / 2
        end_y = target_y + box_height / 2
        add_arrow(d, source_x, start_y, target_x, end_y, label, color)

    # Customer requests enter the owner queue; the decision and any replacement
    # update flow back to the customer's booking page.
    cross_arrow(0, customer_ys[6], 1, owner_ys[5], None, BLUE)
    cross_arrow(1, owner_ys[6], 0, customer_ys[7], None, EMERALD)
    cross_arrow(1, owner_ys[7], 0, customer_ys[8], None, EMERALD)
    cross_arrow(2, career_ys[2], 1, owner_ys[2], None, CYAN)

    owner_complete_x = lane_xs[1] + 7
    between_lanes_x = lane_xs[0] + lane_width + lane_gap / 2
    customer_review_x = lane_xs[0] + lane_width - 7
    owner_complete_y = owner_ys[8] + box_height / 2
    customer_review_y = customer_ys[10] + box_height / 2
    d.add(Line(owner_complete_x, owner_complete_y, between_lanes_x, owner_complete_y,
               strokeColor=EMERALD, strokeWidth=1.2))
    d.add(Line(between_lanes_x, owner_complete_y, between_lanes_x, customer_review_y,
               strokeColor=EMERALD, strokeWidth=1.2))
    add_arrow(d, between_lanes_x, customer_review_y, customer_review_x,
              customer_review_y, None, EMERALD)

    d.add(String(width / 2, 2,
                 "Employee breaks are handled as needed. Admin tools are backend-only; payments and separate employee/service ratings are planned.",
                 fontName=FONTS["DocRegular"], fontSize=5.7,
                 fillColor=MUTED, textAnchor="middle"))
    return d


def booking_sequence_diagram(width):
    return sequence_diagram(width, ["Customer", "Booking API", "Catalog", "Availability", "Owner"], [
        (0, 1, "Request availability", False),
        (1, 2, "Load active salon services", False),
        (2, 1, "Price and duration snapshots", True),
        (1, 3, "Generate qualified free slots", False),
        (3, 0, "Times and eligible employees", True),
        (0, 1, "Create with selected slot", False),
        (1, 3, "Revalidate current availability", False),
        (3, 1, "Selected slot remains valid", True),
        (1, 0, "Return pending booking", True),
        (4, 1, "Accept, reject, or assign", False),
        (1, 3, "Recheck capability and schedule", False),
        (3, 4, "Decision is valid", True),
        (4, 1, "Complete accepted visit", False),
        (1, 0, "Completed booking", True),
    ])


def booking_lifecycle_diagram(width):
    height = 245
    d = Drawing(width, height)
    add_box(d, width / 2 - 55, 170, 110, 48, "PENDING", ["Customer request"], BLUE_TINT, BLUE)
    add_box(d, 28, 88, 110, 48, "REJECTED", ["Owner reason required", "Terminal state"], SURFACE, NAVY)
    add_box(d, width / 2 - 55, 88, 110, 48, "ACCEPTED", ["Owner confirmation", "Employee optional"], colors.HexColor("#ECFDF3"), EMERALD)
    add_box(d, width - 138, 88, 110, 48, "CANCELLED", ["Customer action", "Terminal state"], SURFACE, NAVY)
    add_box(d, width / 2 - 55, 8, 110, 48, "COMPLETED", ["Owner after visit", "Review eligible"], MIDNIGHT, MIDNIGHT, WHITE)
    add_arrow(d, width / 2 - 38, 170, 83, 136, "owner rejects", BLUE)
    add_arrow(d, width / 2, 170, width / 2, 136, "owner accepts", EMERALD)
    add_arrow(d, width / 2 + 38, 170, width - 83, 136, "customer cancels", BLUE)
    add_arrow(d, width / 2, 88, width / 2, 56, "owner completes", EMERALD)
    add_arrow(d, width / 2 + 55, 112, width - 138, 112, "customer cancels", BLUE)
    return d


def review_sequence_diagram(width):
    return sequence_diagram(width, ["Customer", "Review API", "Booking", "Review store", "Public client"], [
        (0, 1, "Submit booking, rating, comment", False),
        (1, 2, "Lock and verify owned completion", False),
        (2, 1, "Eligible booking", True),
        (1, 3, "Enforce one review per booking", False),
        (3, 0, "Return verified salon review", True),
        (4, 1, "Request salon reviews", False),
        (1, 3, "Filter active customers", False),
        (3, 4, "List and aggregate summary", True),
    ])


def notification_sequence_diagram(width):
    return sequence_diagram(width, ["Salon action", "Booking service", "Notification", "Database", "Signed-in user"], [
        (0, 1, "Accept, reject, reassign, cancel", False),
        (1, 1, "Lock and validate booking state", False),
        (1, 3, "Persist booking transition", False),
        (1, 2, "Publish event in same transaction", False),
        (2, 2, "Build recipient from domain data", False),
        (2, 3, "Save unique event key + snapshot", True),
        (3, 1, "Commit booking and notification", True),
        (4, 2, "Poll authenticated inbox", False),
        (2, 3, "Query recipient newest first", False),
        (3, 4, "ApiResponse with page DTO", True),
        (4, 2, "Mark one or all as read", False),
        (2, 3, "Update owned records only", True),
    ])


def benefits_diagram(width):
    height = 245
    d = Drawing(width, height)
    center_x = width / 2
    add_box(d, center_x - 62, 94, 124, 58, "SALON HUP", ["One trusted platform", "One consistent identity"], MIDNIGHT, MIDNIGHT, WHITE)
    benefits = [
        (8, 170, 145, 55, "Customers", ["Book qualified staff", "Share verified feedback"]),
        (width - 153, 170, 145, 55, "Salon owners", ["Control services and staff", "Manage appointment demand"]),
        (8, 18, 145, 55, "Career users", ["Reusable career profile", "Fair visible matching"]),
        (width - 153, 18, 145, 55, "Platform operations", ["Approval and moderation", "Restorable restrictions"]),
    ]
    for x, y, w, h, title, lines in benefits:
        add_box(d, x, y, w, h, title, lines, BLUE_TINT, BLUE)
        target_x = x + w if x < center_x else x
        source_x = center_x - 62 if x < center_x else center_x + 62
        target_y = y + h / 2
        source_y = 123
        add_arrow(d, source_x, source_y, target_x, target_y, None, CYAN)
    return d


def diagram_for(name, width):
    diagrams = {
        "system-context": system_context_diagram,
        "use-cases": use_case_diagram,
        "components": component_diagram,
        "frontend-integration": frontend_integration_diagram,
        "hiring-sequence": hiring_sequence_diagram,
        "service-sequence": service_sequence_diagram,
        "availability-flow": availability_flow_diagram,
        "application-flow": application_flow_diagram,
        "booking-sequence": booking_sequence_diagram,
        "booking-lifecycle": booking_lifecycle_diagram,
        "review-sequence": review_sequence_diagram,
        "notification-sequence": notification_sequence_diagram,
        "benefits": benefits_diagram,
    }
    builder = diagrams.get(name)
    if builder is None:
        raise ValueError(f"Unknown diagram: {name}")
    return KeepTogether([builder(width), Spacer(1, 10)])


def parse_markdown(text, width):
    lines = text.splitlines()
    story = [
        Spacer(1, 205 * mm), PageBreak(),
        Paragraph("Document control", styles["TOCTitle"]),
        Paragraph("A maintained decision record for the current backend and the work required to reach production.", styles["TOCLead"]),
        document_control_table(width), Spacer(1, 9 * mm),
        Paragraph("Contents", styles["TOCTitle"]),
        Paragraph("Sections and subsections are linked in supported PDF readers.", styles["TOCLead"]),
        make_toc(), PageBreak(),
    ]
    index = 0
    in_code = False
    code_lines = []
    while index < len(lines):
        raw = lines[index].rstrip()
        stripped = raw.strip()
        if stripped.startswith("# ") or (stripped.startswith("**") and ":**" in stripped):
            index += 1
            continue
        if stripped.startswith("```") and not in_code:
            in_code, code_lines = True, []
            index += 1
            continue
        if stripped == "```" and in_code:
            story.append(Paragraph("<br/>".join(inline(line).replace(" ", "&nbsp;") for line in code_lines), styles["DocCode"]))
            in_code = False
            index += 1
            continue
        if in_code:
            code_lines.append(raw)
            index += 1
            continue
        if stripped.startswith("## "):
            story.append(Paragraph(inline(stripped[3:]), styles["DocH1"]))
        elif stripped.startswith("### "):
            story.append(Paragraph(inline(stripped[4:]), styles["DocH2"]))
        elif stripped.startswith("#### "):
            story.append(Paragraph(inline(stripped[5:]), styles["DocH3"]))
        elif stripped.startswith("> "):
            story.append(Paragraph(inline(stripped[2:]), styles["Callout"]))
        elif re.fullmatch(r"\[\[DIAGRAM:[a-z-]+\]\]", stripped):
            story.append(diagram_for(stripped[10:-2], width))
        elif stripped == "[[PAGEBREAK]]":
            story.append(PageBreak())
        elif stripped.startswith("| "):
            table_lines = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                table_lines.append(lines[index].strip())
                index += 1
            rows = []
            for table_line in table_lines:
                cells = [cell.strip() for cell in table_line.strip("|").split("|")]
                if all(re.fullmatch(r"[-: ]+", cell) for cell in cells):
                    continue
                rows.append(cells)
            if rows:
                block = [make_table(rows, width), Spacer(1, 8)]
                story.append(KeepTogether(block)) if len(rows) <= 6 else story.extend(block)
            continue
        elif re.match(r"^\d+\. ", stripped):
            number, body = stripped.split(". ", 1)
            story.append(Paragraph(inline(body), styles["DocBullet"], bulletText=number + "."))
        elif stripped.startswith("- "):
            story.append(Paragraph(inline(stripped[2:]), styles["DocBullet"], bulletText="•"))
        elif stripped:
            story.append(Paragraph(inline(stripped), styles["DocBody"]))
        index += 1
    return story


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = ProjectDocTemplate(str(OUTPUT))
    doc.multiBuild(parse_markdown(SOURCE.read_text(encoding="utf-8"), doc.width))
    print(OUTPUT)


if __name__ == "__main__":
    build()
