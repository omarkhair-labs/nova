"""Compatibility alias for the Phase 6 Messaging REST owner."""

import sys

from .messaging import messaging_views as _implementation

sys.modules[__name__] = _implementation
