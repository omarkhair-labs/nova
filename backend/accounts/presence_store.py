"""Compatibility alias for the Phase 6 Messaging presence owner."""

import sys

from .messaging import presence_store as _implementation

sys.modules[__name__] = _implementation
