from __future__ import annotations
import json
from pathlib import Path

MODID = "batcg"

# Ajusta si tu estructura es distinta
ASSETS = Path("src/main/resources/assets") / MODID
MODELS = ASSETS / "models" / "item"
TEXTURES = ASSETS / "textures" / "item"

# Tiers que ya usas
TIERS = ["common", "uncommon", "rare", "epic", "legendary", "shiny"]

# Paths existentes (3D)
FRAME_3D_CHILD_DIR = MODELS / "card" / "frame"
ICON_3D_CHILD_DIR  = MODELS / "card" / "icon"

# Paths GUI nuevos
GUI_DIR = MODELS / "card" / "gui"
GUI_FRAME_PARENT = GUI_DIR / "frame.json"
GUI_ICON_PARENT  = GUI_DIR / "icon.json"
GUI_FRAME_CHILD_DIR = GUI_DIR / "frame"
GUI_ICON_CHILD_DIR  = GUI_DIR / "icon"

def write_json(path: Path, obj: dict):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")

def parent_gui_frame() -> dict:
    return {
        "texture_size": [32, 32],
        "gui_light": "front",
        "ambientocclusion": False,
        "render_type": "cutout",
        "elements": [
            {
                "name": "frame_gui",
                "shade": False,
                "from": [0, 0, 1.001],
                "to":   [10, 13.5, 1.001],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#0"}
                }
            }
        ]
    }

def parent_gui_icon() -> dict:
    return {
        "texture_size": [32, 32],
        "gui_light": "front",
        "ambientocclusion": False,
        "render_type": "cutout",
        "elements": [
            {
                "name": "icon_gui",
                "shade": False,
                "from": [1, 5, 1.002],
                "to":   [9, 13, 1.002],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#0"}
                }
            }
        ]
    }

def make_frame_child(tier: str, use_gui_textures: bool) -> dict:
    # Si quieres usar texturas gui, apuntamos a common_gui, etc.
    tex = f"{MODID}:item/card/frame/{tier}" + ("_gui" if use_gui_textures else "")
    return {
        "parent": f"{MODID}:item/card/gui/frame",
        "textures": {
            "0": tex,
            "particle": tex
        }
    }

def make_icon_child(icon_id: str, use_gui_textures: bool) -> dict:
    tex = f"{MODID}:item/card/icon/{icon_id}" + ("_gui" if use_gui_textures else "")
    return {
        "parent": f"{MODID}:item/card/gui/icon",
        "textures": {
            "0": tex,
            "particle": tex
        }
    }

def discover_icon_ids_from_models() -> list[str]:
    """
    Descubre IDs leyendo tus modelos 3D existentes:
    assets/batcg/models/item/card/icon/<id>.json
    """
    if not ICON_3D_CHILD_DIR.exists():
        return []
    ids = []
    for p in ICON_3D_CHILD_DIR.glob("*.json"):
        ids.append(p.stem.lower())
    ids.sort()
    return ids

def copy_gui_textures_if_missing(icon_ids: list[str]):
    """
    Opcional: copiar texturas normales -> _gui para que existan.
    NO hace hard-alpha; solo copia.
    """
    # Frames
    for tier in TIERS:
        src = TEXTURES / "card" / "frame" / f"{tier}.png"
        dst = TEXTURES / "card" / "frame" / f"{tier}_gui.png"
        if src.exists() and not dst.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(src.read_bytes())

    # Icons
    for icon_id in icon_ids:
        src = TEXTURES / "card" / "icon" / f"{icon_id}.png"
        dst = TEXTURES / "card" / "icon" / f"{icon_id}_gui.png"
        if src.exists() and not dst.exists():
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(src.read_bytes())

def main():
    # Configura esto:
    USE_GUI_TEXTURES = False   # pon True si vas a usar *_gui.png
    COPY_GUI_TEXTURES = False  # pon True si quieres que copie png -> _gui.png

    # 1) Parents GUI
    write_json(GUI_FRAME_PARENT, parent_gui_frame())
    write_json(GUI_ICON_PARENT, parent_gui_icon())

    # 2) Children frames GUI por tier
    GUI_FRAME_CHILD_DIR.mkdir(parents=True, exist_ok=True)
    for tier in TIERS:
        write_json(GUI_FRAME_CHILD_DIR / f"{tier}.json", make_frame_child(tier, USE_GUI_TEXTURES))

    # 3) Children icons GUI por ID
    icon_ids = discover_icon_ids_from_models()
    if not icon_ids:
        print("No encontré modelos de icon 3D en:", ICON_3D_CHILD_DIR)
        print("Crea los models 3D o cambia el método de discovery.")
    else:
        GUI_ICON_CHILD_DIR.mkdir(parents=True, exist_ok=True)
        for icon_id in icon_ids:
            write_json(GUI_ICON_CHILD_DIR / f"{icon_id}.json", make_icon_child(icon_id, USE_GUI_TEXTURES))

    # 4) (Opcional) copiar texturas -> *_gui.png
    if COPY_GUI_TEXTURES and icon_ids:
        copy_gui_textures_if_missing(icon_ids)

    print("✅ GUI models generados en:", GUI_DIR)
    print("Frames GUI:", GUI_FRAME_CHILD_DIR)
    print("Icons GUI :", GUI_ICON_CHILD_DIR)
    if USE_GUI_TEXTURES:
        print("ℹ️ Apuntando a texturas *_gui.png (asegúrate de que existan).")
    else:
        print("ℹ️ Reutilizando texturas normales (sin *_gui).")

if __name__ == "__main__":
    main()
