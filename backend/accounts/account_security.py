"""Compatibility alias for the Phase 6 Auth implementation owner."""

import sys

from .auth import security as _implementation

sys.modules[__name__] = _implementation
