"""Resolve native renders to Forge's edition/functional-variant image names."""
from collections import Counter, defaultdict
import json
import re
import unicodedata


def card_image_filenames(cards, edition):
    variants = {}
    section = None
    for line in edition.read_text().splitlines():
        line = line.strip()
        if line.startswith('['):
            section = line
        if section != '[cards]':
            continue
        match = re.fullmatch(r'(\d+)\s+\S+\s+(.+?)(?:\s+\$(\{.*\}))?', line)
        if not match:
            continue
        collector, name, metadata = match.groups()
        variant = json.loads(metadata).get('variant', '') if metadata else ''
        if not isinstance(variant, str) or (variant and not re.fullmatch(r'[A-Za-z0-9_ -]+', variant)):
            raise ValueError('Unsafe image variant: '+collector)
        variants['cb-'+collector.zfill(3)] = (name, variant)
    groups = {}
    ordinary_names = {card['name'] for card in cards if not card['token']}
    for card in cards:
        name, variant = variants.get(card['id'], (card['name'], ''))
        if name != card['name'] or (not card['token'] and card['id'] not in variants):
            raise ValueError('Image edition/source mismatch: '+card['id'])
        # A named token must not change the parent's normal Forge image key.
        if card['token'] and name in ordinary_names:
            name += ' Token'
        groups[card['id']] = (name, variant)
    counts = Counter(groups.values())
    indices = defaultdict(int)
    filenames, used = {}, set()
    for card in cards:
        group = groups[card['id']]
        name, variant = group
        if variant:
            name += ' '+variant
        name = unicodedata.normalize('NFD', name)
        name = ''.join(c for c in name if not unicodedata.combining(c) and c not in '\"/:?')
        indices[group] += 1
        if counts[group] > 1:
            name += str(indices[group])
        filename = name+'.full.png'
        if filename.casefold() in used:
            raise ValueError('Windows card picture collision: '+filename)
        used.add(filename.casefold())
        filenames[card['id']] = filename
    return filenames
