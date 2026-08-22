"""Compatibility alias for the Phase 6 Calls signaling owner."""

import sys

from .calls import call_realtime as _implementation

sys.modules[__name__] = _implementation
