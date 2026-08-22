"""Compatibility alias for the Phase 6 Calls reliable realtime owner."""

import sys

from .calls import call_reliability_realtime as _implementation

sys.modules[__name__] = _implementation
