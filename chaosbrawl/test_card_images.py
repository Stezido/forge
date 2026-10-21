import tempfile
import unittest
from pathlib import Path

from card_images import card_image_filenames


class CardImageTests(unittest.TestCase):
    def test_same_name_token_does_not_renumber_parent_image(self):
        with tempfile.TemporaryDirectory() as directory:
            edition = Path(directory)/'edition.txt'
            edition.write_text('[cards]\n016 C Nagging Siren Twin\n')
            parent = {'id': 'cb-016', 'name': 'Nagging Siren Twin', 'token': False}
            token = {'id': 'cb-315', 'name': 'Nagging Siren Twin', 'token': True}
            before = card_image_filenames([parent], edition)
            after = card_image_filenames([parent, token], edition)
            self.assertEqual(after['cb-016'], before['cb-016'])
            self.assertEqual(after['cb-016'], 'Nagging Siren Twin.full.png')
            self.assertEqual(after['cb-315'], 'Nagging Siren Twin Token.full.png')


if __name__ == '__main__':
    unittest.main()
