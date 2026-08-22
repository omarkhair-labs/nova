"""Compatibility alias for the Phase 6 Messaging paging owner."""

import sys

from .messaging import paging as _implementation

sys.modules[__name__] = _implementation
