"""Compatibility alias for the Phase 6 Messaging WebSocket consumer owner."""

import sys

from .messaging import realtime as _implementation

sys.modules[__name__] = _implementation
