"""Compatibility alias for the Phase 6 Reels profile owner."""

import sys

from .reels import profile as _implementation

sys.modules[__name__] = _implementation
