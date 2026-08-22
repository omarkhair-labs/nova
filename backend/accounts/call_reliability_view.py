"""Compatibility alias for the Phase 6 Calls reliable REST owner."""

import sys

from .calls import call_reliability_view as _implementation

sys.modules[__name__] = _implementation
