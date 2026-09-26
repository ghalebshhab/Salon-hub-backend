from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    KeepTogether,
)


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "documentation" / "Salon-Hup-Project-Documentation.md"
OUTPUT = ROOT / "output" / "pdf" / "Salon-Hup-Project-Documentation.pdf"

PAGE_WIDTH, PAGE_HEIGHT = A4
NAVY = colors.HexColor("#17243B")
TEAL = colors.HexColor("#169C91")
LIGHT_TEAL = colors.HexColor("#E9F7F5")
INK = colors.HexColor("#253247")
MUTED = colors.HexColor("#68758A")
LINE = colors.HexColor("#D8DEE8")
PAPER = colors.HexColor("#F7F9FC")
WHITE = colors.white


def register_fonts():
    candidates = [
        ("DocRegular", Path("C:/Windows/Fonts/aptos.ttf")),
        ("DocBold", Path("C:/Windows/Fonts/aptos-bold.ttf")),
        ("DocMono", Path("C:/Windows/Fonts/consola.ttf")),
    ]
    fallbacks = {
        "DocRegular": "Helvetica",
        "DocBold": "Helvetica-Bold",
        "DocMono": "Courier",
    }
    resolved = {}
    for name, path in candidates:
        if path.exists():
            pdfmetrics.registerFont(TTFont(name, str(path)))
            resolved[name] = name
        else:
            resolved[name] = fallbacks[name]
    return resolved


FONTS = register_fonts()


def inline(text):
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = re.sub(r"`([^`]+)`", r'<font name="%s" color="#0F766E">\1</font>' % FONTS["DocMono"], text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    return text


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="DocBody", parent=styles["BodyText"], fontName=FONTS["DocRegular"],
    fontSize=9.4, leading=14, textColor=INK, spaceAfter=7,
))
styles.add(ParagraphStyle(
    name="DocH1", parent=styles["Heading1"], fontName=FONTS["DocBold"],
    fontSize=19, leading=23, textColor=NAVY, spaceBefore=12, spaceAfter=9,
    keepWithNext=True,
))
styles.add(ParagraphStyle(
    name="DocH2", parent=styles["Heading2"], fontName=FONTS["DocBold"],
    fontSize=13, leading=17, textColor=TEAL, spaceBefore=10, spaceAfter=6,
    keepWithNext=True,
))
styles.add(ParagraphStyle(
    name="DocH3", parent=styles["Heading3"], fontName=FONTS["DocBold"],
    fontSize=10.5, leading=14, textColor=NAVY, spaceBefore=8, spaceAfter=4,
    keepWithNext=True,
))
styles.add(ParagraphStyle(
    name="DocBullet", parent=styles["DocBody"], leftIndent=14, firstLineIndent=-8,
    bulletIndent=2, spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="DocCode", parent=styles["Code"], fontName=FONTS["DocMono"],
    fontSize=8.2, leading=11, textColor=colors.HexColor("#E6EDF7"),
    backColor=NAVY, borderPadding=9, spaceBefore=4, spaceAfter=10,
))
styles.add(ParagraphStyle(
    name="TableHeader", fontName=FONTS["DocBold"], fontSize=7.8,
    leading=10, textColor=WHITE, alignment=TA_LEFT,
))
styles.add(ParagraphStyle(
    name="TableCell", fontName=FONTS["DocRegular"], fontSize=7.6,
    leading=10, textColor=INK,
))


class ProjectDocTemplate(BaseDocTemplate):
    def __init__(self, filename):
        super().__init__(
            filename,
            pagesize=A4,
            leftMargin=19 * mm,
            rightMargin=19 * mm,
            topMargin=20 * mm,
            bottomMargin=18 * mm,
            title="Salon Hup Project Documentation",
            author="Salon Hup Development Team",
            subject="Product and technical specification",
        )
        frame = Frame(
            self.leftMargin,
            self.bottomMargin,
            self.width,
            self.height,
            id="content",
            leftPadding=0,
            rightPadding=0,
            topPadding=0,
            bottomPadding=0,
        )
        self.addPageTemplates([
            PageTemplate(id="cover", frames=frame, onPage=draw_cover),
            PageTemplate(id="body", frames=frame, onPage=draw_body),
        ])

    def afterPage(self):
        if self.page == 1:
            self.handle_nextPageTemplate("body")


def draw_cover(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(NAVY)
    canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, stroke=0, fill=1)
    canvas.setFillColor(TEAL)
    canvas.circle(PAGE_WIDTH - 25 * mm, PAGE_HEIGHT - 30 * mm, 34 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.HexColor("#1DAD9F"))
    canvas.circle(PAGE_WIDTH - 3 * mm, PAGE_HEIGHT - 60 * mm, 23 * mm, stroke=0, fill=1)
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 34)
    canvas.drawString(24 * mm, PAGE_HEIGHT - 68 * mm, "Salon Hup")
    canvas.setFont(FONTS["DocRegular"], 17)
    canvas.setFillColor(colors.HexColor("#C8D4E7"))
    canvas.drawString(24 * mm, PAGE_HEIGHT - 82 * mm, "Project Documentation")
    canvas.setFillColor(TEAL)
    canvas.roundRect(24 * mm, PAGE_HEIGHT - 106 * mm, 49 * mm, 9 * mm, 4 * mm, stroke=0, fill=1)
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 8)
    canvas.drawCentredString(48.5 * mm, PAGE_HEIGHT - 103.2 * mm, "ACTIVE DEVELOPMENT")
    canvas.setFillColor(colors.HexColor("#AEBBD0"))
    canvas.setFont(FONTS["DocRegular"], 10)
    canvas.drawString(24 * mm, 45 * mm, "Product and technical specification")
    canvas.drawString(24 * mm, 37 * mm, "Version 1.1  |  26 September 2026")
    canvas.setFillColor(WHITE)
    canvas.setFont(FONTS["DocBold"], 9)
    canvas.drawRightString(PAGE_WIDTH - 20 * mm, 20 * mm, "SALON HUP")
    canvas.restoreState()


def draw_body(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.5)
    canvas.line(doc.leftMargin, PAGE_HEIGHT - 13 * mm, PAGE_WIDTH - doc.rightMargin, PAGE_HEIGHT - 13 * mm)
    canvas.setFont(FONTS["DocBold"], 7.5)
    canvas.setFillColor(NAVY)
    canvas.drawString(doc.leftMargin, PAGE_HEIGHT - 10 * mm, "SALON HUP")
    canvas.setFont(FONTS["DocRegular"], 7.5)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(PAGE_WIDTH - doc.rightMargin, PAGE_HEIGHT - 10 * mm, "PROJECT DOCUMENTATION")
    canvas.setStrokeColor(LINE)
    canvas.line(doc.leftMargin, 12 * mm, PAGE_WIDTH - doc.rightMargin, 12 * mm)
    canvas.setFont(FONTS["DocRegular"], 7.5)
    canvas.drawString(doc.leftMargin, 7.5 * mm, "Version 1.1")
    canvas.drawRightString(PAGE_WIDTH - doc.rightMargin, 7.5 * mm, f"Page {doc.page}")
    canvas.restoreState()


def make_table(rows, available_width):
    converted = []
    for row_index, row in enumerate(rows):
        style = styles["TableHeader"] if row_index == 0 else styles["TableCell"]
        converted.append([Paragraph(inline(cell), style) for cell in row])
    columns = len(rows[0])
    if columns == 2:
        widths = [available_width * 0.29, available_width * 0.71]
    elif columns == 3:
        widths = [available_width * 0.18, available_width * 0.32, available_width * 0.50]
    elif columns == 4:
        widths = [available_width * 0.12, available_width * 0.29, available_width * 0.42, available_width * 0.17]
    else:
        widths = [available_width / columns] * columns
    table = Table(converted, colWidths=widths, repeatRows=1, hAlign="LEFT")
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [WHITE, PAPER]),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


def parse_markdown(text, available_width):
    lines = text.splitlines()
    story = [Spacer(1, 205 * mm), PageBreak()]
    index = 0
    in_code = False
    code_lines = []
    metadata_done = False

    while index < len(lines):
        raw = lines[index].rstrip()
        stripped = raw.strip()

        if stripped.startswith("# "):
            index += 1
            continue

        if stripped.startswith("**Document type:") or (
            not metadata_done and stripped.startswith("**") and ":**" in stripped
        ):
            metadata_done = True
            index += 1
            continue

        if stripped == "```text":
            in_code = True
            code_lines = []
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
                story.append(Spacer(1, 3))
                table_block = [
                    make_table(rows, available_width),
                    Spacer(1, 8),
                ]
                if len(rows) <= 6:
                    story.append(KeepTogether(table_block))
                else:
                    story.extend(table_block)
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
    source_text = SOURCE.read_text(encoding="utf-8")
    doc = ProjectDocTemplate(str(OUTPUT))
    story = parse_markdown(source_text, doc.width)
    doc.build(story)
    print(OUTPUT)


if __name__ == "__main__":
    build()
