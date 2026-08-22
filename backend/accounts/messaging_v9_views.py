"""Compatibility alias for the stable Phase 6 Messaging tools owner."""

import sys

from .messaging import tools as _implementation

sys.modules[__name__] = _implementation
