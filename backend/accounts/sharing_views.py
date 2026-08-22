"""Compatibility alias for the Phase 6 Sharing domain owner."""

import sys

from .sharing import views as _implementation

sys.modules[__name__] = _implementation
