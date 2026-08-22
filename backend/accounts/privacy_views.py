"""Compatibility alias for the Phase 6 Privacy view implementation owner."""

import sys

from .privacy import views as _implementation

sys.modules[__name__] = _implementation
