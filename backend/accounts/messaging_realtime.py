"""Compatibility alias for the Phase 6 Messaging realtime-event owner."""

import sys

from .messaging import messaging_realtime as _implementation

sys.modules[__name__] = _implementation
