"""Compatibility alias for the Phase 6 Reels ranking owner."""

import sys

from .reels import ranking as _implementation

sys.modules[__name__] = _implementation
