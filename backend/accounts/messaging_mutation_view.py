"""Compatibility alias for the Phase 6 Messaging mutation owner."""

import sys

from .messaging import mutation as _implementation

sys.modules[__name__] = _implementation
