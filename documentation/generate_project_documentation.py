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


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "documentation" / "Salon-Hup-Project-Documentation.md"
OUTPUT = ROOT / "output" / "pdf" / "Salon-Hup-Project-Documentation.pdf"
VERSION = "1.0.0"
UPDATED = "29 September 2026"
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
    if columns == 5:
        return [width * 0.13, width * 0.19, width * 0.37, width * 0.19, width * 0.12]
    if columns == 6:
        return [width * 0.20, width * 0.16, width * 0.16, width * 0.16, width * 0.16, width * 0.16]
    return [width / columns] * columns


def make_table(rows, width):
    converted = []
    for row_index, row in enumerate(rows):
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
        ["System", "Backend API - Spring Boot 4.1.0 / Java 17"],
        ["Audience", "Product owner, engineering, QA, operations, and reviewers"],
        ["Source of truth", "Approved behavior implemented in the backend repository"],
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
