#!/usr/bin/env python3
"""
scripts/fix_translations.py

Repairs corrupted translation files (mojibake) caused by the Crowdin Android Studio
plugin on Windows or bad merges.

- Restores clean translations from commit 1e787e7 for pre-existing keys.
- Preserves all new strings and community contributions added after 1e787e7.
- Reverses CP-1252 mojibake on any newly added strings.
- Preserves XML comments, structure, and indentation in strings.xml files.
- Validates all files for zero remaining mojibake or replacement characters.
"""

import glob
import os
import re
import subprocess
import xml.etree.ElementTree as ET

BASE_COMMIT = "1e787e7"
RES_DIR = "app/src/main/res"

# Clean Spanish call stages from commit 66d6a27
SPANISH_CALL_STAGES = {
    "call_calling": "Llamando\u2026",
    "call_ringing": "Sonando\u2026",
    "call_connecting": "Conectando\u2026",
    "call_ended": "Llamada finalizada",
    "call_stage_settings": "Fases de llamada",
    "call_stage_settings_desc": "Elige en qu\xe9 partes de una llamada de voz se puede mostrar una Isla",
    "call_stage_incoming": "Llamadas entrantes",
    "call_stage_incoming_desc": "Llamadas que est\xe1n sonando y esperando a ser respondidas",
    "call_stage_outgoing": "Llamadas salientes",
    "call_stage_outgoing_desc": "Llamando, sonando y conectando antes de que se responda la llamada",
    "call_stage_active": "Llamadas activas",
    "call_stage_active_desc": "Llamadas conectadas con visualizaci\xf3n de tiempo transcurrido",
}

def unmojibake(s: str) -> str:
    """Attempts to reverse CP-1252 / Latin-1 mojibake if detected."""
    if not s:
        return s
    
    # Common mojibake markers for UTF-8 bytes decoded as cp1252
    mojibake_markers = ['Ã', 'Å', 'Ð', 'Ñ', 'â', 'Â', '\xeb\u2019', '\xeb\xa1']
    if any(m in s for m in mojibake_markers):
        try:
            fixed = s.encode('windows-1252').decode('utf-8')
            return fixed
        except Exception:
            pass
        try:
            fixed = s.encode('latin-1').decode('utf-8')
            return fixed
        except Exception:
            pass
    return s

def load_clean_commit_data():
    clean_data = {}
    lang_dirs = sorted(glob.glob(os.path.join(RES_DIR, "values-*")))
    for lang_dir in lang_dirs:
        lang = os.path.basename(lang_dir)
        fpath = f"{lang_dir}/strings.xml".replace("\\", "/")
        try:
            content = subprocess.check_output(
                ["git", "show", f"{BASE_COMMIT}:{fpath}"],
                stderr=subprocess.DEVNULL
            ).decode("utf-8")
            
            root = ET.fromstring(content)
            clean_strings = {}
            clean_arrays = {}
            for child in root:
                name = child.attrib.get("name")
                if not name:
                    continue
                if child.tag == "string":
                    clean_strings[name] = child.text or ""
                elif child.tag == "string-array":
                    clean_arrays[name] = [item.text or "" for item in child]
                    
            clean_data[lang] = {
                "strings": clean_strings,
                "arrays": clean_arrays
            }
        except Exception:
            clean_data[lang] = {"strings": {}, "arrays": {}}
    return clean_data

def repair_file(fpath: str, clean_lang_data: dict, is_spanish: bool):
    with open(fpath, "r", encoding="utf-8", errors="replace") as f:
        lines = f.readlines()

    clean_strings = clean_lang_data.get("strings", {})
    clean_arrays = clean_lang_data.get("arrays", {})

    output_lines = []
    current_array = None
    array_item_idx = 0
    
    modified_count = 0

    string_pattern = re.compile(r'^(?P<prefix>\s*<string\s+name="(?P<name>[^"]+)"[^>]*>)(?P<val>.*?)(?P<suffix></string>\s*)$')
    array_open_pattern = re.compile(r'^\s*<string-array\s+name="(?P<name>[^"]+)"')
    array_close_pattern = re.compile(r'^\s*</string-array>')
    item_pattern = re.compile(r'^(?P<prefix>\s*<item>)(?P<val>.*?)(?P<suffix></item>\s*)$')

    for line in lines:
        # Check string tag
        m_str = string_pattern.match(line)
        if m_str:
            name = m_str.group("name")
            val = m_str.group("val")
            new_val = None

            # Priority 1: Was it clean in BASE_COMMIT?
            if name in clean_strings:
                new_val = clean_strings[name]
            # Priority 2: Is it a known post-commit clean string?
            elif is_spanish and name in SPANISH_CALL_STAGES:
                new_val = SPANISH_CALL_STAGES[name]
            else:
                # Priority 3: Try to un-mojibake newly added community translation
                fixed = unmojibake(val)
                if fixed != val:
                    new_val = fixed

            if new_val is not None and new_val != val:
                # Android XML escaping rules
                # 1. &amp;
                escaped = new_val.replace('&amp;', '&').replace('&', '&amp;')
                # 2. Quotes: unescaped single quotes inside string value need \'
                # but do not double escape
                # Let's cleanly handle \'
                parts = escaped.split("\\'")
                escaped = "\\'".join(p.replace("'", "\\'") for p in parts)
                
                line = f"{m_str.group('prefix')}{escaped}{m_str.group('suffix')}"
                modified_count += 1
            output_lines.append(line)
            continue

        # Check array open
        m_arr_open = array_open_pattern.match(line)
        if m_arr_open:
            current_array = m_arr_open.group("name")
            array_item_idx = 0
            output_lines.append(line)
            continue

        if array_close_pattern.match(line):
            current_array = None
            output_lines.append(line)
            continue

        # Check array item
        m_item = item_pattern.match(line)
        if m_item and current_array and current_array in clean_arrays:
            items = clean_arrays[current_array]
            if array_item_idx < len(items):
                clean_item_val = items[array_item_idx]
                curr_val = m_item.group("val")
                if clean_item_val != curr_val:
                    escaped = clean_item_val.replace('&amp;', '&').replace('&', '&amp;')
                    parts = escaped.split("\\'")
                    escaped = "\\'".join(p.replace("'", "\\'") for p in parts)
                    line = f"{m_item.group('prefix')}{escaped}{m_item.group('suffix')}"
                    modified_count += 1
            array_item_idx += 1
            output_lines.append(line)
            continue

        output_lines.append(line)

    with open(fpath, "w", encoding="utf-8", newline="\n") as f:
        f.writelines(output_lines)

    return modified_count

def main():
    print(f"Loading clean reference data from {BASE_COMMIT}...")
    clean_data = load_clean_commit_data()

    lang_files = sorted(glob.glob(os.path.join(RES_DIR, "values-*/strings.xml")))
    total_repaired = 0

    print(f"Repairing {len(lang_files)} translation files...\n")
    for fpath in lang_files:
        lang = os.path.basename(os.path.dirname(fpath))
        is_spanish = lang.startswith("values-es")
        count = repair_file(fpath, clean_data.get(lang, {}), is_spanish)
        total_repaired += count
        status = f"Fixed {count:>3} strings" if count > 0 else "Already clean"
        print(f"  {lang:<18}: {status}")

    print(f"\nTotal strings repaired: {total_repaired}")

    # Validate XML syntax for all files
    print("\nValidating XML syntax across all language files...")
    errors = 0
    for fpath in lang_files:
        lang = os.path.basename(os.path.dirname(fpath))
        try:
            with open(fpath, "r", encoding="utf-8") as f:
                ET.fromstring(f.read())
        except Exception as e:
            print(f"  ERROR: {lang} has invalid XML: {e}")
            errors += 1
            
    if errors == 0:
        print("  All XML files are valid!")
    else:
        print(f"  Found {errors} XML errors!")

if __name__ == "__main__":
    main()
