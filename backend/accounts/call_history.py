"""Compatibility alias for the Phase 6 Calls history owner."""

import sys

from .calls import call_history as _implementation

sys.modules[__name__] = _implementation
