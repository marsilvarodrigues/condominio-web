#!/usr/bin/env python3
"""
Generate PlantUML class and activity diagrams for Java files changed since a base ref.

For each changed class:
  - One combined class diagram covering all changed classes (hierarchy + public methods)
  - One activity diagram per public method (shows calls to service/repository/gateway layer)

Renders to SVG via the `plantuml` CLI and writes target/diagrams/summary.md
for use as a GitHub Step Summary.

Usage:
  python3 scripts/gen_diagrams.py --base-ref origin/main
  python3 scripts/gen_diagrams.py --base-ref origin/main --output-dir target/diagrams
"""

import argparse
import base64
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


# ── Data structures ────────────────────────────────────────────────────────────

@dataclass
class MethodInfo:
    name: str
    return_type: str
    params: str
    calls: list = field(default_factory=list)


@dataclass
class ClassInfo:
    name: str
    kind: str          # class | interface | record | enum | abstract class
    extends: Optional[str]
    implements: list
    methods: list = field(default_factory=list)
    package: str = ""


# ── Git helpers ────────────────────────────────────────────────────────────────

def get_changed_java_files(base_ref: str) -> list:
    """Return list of Path objects for Java source files added/changed since base_ref."""
    try:
        result = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACM", base_ref, "HEAD"],
            capture_output=True, text=True, check=True
        )
        return [
            Path(f) for f in result.stdout.strip().splitlines()
            if f.endswith(".java") and "src/main/java" in f and Path(f).exists()
        ]
    except subprocess.CalledProcessError as e:
        print(f"[WARN] git diff failed: {e.stderr.strip()}", file=sys.stderr)
        return []


# ── Java parser ───────────────────────────────────────────────────────────────

_CLASS_PATTERN = re.compile(
    r"(?:^|\n)\s*"
    r"(?:public\s+)?(?:(?:abstract|final|sealed)\s+)*"
    r"(class|interface|record|enum)\s+(\w+)"
    r"(?:\s*<[^>]*>)?"                          # optional generics
    r"(?:\s+extends\s+([\w.<>,\s]+?))?"         # optional extends
    r"(?:\s+implements\s+([\w.<>,\s]+?))?"      # optional implements
    r"\s*[{(]",                                  # opening brace or record paren
    re.MULTILINE,
)

_PKG_PATTERN = re.compile(r"^package\s+([\w.]+)\s*;", re.MULTILINE)

_METHOD_PATTERN = re.compile(
    r"\bpublic\b(?:\s+(?:static|final|synchronized|default))*\s+"
    r"(?!(?:class|interface|record|enum)\b)"
    r"(?:<[^>]*>\s+)?"                          # optional type parameter
    r"([\w<>\[\],\s]+?)\s+"                     # return type (greedy-minimal)
    r"(\w+)\s*"
    r"\(([^)]*)\)"                              # parameter list
    r"(?:\s+throws\s+[\w,\s]+)?"
    r"\s*\{",
)

_CALL_PATTERN = re.compile(
    r"\b(\w+(?:Service|Repository|Gateway|Client|Mapper|Helper))\s*\.\s*(\w+)\s*\("
)


def _strip_generics(s: str) -> str:
    return re.sub(r"<[^>]*>", "", s).strip()


def _last_segment(s: str) -> str:
    return s.split(".")[-1].strip()


def _simplify_type(t: str) -> str:
    t = _strip_generics(t.strip())
    return _last_segment(t)


def _simplify_params(params: str) -> str:
    if not params.strip():
        return ""
    parts = [p.strip() for p in params.split(",") if p.strip()]
    names = []
    for p in parts:
        tokens = p.split()
        names.append(tokens[-1] if tokens else p)
    return ", ".join(names)


def _extract_body(content: str, brace_start: int) -> str:
    depth = 0
    for i in range(brace_start, len(content)):
        if content[i] == "{":
            depth += 1
        elif content[i] == "}":
            depth -= 1
            if depth == 0:
                return content[brace_start:i]
    return content[brace_start:]


def _extract_calls(body: str) -> list:
    seen: set = set()
    calls = []
    for m in _CALL_PATTERN.finditer(body):
        call = f"{m.group(1)}.{m.group(2)}()"
        if call not in seen:
            seen.add(call)
            calls.append(call)
        if len(calls) >= 12:
            break
    return calls


def parse_java_file(path: Path) -> Optional[ClassInfo]:
    try:
        content = path.read_text(encoding="utf-8")
    except Exception as e:
        print(f"[WARN] Cannot read {path}: {e}", file=sys.stderr)
        return None

    pkg_m = _PKG_PATTERN.search(content)
    package = pkg_m.group(1) if pkg_m else ""

    cls_m = _CLASS_PATTERN.search(content)
    if not cls_m:
        return None

    kind = cls_m.group(1)
    name = cls_m.group(2)
    extends_raw = cls_m.group(3)
    implements_raw = cls_m.group(4)

    extends = _last_segment(_strip_generics(extends_raw)) if extends_raw else None
    implements = (
        [_last_segment(_strip_generics(i)) for i in implements_raw.split(",")]
        if implements_raw else []
    )

    cls = ClassInfo(
        name=name, kind=kind,
        extends=extends, implements=implements,
        package=package,
    )

    for m in _METHOD_PATTERN.finditer(content):
        ret = m.group(1).strip()
        mname = m.group(2)
        params = m.group(3).strip()

        # Skip constructors
        if mname == name:
            continue
        # Skip @Override on Object methods that pollute diagrams
        if mname in ("equals", "hashCode", "toString"):
            continue

        body = _extract_body(content, m.end() - 1)
        cls.methods.append(MethodInfo(
            name=mname,
            return_type=ret,
            params=params,
            calls=_extract_calls(body),
        ))

    return cls


# ── PlantUML generators ────────────────────────────────────────────────────────

def generate_class_diagram(classes: list) -> str:
    lines = [
        "@startuml class_diagram",
        "skinparam classAttributeIconSize 0",
        "skinparam classFontSize 12",
        "hide empty members",
        "",
    ]

    for cls in classes:
        keyword = "abstract class" if cls.kind == "abstract" else cls.kind
        lines.append(f"{keyword} {cls.name} {{")
        for m in cls.methods:
            lines.append(
                f"  +{m.name}({_simplify_params(m.params)}): {_simplify_type(m.return_type)}"
            )
        lines.append("}")
        lines.append("")

    for cls in classes:
        if cls.extends:
            lines.append(f"{cls.name} --|> {cls.extends}")
        for iface in cls.implements:
            lines.append(f"{cls.name} ..|> {iface}")

    lines.append("@enduml")
    return "\n".join(lines)


def generate_sequence_diagram(cls: ClassInfo, method: MethodInfo) -> str:
    # Collect unique participants in call order
    seen: set = set()
    participants: list = []
    for call in method.calls:
        target = call.split(".")[0]
        if target not in seen:
            seen.add(target)
            participants.append(target)

    lines = [
        "@startuml",
        f'title {cls.name}.{method.name}()',
        "skinparam sequenceFontSize 11",
        "skinparam sequenceArrowThickness 1.5",
        "skinparam responseMessageBelowArrow true",
        "skinparam maxMessageSize 120",
        "",
        f'participant "{cls.name}" as self',
    ]
    for p in participants:
        lines.append(f'participant "{p}"')

    simplified_params = _simplify_params(method.params)
    lines += [
        "",
        f"[-> self : {method.name}({simplified_params})",
        "activate self",
        "",
    ]

    for call in method.calls:
        target, _, meth_raw = call.partition(".")
        meth = meth_raw.rstrip("()")
        lines += [
            f'self -> "{target}" : {meth}()',
            f'activate "{target}"',
            f'"{target}" --> self',
            f'deactivate "{target}"',
            "",
        ]

    if not method.calls:
        lines.append("note over self : no external layer calls")
        lines.append("")

    lines += [
        "[<-- self : return",
        "deactivate self",
        "@enduml",
    ]
    return "\n".join(lines)


# ── Rendering ─────────────────────────────────────────────────────────────────

def render_plantuml(puml_path: Path, output_dir: Path) -> Optional[Path]:
    try:
        subprocess.run(
            ["plantuml", "-tsvg", "-o", str(output_dir.resolve()), str(puml_path.resolve())],
            capture_output=True, check=True, timeout=30,
        )
        svg = output_dir / (puml_path.stem + ".svg")
        return svg if svg.exists() else None
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired, FileNotFoundError) as e:
        print(f"[WARN] plantuml render failed for {puml_path.name}: {e}", file=sys.stderr)
        return None


# ── Inline image helpers ──────────────────────────────────────────────────────

def svg_to_data_uri(svg_path: Path) -> Optional[str]:
    """Base64-encode an SVG file for embedding as a data URI in GitHub step summary."""
    try:
        b64 = base64.b64encode(svg_path.read_bytes()).decode("ascii")
        return f"data:image/svg+xml;base64,{b64}"
    except Exception as e:
        print(f"[WARN] Could not encode {svg_path}: {e}", file=sys.stderr)
        return None


def inline_img(uri: str, alt: str, width: int = 900) -> str:
    return f'<img src="{uri}" alt="{alt}" width="{width}">\n'


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-ref", default="origin/main",
                        help="git ref to compare HEAD against (default: origin/main)")
    parser.add_argument("--output-dir", default="target/diagrams",
                        help="directory for generated files (default: target/diagrams)")
    args = parser.parse_args()

    out_dir  = Path(args.output_dir)
    puml_dir = out_dir / "puml"
    svg_dir  = out_dir / "svg"
    for d in (out_dir, puml_dir, svg_dir):
        d.mkdir(parents=True, exist_ok=True)

    changed = get_changed_java_files(args.base_ref)
    summary_path = out_dir / "summary.md"

    if not changed:
        summary_path.write_text("## UML Diagrams\n\nNo changed Java source files detected.\n", encoding="utf-8")
        print("No changed Java files — skipping diagram generation.")
        return 0

    print(f"Processing {len(changed)} changed file(s)…")

    classes = []
    for f in changed:
        cls = parse_java_file(f)
        if cls:
            classes.append(cls)
            print(f"  ✓ {cls.name} — {len(cls.methods)} public method(s)")
        else:
            print(f"  ✗ {f.name} — could not parse")

    if not classes:
        summary_path.write_text("## UML Diagrams\n\nNo parseable class changes found.\n", encoding="utf-8")
        return 0

    # ── Class diagram ──────────────────────────────────────────────────────────
    class_puml = puml_dir / "class_diagram.puml"
    class_puml.write_text(generate_class_diagram(classes), encoding="utf-8")
    class_svg = render_plantuml(class_puml, svg_dir)
    if class_svg:
        print(f"  → {class_svg}")

    # ── Sequence diagrams (one per public method) ─────────────────────────────
    activity_results = []
    for cls in classes:
        for method in cls.methods:
            stem = f"sequence_{cls.name}_{method.name}"
            puml_path = puml_dir / f"{stem}.puml"
            puml_path.write_text(generate_sequence_diagram(cls, method), encoding="utf-8")
            svg = render_plantuml(puml_path, svg_dir)
            if svg:
                activity_results.append((cls.name, method.name, svg))
                print(f"  → {svg}")

    # ── Summary markdown ───────────────────────────────────────────────────────
    md: list = [
        "## UML Diagrams — Changed Code\n",
        f"**{len(classes)} class(es) in {len(changed)} file(s) changed vs `{args.base_ref}`**\n",
        "\n### Changed Classes\n",
    ]

    for cls in classes:
        methods_str = ", ".join(f"`{m.name}()`" for m in cls.methods) or "—"
        md.append(f"- **`{cls.package}.{cls.name}`** ({cls.kind}): {methods_str}")

    if class_svg:
        md.append("\n### Class Diagram\n")
        uri = svg_to_data_uri(class_svg)
        if uri:
            md.append(inline_img(uri, "Class Diagram", width=900))
        else:
            md.append("> ⚠️ Could not embed class diagram.\n")

    if activity_results:
        md.append("\n### Sequence Diagrams\n")
        for cls_name, method_name, svg_path in activity_results:
            uri = svg_to_data_uri(svg_path)
            img_html = inline_img(uri, f"{cls_name}.{method_name}", width=600) if uri else "⚠️ render failed\n"
            md += [
                f"\n<details><summary><code>{cls_name}.{method_name}()</code></summary>\n",
                f"\n{img_html}",
                "\n</details>\n",
            ]

    summary_path.write_text("\n".join(md), encoding="utf-8")
    print(f"\nSummary written to {summary_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
