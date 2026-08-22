"""Compatibility alias for the Phase 6 Messaging group-management owner."""

import sys

from .messaging import group_management as _implementation

sys.modules[__name__] = _implementation
